package org.nrg.xnatx.dqr.services.impl;

import org.junit.jupiter.api.Test;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.exceptions.PacsRetrieveNotSupportedException;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DICOMweb retrieval streams one series at a time, so there's no study-level equivalent of the
 * C-MOVE. A DICOMweb PACS is rejected at validation if it's set to study level, and this is the
 * backstop behind that.
 */
class TestDicomWebPacsClientServiceRetrieve {

    @Test
    void aStudyLevelRetrieveIsRefusedWithAnActionableMessage() {
        final DicomWebPacsClientService service =
                new DicomWebPacsClientService(null, null, Collections.emptyMap(), null, null);
        final Pacs pacs = Pacs.builder().label("Orthanc").aeTitle("ORTHANC").dicomWebEnabled(true).build();

        assertThatThrownBy(() -> service.importStudy(pacs, null, Study.builder().studyInstanceUid("1.2.3").build(), "XNAT"))
                .isInstanceOf(PacsRetrieveNotSupportedException.class)
                .hasMessageContaining("Orthanc")
                .hasMessageContaining("SERIES");
    }
}
