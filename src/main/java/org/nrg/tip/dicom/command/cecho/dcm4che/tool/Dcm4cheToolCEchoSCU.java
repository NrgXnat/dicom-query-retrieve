/*
 * org.nrg.tip.dicom.command.cecho.dcm4che.tool.Dcm4cheToolCEchoSCU
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.tip.dicom.command.cecho.dcm4che.tool;

import org.dcm4che2.tool.dcmecho.DcmEcho;
import org.nrg.tip.dicom.command.cecho.CEchoSCU;
import org.nrg.tip.dicom.net.DicomConnectionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dcm4cheToolCEchoSCU implements CEchoSCU {

    private static final Logger log = LoggerFactory.getLogger(Dcm4cheToolCEchoSCU.class);

    private final DcmEcho _dcmEcho;
    private final String _aeTitle;
    private final String _remoteHost;
    private final String _remoteAeTitle;
    private final int _remoteQrPort;

    public Dcm4cheToolCEchoSCU(final DicomConnectionProperties dicomConnectionProperties) {
        _dcmEcho = new DcmEcho(_aeTitle = dicomConnectionProperties.getLocalAeTitle());
        _dcmEcho.setRemoteHost(_remoteHost = dicomConnectionProperties.getRemoteHost());
        _dcmEcho.setCalledAET(_remoteAeTitle = dicomConnectionProperties.getRemoteAeTitle(), true);
        _dcmEcho.setRemotePort(_remoteQrPort = dicomConnectionProperties.getRemotePort());
    }

    @Override
    public void cecho() {
        boolean isInError = false;
        try {
            _dcmEcho.open();
            _dcmEcho.echo();
            log.debug("Received C-ECHO response from PACS, calling from " + _aeTitle + " to " + _remoteAeTitle + ":" + _remoteQrPort + " on host " + _remoteHost + ".");
        } catch (final Exception e) {
            isInError = true;
            log.error("There was a problem running the C-ECHO command against the DICOM network connection, calling from " + _aeTitle + " to " + _remoteAeTitle + ":" + _remoteQrPort + " on host " + _remoteHost + ".", e);
            throw new RuntimeException(e);
        } finally {
            try {
                _dcmEcho.close();
            } catch (final Exception e) {
                // We'll only log an error here if we haven't already run into an error: if we have, this new error is
                // probably just a by-product of the initial error and is hiding that error.
                if (!isInError) {
                    log.error("There was a problem closing the DICOM network connection used for the C-ECHO command", e);
                }
            }
        }
    }
}
