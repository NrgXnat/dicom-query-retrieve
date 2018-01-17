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
import org.apache.commons.lang.StringUtils;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.services.PacsEntityService;
import org.nrg.dqr.services.PacsService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.restlet.extensions.PacsNotFoundException;
import org.nrg.xnat.restlet.extensions.PacsNotStorableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportSessionToPacs extends DqrSecureAction {

    private static final Logger _log = LoggerFactory.getLogger(ExportSessionToPacs.class);

    private PacsService _service;
    private Pacs _pacs;
    private XnatMrsessiondata _session;
    private String[] _scanIds;
    private UserI _user;

    @Override
    public void doPerform(final RunData data, final Context context) throws PacsNotFoundException {

        _user = TurbineUtils.getUser(data);

        final long pacsId = Long.valueOf((String) TurbineUtils.GetPassedParameter("pacs", data));
        _pacs = getPacsEntityService().retrieve(pacsId);
        if (_pacs == null) {
            throw new PacsNotFoundException();
        }
        _service = XDAT.getContextService().getBean(PacsService.class);

        final String session = (String) TurbineUtils.GetPassedParameter("session", data);
        if (StringUtils.isBlank(session)) {
            throw new RuntimeException("You must specify a session ID for this operation.");
        }
        _session = XnatMrsessiondata.getXnatMrsessiondatasById(session, _user, false);
        if (_session == null) {
            throw new RuntimeException("Couldn't find a session corresponding to the submitted session ID: " + session);
        }

        try {
            _scanIds = (String[]) TurbineUtils.GetPassedObjects("scansToExport", data);
            if (_scanIds == null) {
                _log.debug("No scan IDs found to export, returning.");
                context.put("numberOfProcessedScans", 0);
            } else {
                exportOnDemand();
                context.put("numberOfProcessedScans", _scanIds.length);
                context.put("_user", _user);
                context.put("StringUtils", new StringUtils());
                if (_log.isDebugEnabled()) {
                    _log.debug("User {} exported {} scans from session {}", _user.getLogin(), _scanIds.length, _session.getId());
                }
            }

            data.setScreenTemplate("ExportSessionToPacsRequested.vm");
        } catch (Exception exception) {
            context.put("sessionId", _session.getId());
            context.put("scanIds", Joiner.on(", ").join(_scanIds));
            context.put("host", _pacs.getHost());
            context.put("aeTitle", _pacs.getAeTitle());
            try {
                context.put("storagePort", _pacs.getStoragePort() == null ? "N/A" : _pacs.getStoragePort().toString());
            } catch (Exception e1) {
                context.put("storagePort", "N/A");
            }
            try {
                context.put("qrPort", _pacs.getQueryRetrievePort() == null ? "N/A" : _pacs.getQueryRetrievePort().toString());
            } catch (Exception e1) {
                context.put("qrPort", "N/A");
            }
            context.put("user", _user);
            context.put("StringUtils", new StringUtils());
            context.put("error", exception);
            data.setScreenTemplate("ExportSessionToPacsError.vm");
        }
    }

    private void exportOnDemand() throws PacsNotStorableException {
        if(_pacs.isStorable()) {
            for (String scanId : _scanIds) {
                XnatImagescandata scan = _session.getScanById(scanId);
                _service.exportSeries(_user, _pacs, scan);
                if (_log.isInfoEnabled()) {
                    _log.info("Exported series " + scanId + " from session " + _session.getId());
                }
            }
        }
        else{
            throw new PacsNotStorableException();
        }
    }

    private PacsEntityService getPacsEntityService() {
        return XDAT.getContextService().getBean(PacsEntityService.class);
    }

}
