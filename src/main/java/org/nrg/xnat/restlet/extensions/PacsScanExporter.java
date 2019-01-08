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

import org.nrg.dqr.domain.entities.DqrAdminSettingsForProject;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.services.DqrAdminSettingsForProjectService;
import org.nrg.dqr.services.PacsService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.security.helpers.Permissions;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xft.event.EventDetails;
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
        UserI user = getUser();
        if (user.isGuest()) {
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "You must be logged in to query a PACS.");
        }
        else {
            searchForScan();
            XnatImagescandata scan = getScan();
            if (scan != null) {
                try {
                    if (!Permissions.canRead(user, scan)) {
                        throw new RuntimeException("You do not have access to this session.");
                    }
                }
                catch(Exception e){
                    throw new RuntimeException("Error checking permissions for session.");
                }
                if(!Roles.checkRole(user,"Dqr") && !Roles.checkRole(user,"Administrator")){
                    throw new RuntimeException("You do not have access to DQR functionality.");
                }
                else {
                    DqrAdminSettingsForProject existingSettings = XDAT.getContextService().getBean(DqrAdminSettingsForProjectService.class).findSettingsByProject(scan.getProject());
                    if (existingSettings == null) {
                        //You cannot import into a project that does not have DQR enabled.
                        throw new RuntimeException("You cannot import into a project that does not have DQR enabled.");
                    }
                }

                try {
                    Pacs pacsToExportTo = PacsServiceResource.getPacs(getRequest());
                    if (pacsToExportTo.isStorable()) {
                        pacsService.exportSeries(XDAT.getUserDetails(), pacsToExportTo, getScan());
                    } else {
                        throw new PacsNotStorableException();
                    }
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
                } catch (final PacsNotStorableException e) {
                    getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, "Requested PACS is not a PACS that can have data sent to it.");
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
