/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cstore.dcm4che3.Dcm4che3CStoreSCUTest
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2026, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cstore.dcm4che3;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.PDVInputStream;
import org.dcm4che3.net.TransferCapability;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCEchoSCP;
import org.dcm4che3.net.service.BasicCStoreSCP;
import org.dcm4che3.net.service.DicomServiceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nrg.xnatx.dqr.dicom.command.cstore.CStoreFailureException;
import org.nrg.xnatx.dqr.dicom.command.cstore.CStoreResults;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests {@link Dcm4che3CStoreSCU} against an in-process dcm4che3 storage SCP.
 */
@ExtendWith(MockitoExtension.class)
class Dcm4che3CStoreSCUTest {

    private static final String LOCAL_AE  = "DQR_TEST";
    private static final String REMOTE_AE = "TEST_SCP";

    @Mock
    DqrPreferences preferences;

    @TempDir
    Path folder;

    private final Map<String, Attributes> stored           = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, String>     storedTransferTs = Collections.synchronizedMap(new HashMap<>());

    private Device                   scp;
    private ExecutorService          executor;
    private ScheduledExecutorService scheduledExecutor;
    private int                      port;

    @BeforeEach
    void startScp() throws Exception {
        try (final ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        final Connection connection = new Connection("dicom", "localhost", port);

        final ApplicationEntity ae = new ApplicationEntity(REMOTE_AE);
        ae.setAssociationAcceptor(true);
        ae.addConnection(connection);
        ae.addTransferCapability(new TransferCapability(null, "*", TransferCapability.Role.SCP, "*"));

        executor = Executors.newCachedThreadPool();
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

        scp = new Device("test-scp");
        scp.addConnection(connection);
        scp.addApplicationEntity(ae);
        scp.setExecutor(executor);
        scp.setScheduledExecutor(scheduledExecutor);

        final DicomServiceRegistry registry = new DicomServiceRegistry();
        registry.addDicomService(new BasicCEchoSCP());
        registry.addDicomService(new BasicCStoreSCP("*") {
            @Override
            protected void store(final Association association, final PresentationContext pc, final Attributes request, final PDVInputStream data, final Attributes response) throws IOException {
                final String transferSyntax = pc.getTransferSyntax();
                final String sopInstanceUid = request.getString(Tag.AffectedSOPInstanceUID);
                stored.put(sopInstanceUid, data.readDataset(transferSyntax));
                storedTransferTs.put(sopInstanceUid, transferSyntax);
            }
        });
        scp.setDimseRQHandler(registry);
        scp.bindConnections();
    }

    @AfterEach
    void stopScp() {
        if (scp != null) {
            scp.unbindConnections();
        }
        if (executor != null) {
            executor.shutdown();
        }
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
        }
    }

    /**
     * This is the end-to-end regression test for the C-STORE path: sending objects that are stored in different
     * transfer syntaxes requires the SCU to negotiate a presentation context for each of them and to send each object
     * in its own transfer syntax, with pixel data intact.
     */
    @Test
    void testSendsEveryObjectInItsOwnTransferSyntax() throws Exception {
        when(preferences.getDqrCallingAe()).thenReturn(LOCAL_AE);

        final byte[] explicitPixelData = pixelData((byte) 0x0A);
        final byte[] implicitPixelData = pixelData((byte) 0x0B);

        final File explicit = writeDicomFile("explicit.dcm", UID.MRImageStorage, "1.2.3.4.1", UID.ExplicitVRLittleEndian, explicitPixelData);
        final File implicit = writeDicomFile("implicit.dcm", UID.SecondaryCaptureImageStorage, "1.2.3.4.2", UID.ImplicitVRLittleEndian, implicitPixelData);

        final CStoreResults results = buildCStoreSCU().cStoreFiles(Arrays.asList(explicit, implicit));

        assertThat(toList(results.getSuccesses())).hasSize(2);
        assertThat(toList(results.getFailures())).isEmpty();

        assertThat(stored).containsOnlyKeys("1.2.3.4.1", "1.2.3.4.2");
        assertThat(storedTransferTs).containsEntry("1.2.3.4.1", UID.ExplicitVRLittleEndian)
                                    .containsEntry("1.2.3.4.2", UID.ImplicitVRLittleEndian);

        // The objects have to arrive at the PACS complete: reading them into memory in a way that drops bulk data
        // would send image objects with no pixel data.
        assertThat(stored.get("1.2.3.4.1").getBytes(Tag.PixelData)).isEqualTo(explicitPixelData);
        assertThat(stored.get("1.2.3.4.2").getBytes(Tag.PixelData)).isEqualTo(implicitPixelData);
        assertThat(stored.get("1.2.3.4.1").getString(Tag.PatientName)).isEqualTo("Test^Patient");
    }

