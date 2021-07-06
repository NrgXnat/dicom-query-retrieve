/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cecho.dcm4che.tool.Dcm4cheToolCEchoSCU
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cecho.dcm4che.tool;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che2.net.ConfigurationException;
import org.dcm4che2.tool.dcmecho.DcmEcho;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.exceptions.DqrRuntimeException;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

import java.io.IOException;
import java.net.SocketTimeoutException;

@Slf4j
public class Dcm4cheToolCEchoSCU implements CEchoSCU {
    public Dcm4cheToolCEchoSCU(final DqrPreferences preferences, final DicomConnectionProperties dicomConnectionProperties) {
        _dcmEcho = new DcmEcho(_aeTitle = StringUtils.defaultIfBlank(preferences.getDqrCallingAe(), dicomConnectionProperties.getLocalAeTitle()));
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
            log.debug("Received C-ECHO response from PACS, calling from {} to {}:{} on host {}.", _aeTitle, _remoteAeTitle, _remoteQrPort, _remoteHost);
        } catch (final Exception e) {
            isInError = true;
            final String message = String.format("There was a problem running the C-ECHO command against the DICOM network connection, calling from %s to %s:%d on host %s.", _aeTitle, _remoteAeTitle, _remoteQrPort, _remoteHost);
            log.error(message, e);
            throw new DqrRuntimeException(message, e);
        } finally {
            try {
                _dcmEcho.close();
            } catch (NullPointerException ignored) {
                // DcmEcho throws these intermittently when closing the association, for unknown reasons
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
        } catch (SocketTimeoutException e) {
            log.warn("Tried to check the connection to AE {}:{} on host {} but the connection timed out. Verify that you can reach the host on the specified port.", _remoteAeTitle, _remoteQrPort, _remoteHost);
        } catch (IOException e) {
            log.warn("An error occurred trying to check the connection to AE {}:{} on host {}", _remoteAeTitle, _remoteQrPort, _remoteHost, e);
        } catch (ConfigurationException e) {
            log.warn("A configuration error occurred trying to check the connection to AE {}:{} on host {}", _remoteAeTitle, _remoteQrPort, _remoteHost, e);
        } catch (InterruptedException e) {
            log.warn("Tried to check the connection to AE {}:{} on host {} but the connection was interrupted", _remoteAeTitle, _remoteQrPort, _remoteHost, e);
        } finally {
            try {
                _dcmEcho.close();
            } catch (NullPointerException ignored) {
                // DcmEcho throws these intermittently when closing the association, for unknown reasons
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
