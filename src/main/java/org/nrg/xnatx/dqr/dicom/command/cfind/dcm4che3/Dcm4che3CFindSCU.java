/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che3.Dcm4che3CFindSCU
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2024, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che3;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.ElementDictionary;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.net.DimseRSPHandler;
import org.dcm4che3.net.Priority;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.TransferCapability;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.nrg.xnatx.dqr.dicom.command.dcm4che3.Dcm4che3DicomClient;
import org.nrg.xnatx.dqr.dicom.command.dcm4che3.QueryRetrieveLevel;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * C-FIND SCU implementation using dcm4che3 API.
 * Replaces functionality of org.dcm4che2.tool.dcmqr.DcmQR for C-FIND operations.
 */
@Slf4j
public class Dcm4che3CFindSCU extends Dcm4che3DicomClient {

    @Getter @Setter
    private QueryRetrieveLevel queryLevel = QueryRetrieveLevel.STUDY;

    @Getter @Setter
    private int cancelAfter = 0; // 0 means no limit

    @Getter
    private final Attributes keys;

    private final List<Integer> returnKeys;

    public Dcm4che3CFindSCU(final String localAETitle,
                            final DicomConnectionProperties connectionProperties) {
        super(localAETitle, connectionProperties);
        this.keys = new Attributes();
        this.returnKeys = new ArrayList<>();
    }

    @Override
    protected void configureTransferCapabilities() {
        // Add transfer capabilities for all query/retrieve SOP classes
        addTransferCapability(UID.PatientRootQueryRetrieveInformationModelFind, TransferCapability.Role.SCU);
        addTransferCapability(UID.StudyRootQueryRetrieveInformationModelFind, TransferCapability.Role.SCU);
        addTransferCapability(UID.PatientStudyOnlyQueryRetrieveInformationModelFind, TransferCapability.Role.SCU);
    }

    /**
     * Adds a matching key for the query.
     * @param tagPath Array of tags representing the path (for sequences, use multiple tags)
     * @param value The value to match
     */
    public void addMatchingKey(int[] tagPath, String value) {
        if (tagPath.length == 1) {
            VR vr = ElementDictionary.vrOf(tagPath[0], null);
            keys.setString(tagPath[0], vr, value);
        } else {
            // Handle nested tags (sequences)
            Attributes current = keys;
            for (int i = 0; i < tagPath.length - 1; i++) {
                Attributes nested = current.getNestedDataset(tagPath[i]);
                if (nested == null) {
                    nested = new Attributes();
                    current.newSequence(tagPath[i], 1).add(nested);
                }
                current = nested;
            }
            int lastTag = tagPath[tagPath.length - 1];
            VR vr = ElementDictionary.vrOf(lastTag, null);
            current.setString(lastTag, vr, value);
        }
    }

    /**
     * Adds a return key (attribute to be returned in results).
     */
    public void addReturnKey(int[] tagPath) {
        if (tagPath.length == 1) {
            returnKeys.add(tagPath[0]);
        }
        // For simplicity, only support single-level return keys
    }

    /**
     * Adds default return keys for the current query level.
     */
    public void addDefReturnKeys() {
        for (int tag : queryLevel.getDefaultReturnKeys()) {
            if (!returnKeys.contains(tag)) {
                returnKeys.add(tag);
            }
        }
    }

    /**
     * Performs the C-FIND query.
     * @return List of matching Attributes objects
     */
    public List<Attributes> query() throws Exception {
        List<Attributes> results = new ArrayList<>();

        // Set query retrieve level
        keys.setString(Tag.QueryRetrieveLevel, VR.CS, queryLevel.getLevelName());

        // Add return keys as empty values
        for (int tag : returnKeys) {
            if (!keys.contains(tag)) {
                VR vr = ElementDictionary.vrOf(tag, null);
                keys.setNull(tag, vr);
            }
        }

        String sopClass = queryLevel.getFindSopClass();

        // Create associate request
        AAssociateRQ rq = createAssociateRQ(sopClass);
        open(rq);

        try {
            final CountDownLatch latch = new CountDownLatch(1);
            final int[] resultCount = {0};

            DimseRSPHandler rspHandler = new DimseRSPHandler(association.nextMessageID()) {
                @Override
                public void onDimseRSP(org.dcm4che3.net.Association as, Attributes cmd, Attributes data) {
                    super.onDimseRSP(as, cmd, data);

                    int status = cmd.getInt(Tag.Status, -1);

                    if (Status.isPending(status)) {
                        if (data != null) {
                            resultCount[0]++;
                            // Only add results up to the limit
                            if (cancelAfter <= 0 || results.size() < cancelAfter) {
                                results.add(new Attributes(data));
                            }
                            // Note: We don't try to cancel the query mid-stream as dcm4che3's cancel API
                            // requires PresentationContext which is not easily available here.
                            // The server will continue sending until done, but we just ignore extra results.
                        }
                    } else {
                        // Final response
                        if (status != Status.Success && status != Status.Cancel) {
                            log.warn("C-FIND completed with status: 0x{}", Integer.toHexString(status));
                        }
                        latch.countDown();
                    }
                }
            };

            log.debug("Sending C-FIND request with keys: {}", keys);
            association.cfind(sopClass, Priority.NORMAL, keys, null, rspHandler);

            // Wait for completion with timeout
            if (!latch.await(5, TimeUnit.MINUTES)) {
                log.warn("C-FIND operation timed out");
            }

            log.debug("C-FIND returned {} results", results.size());

        } finally {
            release();
        }

        return results;
    }

    /**
     * Clears all matching keys.
     */
    public void clearKeys() {
        keys.clear();
        returnKeys.clear();
    }

    /**
     * Opens the association.
     */
    public void open() throws Exception {
        String sopClass = queryLevel.getFindSopClass();
        AAssociateRQ rq = createAssociateRQ(sopClass);
        open(rq);
    }
}
