/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.messaging.PacsSessionExportRequest
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.messaging;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.nrg.xapi.exceptions.InitializationException;
import org.nrg.xapi.exceptions.InsufficientPrivilegesException;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.security.helpers.Permissions;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.entities.Pacs;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Value
@Builder
public class PacsSessionExportRequest implements Serializable {
    private static final long serialVersionUID = 6524490305495256495L;

    public static XnatImagesessiondata getSession(final UserI user, final String sessionId) throws NotFoundException, InitializationException, InsufficientPrivilegesException {
        final XnatImagesessiondata session = XnatImagesessiondata.getXnatImagesessiondatasById(sessionId, user, false);
        if (session == null) {
            throw new NotFoundException(XnatImagesessiondata.SCHEMA_ELEMENT_NAME, sessionId);
        }

        final boolean canRead;
        try {
            canRead = Permissions.canRead(user, session);
        } catch (Exception e) {
            throw new InitializationException("An error occurred trying to check whether user " + user.getUsername() + " has sufficient privileges to access the session " + sessionId, e);
        }
        if (!canRead) {
            throw new InsufficientPrivilegesException(user.getUsername(), session.getId());
        }

        return session;
    }

    Pacs    pacs;
    Date    dateRequested;
    String  requestingUser;
    Integer workflowId;
    String  sessionId;
    @Singular
    List<String> scanIds;
}
