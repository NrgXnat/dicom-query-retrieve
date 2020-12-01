/*
 * dicom-query-retrieve: org.nrg.dqr.messaging.PacsRequestListener
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.dqr.messaging;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nrg.dqr.services.PacsService;
import org.nrg.mail.services.MailService;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xft.event.EventDetails;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.security.UserI;

@Getter(AccessLevel.PROTECTED)
@Accessors(prefix = "_")
@Slf4j
public abstract class PacsRequestListener {
    protected PacsRequestListener(final PacsService pacsService, final MailService mailService, final SiteConfigPreferences preferences, final String subject, final String body, final String action) {
        _pacsService = pacsService;
        _mailService = mailService;
        _preferences = preferences;
        _subject = subject;
        _body = StringUtils.appendIfMissing(body, ": ");
        _action = action;
    }

    protected void sendCompleteNotification(final UserI user, final String requestId, final String projectId, final String comment) throws Exception {
        // refresh the user, just in case their email has changed since they made the request
        try {
            _mailService.sendMessage(_preferences.getAdminEmail(), user.getEmail(), _subject, _body + requestId);
            final EventDetails        eventDetails = EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, _action);
            if (StringUtils.isNotBlank(comment)) {
                eventDetails.setComment(comment);
            }
            final PersistentWorkflowI workflow     = PersistentWorkflowUtils.buildOpenWorkflow(user, XnatMrsessiondata.SCHEMA_ELEMENT_NAME, requestId, projectId, eventDetails);
            assert workflow != null;
            PersistentWorkflowUtils.complete(workflow, workflow.buildEvent());
        } catch (final UserNotFoundException e) {
            // not much to do here - was their account deleted since they made the request?
            log.error("User {} queued a PACS request, but their user account cannot be found to send them a confirmation email.", user.getUsername());
        }
    }

    private final PacsService           _pacsService;
    private final MailService           _mailService;
    private final SiteConfigPreferences _preferences;
    private final String                _subject;
    private final String                _body;
    private final String                _action;
}
