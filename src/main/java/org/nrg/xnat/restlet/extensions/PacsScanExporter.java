/*
 * org.nrg.xnat.restlet.extensions.PacsScanExporter
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnat.restlet.extensions;

import lombok.extern.slf4j.Slf4j;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.preferences.DqrPreferences;
import org.nrg.dqr.services.DqrAdminSettingsForProjectService;
import org.nrg.dqr.services.PacsEntityService;
import org.nrg.dqr.services.PacsService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.security.helpers.Permissions;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.restlet.XnatRestlet;
import org.nrg.xnat.restlet.resources.ScanResource;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

@XnatRestlet("/services/pacs/{PACS_ID}/export/experiments/{ASSESSED_ID}/scans/{SCAN_ID}")
@Slf4j
public class PacsScanExporter extends ScanResource {
    public PacsScanExporter(final Context context, final Request request, final Response response) {
        super(context, request, response);
        _pacsEntityService = XDAT.getContextService().getBean(PacsEntityService.class);
        _pacsService = XDAT.getContextService().getBean(PacsService.class);
        _preferences = XDAT.getContextService().getBean(DqrPreferences.class);
        _adminSettings = XDAT.getContextService().getBean(DqrAdminSettingsForProjectService.class);
    }

    @Override
    public void handlePut() {
        final UserI user = getUser();
        if (user.isGuest()) {
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "You must be logged in to query a PACS.");
            return;
        }

        searchForScan();
        final XnatImagescandata scan = getScan();
        if (scan == null) {
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Unable to find the specified scan.");
            return;
        }
        try {
            if (!Permissions.canRead(user, scan)) {
                throw new RuntimeException("You do not have access to this session.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error checking permissions for session.");
        }
        if (!Roles.checkRole(user, "Dqr") && !Roles.checkRole(user, "Administrator") && !_preferences.getAllowAllUsersToUseDqr()) {
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "You do not have access to DQR functionality.");
            return;
        }
        if (!_preferences.getAllowAllProjectsToUseDqr() && !_adminSettings.isDqrEnabledForProject(scan.getProject())) {
            //You cannot import into a project that does not have DQR enabled.
            throw new RuntimeException("You cannot import into a project that does not have DQR enabled.");
        }

        try {
            final Pacs pacsToExportTo = _pacsEntityService.retrieve(PacsServiceResource.getPacsId(getRequest()));
            if (pacsToExportTo == null) {
                throw new PacsNotFoundException();
            }
            if (!pacsToExportTo.isStorable()) {
                throw new PacsNotStorableException();
            }

            _pacsService.exportSeries(getUser(), pacsToExportTo, getScan());

            final String projectId = proj != null ? proj.getId() : "Unknown";
            final String studyId   = getScan().getImageSessionId();

            final PersistentWorkflowI workflow = PersistentWorkflowUtils.buildOpenWorkflow(getUser(), XnatMrsessiondata.SCHEMA_ELEMENT_NAME, studyId, projectId, EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, "EXPORT_TO_PACS_REQUEST", null, "Series: " + getScan().getId()));
            assert workflow != null;
            PersistentWorkflowUtils.complete(workflow, workflow.buildEvent());
        } catch (final PacsNotFoundException e) {
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Unable to find the specified PACS.");
        } catch (final PacsNotStorableException e) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, "Requested PACS is not a PACS that can have data sent to it.");
        } catch (PersistentWorkflowUtils.ActionNameAbsent e) {
            log.warn("Error creating new workflow event", e);
            respondToException(e, Status.SERVER_ERROR_INTERNAL);
        } catch (PersistentWorkflowUtils.IDAbsent e) {
            log.warn("ID absent when creating new workflow event", e);
            respondToException(e, Status.SERVER_ERROR_INTERNAL);
        } catch (PersistentWorkflowUtils.JustificationAbsent e) {
            log.warn("Justification absent but required when creating new workflow event", e);
            respondToException(e, Status.SERVER_ERROR_INTERNAL);
        } catch (Exception e) {
            respondToException(e, Status.SERVER_ERROR_INTERNAL);
        }
    }

    @Override
    public boolean allowGet() {
        return false;
    }

    @Override
    public boolean allowPut() {
        return true;
    }

    private final PacsEntityService                 _pacsEntityService;
    private final PacsService                       _pacsService;
    private final DqrPreferences                    _preferences;
    private final DqrAdminSettingsForProjectService _adminSettings;
}
