/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cecho.dcm4che3.Dcm4che3CEchoSCU
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2024, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cecho.dcm4che3;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che3.data.UID;
import org.dcm4che3.net.TransferCapability;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.dcm4che3.Dcm4che3DicomClient;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.exceptions.DqrRuntimeException;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

/**
 * C-ECHO SCU implementation using dcm4che3 API.
 */
@Slf4j
public class Dcm4che3CEchoSCU extends Dcm4che3DicomClient implements CEchoSCU {

    private final String localAETitle;
    private final String remoteHost;
    private final int remotePort;

    public Dcm4che3CEchoSCU(final DqrPreferences preferences,
                            final DicomConnectionProperties connectionProperties) {
        super(StringUtils.defaultIfBlank(preferences.getDqrCallingAe(), connectionProperties.getLocalAeTitle()),
              connectionProperties);

        this.localAETitle = getLocalAE().getAETitle();
        this.remoteHost = connectionProperties.getRemoteHost();
        this.remotePort = connectionProperties.getRemotePort();
    }

    @Override
    protected void configureTransferCapabilities() {
        addTransferCapability(UID.Verification, TransferCapability.Role.SCU);
    }

    @Override
    public void cecho() {
        boolean isInError = false;
        try {
            // Create associate request with Verification SOP Class
            AAssociateRQ rq = createAssociateRQ(UID.Verification);
            open(rq);

            // Send C-ECHO - dcm4che3 cecho() is synchronous and returns DimseRSP
            association.cecho(UID.Verification);

            log.debug("Received C-ECHO response from PACS, calling from {} to {}:{} on host {}",
                    localAETitle, getRemoteAETitle(), remotePort, remoteHost);

        } catch (Exception e) {
            isInError = true;
            String message = String.format(
                    "There was a problem running the C-ECHO command against the DICOM network connection, " +
                    "calling from %s to %s:%d on host %s",
                    localAETitle, getRemoteAETitle(), remotePort, remoteHost);
            log.error(message, e);
            throw new DqrRuntimeException(message, e);
        } finally {
            try {
                release();
            } catch (Exception e) {
                if (!isInError) {
                    log.error("There was a problem closing the DICOM network connection used for the C-ECHO command", e);
                }
            }
        }
    }

    @Override
    public boolean canConnect() {
        try {
            AAssociateRQ rq = createAssociateRQ(UID.Verification);
            open(rq);

            // dcm4che3 cecho() is synchronous
            association.cecho(UID.Verification);

            return true;
        } catch (Exception e) {
            log.warn("Failed to connect to AE {}:{} on host {}: {}",
                    getRemoteAETitle(), remotePort, remoteHost, e.getMessage());
            return false;
        } finally {
            try {
                release();
            } catch (Exception e) {
                log.warn("Error releasing connection", e);
            }
        }
    }
}