    @Test
    void testFailsWhenPacsIsUnavailable() throws Exception {
        when(preferences.getDqrCallingAe()).thenReturn(LOCAL_AE);

        final File file = writeDicomFile("explicit.dcm", UID.MRImageStorage, "1.2.3.4.1", UID.ExplicitVRLittleEndian, pixelData((byte) 0x0A));

        scp.unbindConnections();

        assertThatThrownBy(() -> buildCStoreSCU().cStoreFiles(Collections.singletonList(file))).isInstanceOf(RuntimeException.class);
        assertThat(stored).isEmpty();
    }

    @Test
    void testFailsWhenSeriesContainsNoDicomFiles() {
        assertThatThrownBy(() -> buildCStoreSCU().cStoreFiles(Collections.emptyList())).isInstanceOf(CStoreFailureException.class)
                                                                                       .hasMessageContaining("0 submitted file(s)");
    }

    private Dcm4che3CStoreSCU buildCStoreSCU() {
        final Pacs pacs = Pacs.builder().aeTitle(REMOTE_AE).host("localhost").label("test-pacs").queryRetrievePort(port).storable(true).build();
        return new Dcm4che3CStoreSCU(preferences, new DicomConnectionProperties(LOCAL_AE, pacs));
    }

    private File writeDicomFile(final String name, final String sopClassUid, final String sopInstanceUid, final String transferSyntaxUid, final byte[] pixels) throws IOException {
        final Attributes attributes = new Attributes();
        attributes.setString(Tag.SOPClassUID, VR.UI, sopClassUid);
        attributes.setString(Tag.SOPInstanceUID, VR.UI, sopInstanceUid);
        attributes.setString(Tag.StudyInstanceUID, VR.UI, "1.2.3.4");
        attributes.setString(Tag.SeriesInstanceUID, VR.UI, "1.2.3.4.0");
        attributes.setString(Tag.PatientName, VR.PN, "Test^Patient");
        attributes.setString(Tag.PatientID, VR.LO, "TEST_PATIENT");
        attributes.setString(Tag.PhotometricInterpretation, VR.CS, "MONOCHROME2");
        attributes.setInt(Tag.SamplesPerPixel, VR.US, 1);
        attributes.setInt(Tag.Rows, VR.US, 4);
        attributes.setInt(Tag.Columns, VR.US, 4);
        attributes.setInt(Tag.BitsAllocated, VR.US, 8);
        attributes.setInt(Tag.BitsStored, VR.US, 8);
        attributes.setInt(Tag.HighBit, VR.US, 7);
        attributes.setInt(Tag.PixelRepresentation, VR.US, 0);
        attributes.setBytes(Tag.PixelData, VR.OB, pixels);

        final File file = folder.resolve(name).toFile();
        try (final DicomOutputStream out = new DicomOutputStream(file)) {
            out.writeDataset(attributes.createFileMetaInformation(transferSyntaxUid), attributes);
        }
        return file;
    }

    private static byte[] pixelData(final byte value) {
        final byte[] pixels = new byte[16];
        Arrays.fill(pixels, value);
        return pixels;
    }

    private static <T> List<T> toList(final Iterator<T> iterator) {
        final List<T> items = new ArrayList<>();
        iterator.forEachRemaining(items::add);
        return items;
    }
}
