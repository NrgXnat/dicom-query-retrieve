/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.messaging.PacsStudyImportRequestListener
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.messaging;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.nrg.mail.services.MailService;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.services.DicomQueryRetrieveService;
import org.nrg.xnatx.dqr.services.PacsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PacsSearchRequestListener extends AbstractPacsRequestListener<PacsSearchRequest> {
    @Autowired
    public PacsSearchRequestListener(final ExecutorService executorService, final PacsService pacsService, final DicomQueryRetrieveService dqrService, final SiteConfigPreferences siteConfigPreferences, final MailService mailService) {
        super(dqrService, siteConfigPreferences, mailService);
        _executorService = executorService;
        _pacsService = pacsService;
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    @JmsListener(id = "pacsSearchRequest", destination = "pacsSearchRequest")
    public void onRequest(final PacsSearchRequest request) throws Exception {
        try {
            final UserI user     = Users.getUser(request.getUsername());
            final Pacs  pacs     = _pacsService.get(request.getPacsId());
            final UUID  searchId = request.getSearchId();

            //Study import requests are not currently set up to allow users to specify which AE to send the data to
            log.info("Listener received search request {} for PACS {} from user {}", searchId, pacs, user.getUsername());

            // public PacsSearchResults<Patient> getPatientsByExample(final UserI user, final Pacs pacs, final PacsSearchCriteria searchCriteria)
            // public Patient getPatientById(final UserI user, final Pacs pacs, final String patientId)
            // public PacsSearchResults<Study> getStudiesByExample(final UserI user, final Pacs pacs, final PacsSearchCriteria searchCriteria)
            // public Study getStudyById(final UserI user, final Pacs pacs, final String studyInstanceUid)
            // public PacsSearchResults<Series> getSeriesByStudy(final UserI user, final Pacs pacs, final Study study)
            // public PacsSearchResults<Series> getSeriesByStudyUid(final UserI user, final Pacs pacs, final String studyUid)
            // public PacsSearchResults<Pair<Study, Series>> getSeriesByStudyUids(final UserI user, final Pacs pacs, final List<String> studyUids)
            // public Series getSeriesById(final UserI user, final Pacs pacs, final String seriesInstanceUid)
            // PatientsByExample: PacsSearchCriteria
            // PatientById: String
            // StudiesByExample: PacsSearchCriteria
            // StudyById: String
            // SeriesByStudy: Study
            // SeriesByStudyUid: String
            // SeriesByStudyUids: List<String>
            // SeriesById: String

            final List<SearchExecutor> executors;
            switch (ObjectUtils.defaultIfNull(request.getSearchType(), PacsSearchRequest.Type.Unknown)) {
                // PatientsByExample: PacsSearchCriteria
                case PatientsByExample:
                    // StudiesByExample: PacsSearchCriteria
                case StudiesByExample:
                    // Not yet supported
                    // request.getPacsSearchCriteria();
                    executors = Collections.emptyList();
                    break;
                // PatientById: String
                case PatientById:
                    // Not yet supported
                    // request.getPatientId();
                    executors = Collections.emptyList();
                    break;
                // StudyById: String
                case StudyById:
                    // Not yet supported
                    // request.getStudyInstanceUid();
                    executors = Collections.emptyList();
                    break;
                // SeriesByStudy: Study
                case SeriesByStudy:
                    // Not yet supported
                    // request.getStudyInstanceUid();
                    executors = Collections.emptyList();
                    break;
                // SeriesByStudyUid: String
                case SeriesByStudyUid:
                    executors = request.getStudyInstanceUids().parallelStream().map(studyInstanceUid -> new SearchExecutor(getDqrService(), searchId, user, pacs, studyInstanceUid)).collect(Collectors.toList());
                    break;
                // SeriesById: String
                case SeriesById:
                    // Not yet supported
                    // request.getSeriesIds();
                    executors = Collections.emptyList();
                    break;
                default:
                    log.warn("No");
                    executors = Collections.emptyList();
            }

            log.info("Launching {} search executors for search request {}", executors.size(), searchId);
            _executorService.invokeAll(executors);
        } catch (final UserNotFoundException e) {
            // not much to do here - was their account deleted since they made the request?
            log.error("User {} queued up a PACS import request, but their user account cannot be found to send them a confirmation email.", request.getUsername());
        } catch (final Exception e) {
            // If errors are not logged before they're rethrown, they do not show up in any of the files
            log.error("Choked on request {} with the following error", request, e);
            throw e;
        }
    }

    private static class SearchExecutor implements Callable<String> {
        SearchExecutor(final DicomQueryRetrieveService dqrService, final UUID requestId, final UserI user, final Pacs pacs, final String studyInstanceUid) {
            _dqrService = dqrService;
            _user = user;
            _pacs = pacs;
            _requestId = requestId;
            _studyInstanceUid = studyInstanceUid;
        }

        @Override
        public String call() throws Exception {
            final PacsSearchResults<Series> result = _dqrService.getSeriesByStudyUid(_user, _pacs, _studyInstanceUid);
            log.info("Got a result for search request {} study instance UID {}", _requestId, _studyInstanceUid);
            _dqrService.updateSearchResults(_requestId, _studyInstanceUid, result);
            return _requestId + ":" + _studyInstanceUid;
        }

        private final DicomQueryRetrieveService _dqrService;
        private final UserI                     _user;
        private final Pacs                      _pacs;
        private final UUID                      _requestId;
        private final String                    _studyInstanceUid;
    }

    private final ExecutorService _executorService;
    private final PacsService     _pacsService;
}
