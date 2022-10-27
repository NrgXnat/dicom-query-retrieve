package org.nrg.xnatx.dqr.services;

import org.nrg.xnat.eventservice.exceptions.UnauthorizedException;
import org.nrg.xnatx.dqr.domain.entities.Pacs;

import java.util.Date;

public interface PacsDicomWebService {
    String dicomWebPing(final String aeTitle, final String rootUrl) throws UnauthorizedException;

    void addCredentialForPacs(final Pacs pacs);

    void addCredentialForPacs(final String aeTitle, final String rootUrl);

    String getContent(final String url) throws UnauthorizedException;

    String buildStudyUrlByDay(final Date day, final String rootUrl);
}
