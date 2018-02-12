/*
 * ExportSessionToPacs
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.apache.turbine.modules.actions;

import com.google.common.base.Joiner;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.apache.turbine.util.RunData;
import org.apache.turbine.util.parser.ParameterParser;
import org.apache.velocity.context.Context;
import org.nrg.dqr.dicom.command.cfind.SearchCriteriaTooVagueException;
import org.nrg.dqr.dicom.command.cmove.CMoveFailureException;
import org.nrg.dqr.dicom.command.cmove.CMoveTargetNotFoundException;
import org.nrg.dqr.domain.Series;
import org.nrg.dqr.domain.Study;
import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.dqr.dto.PacsSearchCriteria;
import org.nrg.dqr.dto.PacsSearchResults;
import org.nrg.dqr.restlet.InvalidStudyDateRangeException;
import org.nrg.dqr.restlet.JsonViews;
import org.nrg.dqr.restlet.RequestUtils;
import org.nrg.dqr.services.ExecutedPacsRequestService;
import org.nrg.dqr.services.PacsEntityService;
import org.nrg.dqr.services.PacsService;
import org.nrg.dqr.services.QueuedPacsRequestService;
import org.nrg.xapi.exceptions.NotAuthenticatedException;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.event.EventDetails;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.restlet.extensions.*;
import org.restlet.data.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PermissionDeniedDataAccessException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class ImportFromSpreadsheet extends DqrSecureAction {

    private static final Logger _log = LoggerFactory.getLogger(ImportFromSpreadsheet.class);

      private PacsService _service;
      private Pacs _pacs;
//    private XnatImagesessiondata _session;
//    private String[] _scanIds;
    private UserI _user;

    @Override
    public void doPerform(final RunData data, final Context context) throws Exception {

        _user = TurbineUtils.getUser(data);
        if(_user.isGuest()){
            throw new NotAuthenticatedException("");
        }
        ParameterParser params = data.getParameters();

        //grab the FileItems available in ParameterParser
        FileItem fi = params.getFileItem("csv_to_store");
        File temp = File.createTempFile("xnat", "csv");
        fi.write(temp);

        List<List<String>> rows = FileUtils.CSVFileToArrayList(temp);

        temp.delete();
        fi.delete();

        final String ae = (String) TurbineUtils.GetPassedParameter("ae", data);

        final String project = (String) TurbineUtils.GetPassedParameter("project", data);

        final long pacsId = Long.valueOf((String) TurbineUtils.GetPassedParameter("pacsId", data));
        _pacs = getPacsEntityService().retrieve(pacsId);
        if (_pacs == null) {
            throw new PacsNotFoundException();
        }
        _service = XDAT.getContextService().getBean(PacsService.class);
        ArrayList<Study> studiesList = new ArrayList<>();
        try {
            for(List<String> row : rows){
                final PacsSearchCriteria searchCriteria = new PacsSearchCriteria();
                searchCriteria.setAccessionNumber(row.get(0));
                final PacsSearchResults<String, Study> studies = _service.getStudiesByExample(
                        XDAT.getUserDetails(), _pacs, searchCriteria);
                for(Study currStudy : studies.getResults()){
                    if(currStudy!=null && !studiesList.contains(currStudy)){
                        studiesList.add(currStudy);
                    }
                }
            }
        } catch (final Throwable e) {
            _log.error("Failed to get studies list from spreadsheet.", e);
        }
        for(Study currStudy : studiesList){
            final PacsSearchResults<String, Series> series = _service.getSeriesByStudy(XDAT.getUserDetails(),
                    _pacs, currStudy);
            String _seriesIdsString = "";
            ArrayList<String> seriesIdsList = new ArrayList<>();
            Object[] seriesResults = series.getResults().toArray();
            for(int index = 0; index<seriesResults.length; index++){
                if (index > 0) {
                    _seriesIdsString += ",";
                }
                String result = ((Series)seriesResults[index]).getSeriesInstanceUid();
                _seriesIdsString += result;
                seriesIdsList.add(result);
            }

            try {
                PacsEntityService pacsEntityService = XDAT.getContextService().getBean(PacsEntityService.class);
                boolean pacsIsAvailable = pacsEntityService.isAvailable(_pacs);
                if(pacsIsAvailable) {
                    ExecutedPacsRequest pacsReq = new ExecutedPacsRequest();
                    pacsReq.setPacsId(pacsId);
                    pacsReq.setUsername(_user.getUsername());
                    pacsReq.setXnatProject(project);
                    pacsReq.setStudyInstanceUid(currStudy.getStudyInstanceUid());
                    pacsReq.setSeriesIds(_seriesIdsString);
                    pacsReq.setDestinationAeTitle(ae);
                    pacsReq.setExecutedTime(new Date());

                    XDAT.getContextService().getBean(ExecutedPacsRequestService.class).create(pacsReq);

                    getPacsService().importFromPacsRequest(pacsReq);

                    final String siteUrl = XDAT.getSiteConfigPreferences().getSiteUrl();
                    final StringBuilder prearchive = new StringBuilder(siteUrl);
                    if (!siteUrl.endsWith("/")) {
                        prearchive.append("/");
                    }
                    prearchive.append("app/template/XDATScreen_prearchives.vm");

                    try {
                        if (_log.isDebugEnabled()) {
                            _log.debug("Completed DICOM request for study " + currStudy.getStudyInstanceUid() + (StringUtils.isBlank(project) ? " with no project assignment." : " assigned to project " + project));
                        }
                        //sendNotification(context, "Selected DICOM series requested", "SeriesRequested");
                    } catch (Exception exception) {
                        _log.warn("User " + _user.getLogin() + " successfully requested one or more DICOM series, but an error occurred sending the notification email.", exception);
                    }

                    final EventDetails eventDetails = EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, "IMPORT_FROM_PACS_REQUEST");
                    eventDetails.setComment("Series: " + _seriesIdsString);
                    PersistentWorkflowI wrk = PersistentWorkflowUtils.buildOpenWorkflow(_user, XnatMrsessiondata.SCHEMA_ELEMENT_NAME, currStudy.getStudyId(), project, eventDetails);
                    assert wrk != null;
                    PersistentWorkflowUtils.complete(wrk, wrk.buildEvent());
                }
                else{
                    QueuedPacsRequest pacsReq = new QueuedPacsRequest();
                    pacsReq.setPacsId(pacsId);
                    pacsReq.setUsername(_user.getUsername());
                    pacsReq.setXnatProject(project);
                    pacsReq.setStudyInstanceUid(currStudy.getStudyInstanceUid());
                    pacsReq.setSeriesIds(_seriesIdsString);
                    pacsReq.setDestinationAeTitle(ae);
                    pacsReq.setQueuedTime(new Date());

                    XDAT.getContextService().getBean(QueuedPacsRequestService.class).create(pacsReq);
                }
            } catch (final PacsNotFoundException exception) {
                _log.warn("PACS not found somehow", exception);
            } catch (final PacsNotQueryableException exception) {
                _log.warn("PACS not queryable somehow", exception);
            } catch (final PacsNotStorableException exception) {
                _log.warn("PACS not storable somehow", exception);
            } catch (final PacsNotAvailableException exception) {
                _log.warn("PACS not available at this time", exception);
            } catch (PersistentWorkflowUtils.ActionNameAbsent e) {
                _log.warn("Error creating new workflow event", e);
            } catch (PersistentWorkflowUtils.IDAbsent e) {
                _log.warn("ID absent when creating new workflow event", e);
            } catch (PersistentWorkflowUtils.JustificationAbsent e) {
                _log.warn("Justification absent but required when creating new workflow event", e);
            } catch (Exception e) {
                final Throwable cause = e.getCause();
                if (cause == null || !(cause instanceof Exception)) {
                } else if (cause instanceof CMoveFailureException) {
                    final CMoveFailureException failure = (CMoveFailureException) cause;
                    _log.error("C-MOVE operation failed:\n" + failure.getMessage(), failure);
                }
            }




        }




    }

    private PacsEntityService getPacsEntityService() {
        return XDAT.getContextService().getBean(PacsEntityService.class);
    }


    private PacsService getPacsService() {
        return XDAT.getContextService().getBean(PacsService.class);
    }
}
