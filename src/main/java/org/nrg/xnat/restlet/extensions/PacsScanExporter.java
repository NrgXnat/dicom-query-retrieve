/*
 * org.nrg.xnat.restlet.extensions.PacsScanExporter
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnat.restlet.extensions;

import org.nrg.tip.services.PacsService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xft.event.EventDetails;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xnat.restlet.XnatRestlet;
import org.nrg.xnat.restlet.resources.ScanResource;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XnatRestlet("/services/pacs/{PACS_ID}/export/experiments/{ASSESSED_ID}/scans/{SCAN_ID}")
public class PacsScanExporter extends ScanResource {

    public PacsScanExporter(final Context context, final Request request, final Response response) {
        super(context, request, response);
        pacsService = PacsServiceResource.initPacsService();
    }

    @Override
    public void handlePut() {
        searchForScan();

        if (getScan() != null) {
            try {
                pacsService.exportSeries(XDAT.getUserDetails(), PacsServiceResource.getPacs(getRequest()), getScan());
                
                final String projectId;
                if (proj != null) {
                    projectId = proj.getId();
                } else {
                    projectId = "Unknown";
                }
                final String studyId = getScan().getImageSessionId();

                final EventDetails eventDetails = EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, "EXPORT_TO_PACS_REQUEST");
                eventDetails.setComment("Series: " + getScan().getId());
                PersistentWorkflowI wrk = PersistentWorkflowUtils.buildOpenWorkflow(getUser(), XnatMrsessiondata.SCHEMA_ELEMENT_NAME, studyId, projectId, eventDetails);
                assert wrk != null;
                PersistentWorkflowUtils.complete(wrk, wrk.buildEvent());
            } catch (final PacsNotFoundException e) {
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Unable to find the specified PACS.");
            } catch (PersistentWorkflowUtils.ActionNameAbsent e) {
                _log.warn("Error creating new workflow event", e);
                respondToException(e, Status.SERVER_ERROR_INTERNAL);
            } catch (PersistentWorkflowUtils.IDAbsent e) {
                _log.warn("ID absent when creating new workflow event", e);
                respondToException(e, Status.SERVER_ERROR_INTERNAL);
            } catch (PersistentWorkflowUtils.JustificationAbsent e) {
                _log.warn("Justification absent but required when creating new workflow event", e);
                respondToException(e, Status.SERVER_ERROR_INTERNAL);
            } catch (Exception e) {
                respondToException(e, Status.SERVER_ERROR_INTERNAL);
            }
        } else {
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Unable to find the specified scan.");
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

    private static final Logger _log = LoggerFactory.getLogger(PacsSeriesImporter.class);

    private final PacsService pacsService;
}
