package org.nrg.xnatx.dqr.dicom.dimse;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.UID;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.IncompatibleConnectionException;
import org.dcm4che3.net.NoPresentationContextException;
import org.dcm4che3.net.QueryOption;
import org.dcm4che3.net.State;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.ExtendedNegotiation;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.pdu.RoleSelection;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.xnatx.dqr.exceptions.PacsConnectionException;
import org.nrg.xnatx.dqr.exceptions.PacsException;
import org.nrg.xnatx.dqr.exceptions.PacsPermanentFailureException;
import org.nrg.xnatx.dqr.exceptions.PacsQueryException;
import org.nrg.xnatx.dqr.exceptions.PacsRetrieveNotSupportedException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Simple DIMSE Q/R Client for performing c-find and c-move SCU operations.
 */
@Slf4j
public class QrClient implements AutoCloseable {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private final AAssociateRQ request = new AAssociateRQ();
    private final String destination;

    private final Association association;

    public enum QueryRetrieveLevel {
        STUDY, SERIES, IMAGE, PATIENT
    }

    public static final String[] TRANSFER_SYNTAX_CHAIN = {
            UID.ExplicitVRLittleEndian, UID.ImplicitVRLittleEndian
    };

    /**
     * Creates a new QrClient and establishes a dicom association with the remote host.
     *
     * @param localAe     - The local Ae title
     * @param remoteHost  - The remote host
     * @param remotePort  - The remote port
     * @param remoteAe    - The remote Ae title
     * @param destination - The move destination (if performing a c-move)
     * @param timeouts    - The connection timeouts to apply, or null to use {@link DimseTimeouts#DEFAULTS}
     * @throws PacsConnectionException - If we were unable to establish a connection to the remote host
     */
    @Builder
    public QrClient(final String localAe, final String remoteHost, final int remotePort,
                    final String remoteAe, @Nullable final String destination,
                    @Nullable final DimseTimeouts timeouts) throws PacsConnectionException {

        this.destination = destination;

        final DimseTimeouts effectiveTimeouts = timeouts != null ? timeouts : DimseTimeouts.DEFAULTS;

        final Connection local = new Connection();
        local.setResponseTimeout(effectiveTimeouts.getResponseTimeoutMs());
        local.setRetrieveTimeout(effectiveTimeouts.getRetrieveTimeoutMs());
        local.setAcceptTimeout(effectiveTimeouts.getAcceptTimeoutMs());
        local.setConnectTimeout(effectiveTimeouts.getConnectTimeoutMs());

        final Connection remote = new Connection();
        remote.setHostname(remoteHost);
        remote.setPort(remotePort);

        final ApplicationEntity applicationEntity = new ApplicationEntity();
        applicationEntity.addConnection(local);

        request.setCalledAET(remoteAe);
        request.setCallingAET(localAe);
        configurePresentationContext();

        final Device device = new Device();
        device.setDeviceName(localAe);
        device.addConnection(local);
        device.setExecutor(executorService);
        device.setScheduledExecutor(scheduledExecutorService);
        device.addApplicationEntity(applicationEntity);

        try {
            association = applicationEntity.connect(local, remote, request);
            log.debug("QRClient association opened");
        } catch (IncompatibleConnectionException | GeneralSecurityException | IOException e) {
            close();
            throw new PacsConnectionException("Failed to open connection to remote AE ", e);
        } catch (InterruptedException e) {
            close();
            Thread.currentThread().interrupt();
            throw new NrgServiceRuntimeException("Interrupted while attempting to open connection", e);
        } catch (Exception e) {
            log.error("Unexpected error while opening connection to remote AE", e);
            close();
            throw e;
        }
    }

    @Override
    public void close() {
        try {
            if (association != null && association.isReadyForDataTransfer()) {
                try {
                    association.waitForOutstandingRSP();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new NrgServiceRuntimeException("Interrupted while waiting for DIMSE response", e);
                }
            }
        } finally {
            if (association != null && association.getState() != State.Sta1) {
                try {
                    association.release();
                } catch (IOException e) {
                    log.error("Failed to release association", e);
                }
            }
            executorService.shutdown();
            scheduledExecutorService.shutdown();
        }
    }

