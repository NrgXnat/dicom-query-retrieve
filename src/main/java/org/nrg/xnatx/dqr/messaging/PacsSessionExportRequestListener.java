/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.messaging.PacsSessionExportRequestListener
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.messaging;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.nrg.mail.services.MailService;
import org.nrg.xapi.exceptions.InitializationException;
import org.nrg.xapi.exceptions.InsufficientPrivilegesException;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.security.user.XnatUserProvider;
import org.nrg.xdat.security.user.exceptions.UserInitException;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.exceptions.PacsNotStorableException;
import org.nrg.xnatx.dqr.services.DicomQueryRetrieveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PacsSessionExportRequestListener extends AbstractPacsRequestListener<PacsSessionExportRequest> {
    private final XnatUserProvider _primaryAdminUserProvider;

    @Autowired
    public PacsSessionExportRequestListener(final DicomQueryRetrieveService dqrService, final SiteConfigPreferences siteConfigPreferences, final MailService mailService, final XnatUserProvider primaryAdminUserProvider) {
        super(dqrService, siteConfigPreferences, mailService);
        _primaryAdminUserProvider = primaryAdminUserProvider;
    }

    @JmsListener(id = "pacsSessionExportRequest", destination = "pacsSessionExportRequest")
    public void onRequest(final PacsSessionExportRequest request) throws PacsNotStorableException, InsufficientPrivilegesException, NotFoundException, InitializationException {
        final String username = request.getRequestingUser();

        final XDATUser user;
        try {
            user = new XDATUser(username);
        } catch (UserNotFoundException e) {
            // not much to do here - was their account deleted since they made the request?
            log.error("User {} queued up a PACS export request, but their user account cannot be found to send them a confirmation email.", username);
            final WrkWorkflowdata workflow = WrkWorkflowdata.getWrkWorkflowdatasByWrkWorkflowdataId(request.getWorkflowId(), _primaryAdminUserProvider.get(), false);
            try {
                workflow.setComments("User account " + username + " does not exist");
                PersistentWorkflowUtils.fail(workflow, workflow.buildEvent());
            } catch (Exception exception) {
                log.error("An error occurred trying to update the workflow {}", workflow.getId(), exception);
            }
            return;
        } catch (UserInitException e) {
            log.error("User {} queued up a PACS export request, but their user account cannot be found to send them a confirmation email.", username);
            final WrkWorkflowdata workflow = WrkWorkflowdata.getWrkWorkflowdatasByWrkWorkflowdataId(request.getWorkflowId(), _primaryAdminUserProvider.get(), false);
            try {
                workflow.setComments("Couldn't retrieve user account " + username + " due to an internal error: " + e.getMessage());
                PersistentWorkflowUtils.fail(workflow, workflow.buildEvent());
            } catch (Exception exception) {
                log.error("An error occurred trying to update the workflow {}", workflow.getId(), exception);
            }
            return;
        }

        log.info("Listener received session export request from user {}", username);
        final Pacs pacs = request.getPacs();
        if (!pacs.isStorable()) {
            throw new PacsNotStorableException(request.getPacs().getId());
        }

        final String               sessionId = request.getSessionId();
        final XnatImagesessiondata session   = PacsSessionExportRequest.getSession(user, sessionId);
        final WrkWorkflowdata      workflow  = getExportWorkflow(request, user);

        final Map<Boolean, List<String>> results      = request.getScanIds().stream().collect(Collectors.groupingBy(scanId -> export(user, pacs, session, scanId, workflow)));
        final boolean                    hasSuccesses = results.containsKey(true);
        final boolean                    hasFailures  = results.containsKey(false);

        // Send complete notification
        try {
            final String subject;
            final String message;
            if (hasSuccesses && !hasFailures) {
                subject = "[" + TurbineUtils.GetSystemName() + "] Export Session " + sessionId + " to PACS " + pacs.getId() + " Request Complete";
                message = "The session you requested " + sessionId + " has been successfully exported to PACS " + pacs.getId() + ", including scans:\n\n * " + String.join("\n * ", request.getScanIds()) + "\n";
                completeExportWorkflow(workflow, sessionId);
            } else if (hasSuccesses) {
                subject = "[" + TurbineUtils.GetSystemName() + "] Export Session " + sessionId + " to PACS " + pacs.getId() + " Request Partial Completion";
                message = "Some of the series you requested to export from " + sessionId + " have been successfully exported to PACS " + pacs.getId() + ", including scans:\n\n * " + String.join("\n * ", results.get(true)) +
                          "\n\nThe following scans failed to send for some reason:\n\n * " + String.join("\n * ", results.get(false)) + "\n";
                completeExportWorkflow(workflow, sessionId);
            } else {
                subject = "[" + TurbineUtils.GetSystemName() + "] Export Session " + sessionId + " to PACS " + pacs.getId() + " Request Failed";
                message = "The session you requested " + sessionId + " failed when being  exported to PACS " + pacs.getId() + ", including scans:\n\n * " + String.join("\n * ", request.getScanIds()) + "\n";
                failExportWorkflow(workflow, sessionId);
            }
            getMailService().sendMessage(getAdminEmail(), user.getEmail(), subject, message);
        } catch (MessagingException e) {
            log.error("Exported ");
        }

        log.info("Listener completed session export request from user {}", username);
    }

    private boolean export(final XDATUser user, final Pacs pacs, final XnatImagesessiondata session, final String scanId, final WrkWorkflowdata workflow) {
        final boolean result = getDqrService().exportSeries(user, pacs, session.getScanById(scanId));
        updateExportWorkflow(workflow, session.getId(), scanId, result);
        return result;
    }

    @Nullable
    private WrkWorkflowdata getExportWorkflow(final PacsSessionExportRequest request, final XDATUser user) {
        final WrkWorkflowdata workflow = WrkWorkflowdata.getWrkWorkflowdatasByWrkWorkflowdataId(request.getWorkflowId(), user, false);
        if (workflow == null) {
            log.warn("Couldn't find workflow ID {} for export operation on session {} and scans {}", request.getWorkflowId(), request.getSessionId(), String.join(", ", request.getScanIds()));
            return null;
        }
        workflow.setStatus("Exporting");
        return workflow;
    }

    private void updateExportWorkflow(final @Nullable WrkWorkflowdata workflow, final String sessionId, final String scanId, final boolean result) {
        updateExportWorkflow(workflow, sessionId, scanId, result, null);
    }

    private void completeExportWorkflow(final @Nullable WrkWorkflowdata workflow, final String sessionId) {
        updateExportWorkflow(workflow, sessionId, null, null, true);
    }

    private void failExportWorkflow(final @Nullable WrkWorkflowdata workflow, final String sessionId) {
        updateExportWorkflow(workflow, sessionId, null, null, false);
    }

    private void updateExportWorkflow(final @Nullable WrkWorkflowdata workflow, final String sessionId, final @Nullable String scanId, final @Nullable Boolean result, final @Nullable Boolean status) {
        if (workflow == null) {
            log.warn("Trying to update workflow for export operation on session {} and scan {}, but the workflow is null: {}", sessionId, scanId, result == null ? "no result status to report" : (result ? "successful export" : "failed export"));
            return;
        }
        final EventMetaI event = workflow.buildEvent();
        try {
            if (status == null) {
                if (StringUtils.isNotBlank(scanId)) {
                    workflow.setScanId(scanId);
                }
                if (result != null) {
                    workflow.setStepDescription(result ? "Exported scan " + scanId + " successfully" : "Scan " + scanId + " failed to export");
                }
                PersistentWorkflowUtils.save(workflow, event);
            } else if (status) {
                workflow.setStepDescription("");
                PersistentWorkflowUtils.complete(workflow, event);
            } else {
                workflow.setStepDescription("");
                PersistentWorkflowUtils.fail(workflow, event);
            }
        } catch (Exception e) {
            log.warn("Failed saving workflow for export operation on session {} and scan {}: {}", sessionId, scanId, result == null ? "no result status to report" : (result ? "successful export" : "failed export"), e);
        }
    }
}
