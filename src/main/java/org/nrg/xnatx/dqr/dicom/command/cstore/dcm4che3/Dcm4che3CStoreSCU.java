/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cstore.dcm4che3.Dcm4che3CStoreSCU
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2026, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cstore.dcm4che3;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.DimseRSPHandler;
import org.dcm4che3.net.InputStreamDataWriter;
import org.dcm4che3.net.Priority;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.TransferCapability;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;
import org.nrg.dcm.io.TransferCapabilityExtractor;
import org.nrg.dicomtools.utilities.DicomUtils;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.base.BaseXnatExperimentdata.UnknownPrimaryProjectException;
import org.nrg.xnatx.dqr.dicom.command.cecho.dcm4che3.Dcm4che3CEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.cstore.CStoreFailureException;
import org.nrg.xnatx.dqr.dicom.command.cstore.CStoreResults;
import org.nrg.xnatx.dqr.dicom.command.cstore.CStoreSCU;
import org.nrg.xnatx.dqr.dicom.command.dcm4che3.Dcm4che3DicomClient;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * C-STORE SCU implementation using dcm4che3 API. All of the objects for a request are sent on a single association,
 * each in the transfer syntax in which it's stored in the archive.
 */
@Slf4j
public class Dcm4che3CStoreSCU extends Dcm4che3DicomClient implements CStoreSCU {

    private final DqrPreferences            preferences;
    private final DicomConnectionProperties connectionProperties;

    public Dcm4che3CStoreSCU(final DqrPreferences preferences, final DicomConnectionProperties connectionProperties) {
        super(StringUtils.defaultIfBlank(preferences.getDqrCallingAe(), connectionProperties.getLocalAeTitle()),
              connectionProperties);

        this.preferences = preferences;
        this.connectionProperties = connectionProperties;
    }

    @Override
    protected void configureTransferCapabilities() {
        // The transfer capabilities for a C-STORE depend on the SOP classes and transfer syntaxes of the objects being
        // sent, so they're added in cStoreFiles() once the files for the request are known.
    }

    @Override
    public CStoreResults cstoreSeries(final XnatImagescandata series) throws CStoreFailureException {
        return cStoreFiles(getDicomFilesForSeries(series));
    }

    CStoreResults cStoreFiles(final List<File> dicomFiles) {
        // Verify that the PACS is reachable before spending any time reading files from the archive.
        try (final Dcm4che3CEchoSCU cechoSCU = new Dcm4che3CEchoSCU(preferences, connectionProperties)) {
            cechoSCU.cecho();
        }

        final CStoreResults results = new CStoreResults();
        try {
            final AAssociateRQ request = createStoreAssociateRQ(dicomFiles);
            try {
                open(request);
            } catch (Exception e) {
                throw new CStoreFailureException("Failed to open an association with the PACS AE " + getRemoteAETitle(), e);
            }

            for (final File file : dicomFiles) {
                try {
                    store(file);
                    if (log.isDebugEnabled()) {
                        log.debug("Successfully sent DICOM object from file {}", file.getAbsolutePath());
                    }
                    results.addSuccess(new CStoreResults.CStoreSuccess(file.getAbsolutePath()));
                } catch (Exception e) {
                    log.warn("Failed sending DICOM object from file {}", file.getAbsolutePath(), e);
                    results.addFailure(new CStoreResults.CStoreFailure(file.getAbsolutePath(), e.getMessage()));
                    throw new CStoreFailureException(e, results);
                }
            }
        } finally {
            close();
        }
        return results;
    }

    /**
     * Builds the association request for the submitted files. The SOP classes and transfer syntaxes are extracted from
     * the files themselves: each object is sent to the PACS in the transfer syntax in which it's stored, so that's what
     * has to be negotiated for the association.
     */
    private AAssociateRQ createStoreAssociateRQ(final List<File> dicomFiles) {
        final TransferCapability[] transferCapabilities = TransferCapabilityExtractor.getTransferCapabilities(dicomFiles, SCU_ROLE);
        if (transferCapabilities.length == 0) {
            throw new CStoreFailureException("Failed to extract the SOP classes and transfer syntaxes required to send the " + dicomFiles.size() + " submitted file(s) to a PACS");
        }

        final AAssociateRQ request = new AAssociateRQ();
        int                pcid    = 1;
        for (final TransferCapability transferCapability : transferCapabilities) {
            getLocalAE().addTransferCapability(transferCapability);
            request.addPresentationContext(new PresentationContext(pcid, transferCapability.getSopClass(), transferCapability.getTransferSyntaxes()));
            pcid += 2; // Presentation context IDs must be odd
        }
        return request;
    }

