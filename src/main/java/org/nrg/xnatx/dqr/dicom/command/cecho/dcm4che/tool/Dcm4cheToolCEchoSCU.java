/*
 * Dcm4cheToolCEchoSCU
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.dicom.command.cecho.dcm4che.tool;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che2.net.ConfigurationException;
import org.dcm4che2.tool.dcmecho.DcmEcho;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

@Slf4j
public class Dcm4cheToolCEchoSCU implements CEchoSCU {
    public Dcm4cheToolCEchoSCU(final DqrPreferences preferences, final DicomConnectionProperties dicomConnectionProperties) {
        Object callingAeObject = preferences.get("dqrCallingAe");
        String callingAe       = dicomConnectionProperties.getLocalAeTitle();
        if (callingAeObject != null && callingAeObject.toString() != null) {
            callingAe = callingAeObject.toString();
        }
        _dcmEcho = new DcmEcho(_aeTitle = callingAe);
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
            log.error("There was a problem running the C-ECHO command against the DICOM network connection, calling from {} to {}:{} on host {}.", _aeTitle, _remoteAeTitle, _remoteQrPort, _remoteHost, e);
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

    @Override
    public boolean canConnect() {
        try {
            _dcmEcho.open();
            _dcmEcho.echo();
            return true;
        } catch (IOException e) {
            log.warn("An error occurred trying to check the connection to AE {}:{} on host {}", _remoteAeTitle, _remoteQrPort, _remoteHost, e);
        } catch (ConfigurationException e) {
            log.warn("A configuration error occurred trying to check the connection to AE {}:{} on host {}", _remoteAeTitle, _remoteQrPort, _remoteHost, e);
        } catch (InterruptedException e) {
            log.warn("Tried to check the connection to AE {}:{} on host {} but the connection was interrupted", _remoteAeTitle, _remoteQrPort, _remoteHost, e);
        } finally {
            try {
                _dcmEcho.close();
            } catch (InterruptedException e) {
                log.warn("Tried to close the connection to AE {}:{} on host {} but the connection was interrupted", _remoteAeTitle, _remoteQrPort, _remoteHost, e);
            }
        }
        return false;
    }

    private final DcmEcho _dcmEcho;
    private final String  _aeTitle;
    private final String  _remoteHost;
    private final String  _remoteAeTitle;
    private final int     _remoteQrPort;
}