    /**
     * Performs a c-find operation
     *
     * @param query    - The query as an Attributes object
     * @param onDataset - The callback method to execute on each dataset retrieved
     * @throws PacsConnectionException When an error occurs connecting to the PACS
     */
    public void query(final Attributes query, @Nullable final Consumer<Attributes> onDataset) throws PacsConnectionException {
        final CfindRspHandler rspHandler = new CfindRspHandler(association.nextMessageID(), Integer.MAX_VALUE, onDataset);
        try {
            log.debug("QRClient: Performing C-FIND with keys: {}", query);
            association.cfind(UID.StudyRootQueryRetrieveInformationModelFind, 0, query, null, rspHandler);
            association.waitForOutstandingRSP();
            if (!rspHandler.isResponseReceived()) {
                throw new PacsConnectionException("no response to C-FIND request");
            }
            if (!rspHandler.isSuccess()) {
                throw new PacsConnectionException("C-FIND failed: " + DimseStatus.describe(rspHandler.getFirstFailureCode())
                        + " [status code(s) " + formatStatusCodes(rspHandler.getFailureCodes()) + "]");
            }
        } catch (IOException e) {
            throw new PacsConnectionException("C-FIND failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NrgServiceRuntimeException("Interrupted while performing C-FIND ", e);
        }
    }

    /**
     * Performs a c-move operation
     *
     * @param attributes - The attributes identifying the object we are wanting to move
     */
    public void move(final Attributes attributes) throws PacsException {
        if (null == destination) {
            throw new IllegalArgumentException("Unable to perform c-move. Destination is null");
        }

        final CmoveRspHandler rspHandler = new CmoveRspHandler(association.nextMessageID());
        try {
            association.cmove(UID.StudyRootQueryRetrieveInformationModelMove, 0, attributes, null, destination, rspHandler);
            association.waitForOutstandingRSP();
            if (!rspHandler.isResponseReceived()) {
                throw new PacsConnectionException("PACS did not respond to C-MOVE request");
            }
            if (!rspHandler.isSuccess()) {
                throw toCMoveFailure(rspHandler);
            }
            log.debug("C-Move -- Completed: {}", rspHandler.getCompleted());
        } catch (NoPresentationContextException e) {
            // The PACS accepted the association but not a presentation context for the retrieve,
            // which is a refusal of the operation rather than a broken connection.
            throw new PacsRetrieveNotSupportedException("The PACS did not accept a presentation context for the C-MOVE: " + e.getMessage(),
                    Status.SOPclassNotSupported, e);
        } catch (IOException e) {
            throw new PacsConnectionException("Lost connection to PACS during C-MOVE", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NrgServiceRuntimeException("Interrupted while performing C-Move", e);
        }
    }

    /**
     * Builds the exception for a C-MOVE the PACS answered with a failure status. A PACS that
     * rejects the request as formed gets a {@link PacsRetrieveNotSupportedException} so the caller
     * can reissue it differently, and one that failed for a reason retrying won't fix gets a
     * {@link PacsPermanentFailureException} so it isn't retried; everything else stays retryable.
     *
     * @param rspHandler The response handler holding the statuses returned by the PACS.
     *
     * @return The exception to throw for this failure.
     */
    private static PacsException toCMoveFailure(final CmoveRspHandler rspHandler) {
        final int    status  = rspHandler.getFirstFailureCode();
        final String message = "C-MOVE failed: " + DimseStatus.describe(status)
                               + " [status code(s) " + formatStatusCodes(rspHandler.getFailureCodes())
                               + ", failed: " + rspHandler.getFailed()
                               + ", completed: " + rspHandler.getCompleted() + "]";

        if (DimseStatus.indicatesUnsupportedRetrieve(status)) {
            return new PacsRetrieveNotSupportedException(message, status);
        }
        if (DimseStatus.isPermanentFailure(status)) {
            return new PacsPermanentFailureException(message, status);
        }
        return new PacsQueryException(message);
    }

    private static String formatStatusCodes(final List<Integer> statusCodes) {
        return statusCodes.stream().map(DimseStatus::toHex).collect(Collectors.joining(", "));
    }

    private void configurePresentationContext() {
        request.addPresentationContext(
                new PresentationContext(request.getNumberOfPresentationContexts() * 2 + 1,
                        UID.StudyRootQueryRetrieveInformationModelFind, TRANSFER_SYNTAX_CHAIN));
        if (null != destination) {
            request.addPresentationContext(
                    new PresentationContext(request.getNumberOfPresentationContexts() * 2 + 1,
                            UID.StudyRootQueryRetrieveInformationModelMove, TRANSFER_SYNTAX_CHAIN));
        }
        request.addExtendedNegotiation(
                new ExtendedNegotiation(UID.StudyRootQueryRetrieveInformationModelFind,
                        QueryOption.toExtendedNegotiationInformation(EnumSet.of(QueryOption.DATETIME))));

        request.addRoleSelection(
                new RoleSelection(UID.StudyRootQueryRetrieveInformationModelFind, true, false));
    }
}