    private void store(final File file) throws Exception {
        // The SOP class and transfer syntax are read the same way the association's presentation contexts were
        // extracted, so the values used to send the object always match something that was negotiated.
        final Attributes header = DicomUtils.read(file, Tag.SOPInstanceUID);
        final String     cuid   = header.getString(Tag.SOPClassUID);
        final String     iuid   = header.getString(Tag.SOPInstanceUID);
        final String     tsuid  = DicomUtils.getTransferSyntaxUID(header);
        if (StringUtils.isAnyBlank(cuid, iuid)) {
            throw new CStoreFailureException("The file " + file.getAbsolutePath() + " doesn't contain the SOP class and instance UIDs required to send it to a PACS");
        }

        try (final DicomInputStream in = new DicomInputStream(newInputStream(file))) {
            // Reading the file meta information positions the stream at the start of the data set, which is then
            // streamed to the PACS as is. This preserves the object's original encoding, including compressed pixel data.
            in.readFileMetaInformation();

            final CStoreRSPHandler handler = new CStoreRSPHandler(association.nextMessageID(), file);
            association.cstore(cuid, iuid, Priority.NORMAL, new InputStreamDataWriter(in), tsuid, handler);
            association.waitForOutstandingRSP();
            handler.validate();
        }
    }

    private List<File> getDicomFilesForSeries(final XnatImagescandata series) {
        for (final XnatAbstractresourceI resource : series.getFile()) {
            if (resource instanceof XnatResource) {
                if (((XnatResource) resource).getFormat().equals(DICOM_RESOURCE_FORMAT)) {
                    try {
                        return ((XnatResource) resource).getCorrespondingFiles(series.getImageSessionData().getArchiveRootPath());
                    } catch (UnknownPrimaryProjectException e) {
                        throw new CStoreFailureException("Could not locate the primary project for the following series:\n" + series);
                    }
                }
            }
        }
        throw new CStoreFailureException("Could not locate DICOM resources for the following series:\n" + series);
    }

    private static InputStream newInputStream(final File file) throws IOException {
        final InputStream in = Files.newInputStream(file.toPath());
        return new BufferedInputStream(file.getName().endsWith(GZIP_SUFFIX) ? new GZIPInputStream(in) : in);
    }

    /**
     * Captures the status returned by the PACS for a single stored object.
     */
    private static class CStoreRSPHandler extends DimseRSPHandler {
        private final File file;

        private int    status = Status.ProcessingFailure;
        private String errorComment;

        private CStoreRSPHandler(final int messageId, final File file) {
            super(messageId);
            this.file = file;
        }

        @Override
        public void onDimseRSP(final Association association, final Attributes command, final Attributes data) {
            super.onDimseRSP(association, command, data);
            status = command.getInt(Tag.Status, Status.ProcessingFailure);
            errorComment = command.getString(Tag.ErrorComment);
        }

        /**
         * Throws {@link CStoreFailureException} unless the PACS accepted the object. Warning statuses mean the object
         * was stored, e.g. with data elements coerced or discarded, so those are logged but not treated as failures.
         */
        private void validate() {
            if (status == Status.Success) {
                return;
            }
            if ((status & 0xF000) == 0xB000) {
                log.warn("The PACS returned warning status {} storing the DICOM object from file {}{}", String.format("%04X", status), file.getAbsolutePath(), StringUtils.isBlank(errorComment) ? "" : ": " + errorComment);
                return;
            }
            throw new CStoreFailureException(String.format("The PACS returned status %04X storing the DICOM object from file %s%s", status, file.getAbsolutePath(), StringUtils.isBlank(errorComment) ? "" : ": " + errorComment));
        }
    }

    private static final String DICOM_RESOURCE_FORMAT = "DICOM";
    private static final String GZIP_SUFFIX           = ".gz";
    private static final String SCU_ROLE              = "SCU";
}
