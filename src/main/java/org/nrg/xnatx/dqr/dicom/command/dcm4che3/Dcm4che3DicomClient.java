/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.dcm4che3.Dcm4che3DicomClient
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2024, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.dcm4che3;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.UID;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.TransferCapability;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;

import java.io.Closeable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Base class for dcm4che3 DICOM SCU implementations.
 * Provides common setup for Device, ApplicationEntity, Connection, and Association.
 */
@Slf4j
public abstract class Dcm4che3DicomClient implements Closeable {

    protected static final String[] TRANSFER_SYNTAXES = {
            UID.ImplicitVRLittleEndian,
            UID.ExplicitVRLittleEndian,
            UID.ExplicitVRBigEndian
    };

    @Getter
    private final Device device;
    @Getter
    private final ApplicationEntity localAE;
    @Getter
    private final Connection localConnection;
    @Getter
    private final Connection remoteConnection;
    @Getter
    private final String remoteAETitle;

    private final ExecutorService executor;
    private final ScheduledExecutorService scheduledExecutor;

    protected Association association;

    protected Dcm4che3DicomClient(final String localAETitle,
                                   final DicomConnectionProperties connectionProperties) {
        this.remoteAETitle = connectionProperties.getRemoteAeTitle();

        // Create local connection
        this.localConnection = new Connection();

        // Create local AE
        this.localAE = new ApplicationEntity(localAETitle);
        this.localAE.addConnection(localConnection);
        this.localAE.setAssociationInitiator(true);

        // Add transfer capabilities - subclasses should call addTransferCapability
        configureTransferCapabilities();

        // Create device
        this.device = new Device("DQR-" + localAETitle);
        this.device.addConnection(localConnection);
        this.device.addApplicationEntity(localAE);

        // Create executors for async operations
        this.executor = Executors.newCachedThreadPool();
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        this.device.setExecutor(executor);
        this.device.setScheduledExecutor(scheduledExecutor);

        // Create remote connection
        this.remoteConnection = new Connection();
        this.remoteConnection.setHostname(connectionProperties.getRemoteHost());
        this.remoteConnection.setPort(connectionProperties.getRemotePort());
    }

    /**
     * Subclasses must implement this to add their required transfer capabilities.
     */
    protected abstract void configureTransferCapabilities();

    /**
     * Helper method to add a transfer capability.
     */
    protected void addTransferCapability(final String sopClass, final TransferCapability.Role role) {
        localAE.addTransferCapability(new TransferCapability(null, sopClass, role, TRANSFER_SYNTAXES));
    }

    /**
     * Opens an association with the remote AE.
     */
    protected void open(final AAssociateRQ rq) throws Exception {
        rq.setCalledAET(remoteAETitle);
        rq.setCallingAET(localAE.getAETitle());

        log.debug("Opening association to {}@{}:{}",
                remoteAETitle,
                remoteConnection.getHostname(),
                remoteConnection.getPort());

        association = localAE.connect(remoteConnection, rq);

        log.debug("Association opened successfully");
    }

    /**
     * Releases the association gracefully.
     */
    protected void release() {
        if (association != null && association.isReadyForDataTransfer()) {
            try {
                association.release();
                log.debug("Association released");
            } catch (Exception e) {
                log.warn("Error releasing association", e);
            }
        }
    }

    /**
     * Aborts the association.
     */
    protected void abort() {
        if (association != null) {
            try {
                association.abort();
                log.debug("Association aborted");
            } catch (Exception e) {
                log.warn("Error aborting association", e);
            }
        }
    }

    @Override
    public void close() {
        release();
        executor.shutdown();
        scheduledExecutor.shutdown();
    }

    /**
     * Creates an AAssociateRQ with the given presentation contexts.
     */
    protected AAssociateRQ createAssociateRQ(final String... sopClasses) {
        AAssociateRQ rq = new AAssociateRQ();
        int pcid = 1;
        for (String sopClass : sopClasses) {
            rq.addPresentationContext(new PresentationContext(pcid, sopClass, TRANSFER_SYNTAXES));
            pcid += 2; // Presentation context IDs must be odd
        }
        return rq;
    }
}
