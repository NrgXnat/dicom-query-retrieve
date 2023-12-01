package org.nrg.xnatx.dqr.dicom;

import org.apache.commons.lang3.RandomStringUtils;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.tool.dcmqr.DcmQR;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.cmove.CMoveFailureException;
import org.nrg.xnatx.dqr.dicom.command.cmove.dcm4che.tool.CFindSCUSeriesLevelByIdWithCMove;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.BasicResultSetLimitStrategy;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestCfindCmove {

    @Mock DicomObject dicomObject;
    @Mock DcmQR dcmQR;
    @Mock DqrPreferences preferences;
    @Mock CEchoSCU cechoSCU;
    @Mock OrmStrategy ormStrategy;
    @Mock DicomConnectionProperties dicomConnectionProperties;

    /**
     * Mock class to allow us to test CFindSCUSeriesLevelByIdWithCMove.cfind() and mock out protected methods
     */
    private class CFindSCUSeriesLevelByIdWithCMoveMock extends CFindSCUSeriesLevelByIdWithCMove {
        public CFindSCUSeriesLevelByIdWithCMoveMock() {
            super(preferences, dicomConnectionProperties, cechoSCU, ormStrategy);
        }

        @Override
        protected DcmQR createDcmQR(final String localAETitle) {
            return dcmQR;
        }

        @Override
        protected List<DicomObject> setParamsAndSendQuery(final PacsSearchCriteria searchCriteria) throws IOException, InterruptedException {
            return Collections.singletonList(dicomObject);
        }
    }

    @Test
    void testImportFromPacsRequestCmoveFails() throws Exception {
        // BasicDicomQueryRetrieveService.importFromPacs() calls buildCMoveSCU()
        // That makes a new Dcm4cheToolCMoveSCU() on which we call cmoveSeries()
        // That calls CMoveSCUSeriesLevel.cmove(), which calls CFindSCUSeriesLevelByIdWithCMove.cfind()
        // The latter is what we will test here.

        // Set up mocks and test values
        final String hostname = RandomStringUtils.randomAlphabetic(5);
        final int numFailed = ThreadLocalRandom.current().nextInt(1, 100);
        final String studyInstanceUid = RandomStringUtils.randomAlphabetic(5);
        final String seriesInstanceUid = RandomStringUtils.randomAlphabetic(5);
        final PacsSearchCriteria searchCriteria = PacsSearchCriteria.builder()
                .studyInstanceUid(studyInstanceUid)
                .seriesInstanceUid(seriesInstanceUid)
                .build();

        when(ormStrategy.getResultSetLimitStrategy()).thenReturn(new BasicResultSetLimitStrategy());
        when(dcmQR.getKeys()).thenReturn(dicomObject);
        when(dicomConnectionProperties.getRemoteHost()).thenReturn(hostname);

        // Simulate a failed C-MOVE
        when(dcmQR.getFailed()).thenReturn(numFailed);

        // Create C-FIND/C-MOVE instance under test
        final CFindSCUSeriesLevelByIdWithCMove cFindSCUSeriesLevel = new CFindSCUSeriesLevelByIdWithCMoveMock();

        // Call the method under test and assert
        assertThatThrownBy(() -> cFindSCUSeriesLevel.cfind(searchCriteria))
                .isInstanceOf(CMoveFailureException.class)
                .hasMessageContaining("C-MOVE failed for %d objects", numFailed);
    }
}