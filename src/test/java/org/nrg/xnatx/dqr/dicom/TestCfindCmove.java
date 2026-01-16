package org.nrg.xnatx.dqr.dicom;

import org.apache.commons.lang3.RandomStringUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.BeforeEach;
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
import org.nrg.xnatx.dqr.exceptions.DqrRuntimeException;
import org.nrg.xnatx.dqr.exceptions.PacsConnectionException;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestCfindCmove {

    @Mock DqrPreferences preferences;
    @Mock CEchoSCU cechoSCU;
    @Mock OrmStrategy ormStrategy;
    @Mock DicomConnectionProperties dicomConnectionProperties;

    @BeforeEach
    void setUp() {
        // Set up the mock connection properties needed by the constructor
        when(dicomConnectionProperties.getLocalAeTitle()).thenReturn("LOCAL_AE");
        when(dicomConnectionProperties.getRemoteHost()).thenReturn("localhost");
        when(dicomConnectionProperties.getRemoteAeTitle()).thenReturn("REMOTE_AE");
        when(dicomConnectionProperties.getRemotePort()).thenReturn(11112); // Unlikely to have a PACS here
        when(ormStrategy.getResultSetLimitStrategy()).thenReturn(new BasicResultSetLimitStrategy());
    }

    /**
     * Mock class to allow us to test CFindSCUSeriesLevelByIdWithCMove.cfind() and mock out protected methods.
     */
    private class CFindSCUSeriesLevelByIdWithCMoveMock extends CFindSCUSeriesLevelByIdWithCMove {
        private final Attributes mockResult;

        public CFindSCUSeriesLevelByIdWithCMoveMock(Attributes mockResult) {
            super(preferences, dicomConnectionProperties, cechoSCU, ormStrategy);
            this.mockResult = mockResult;
        }

        @Override
        protected List<Attributes> setParamsAndSendQuery(final PacsSearchCriteria searchCriteria) throws Exception {
            return Collections.singletonList(mockResult);
        }
    }

    @Test
    void testCMoveFailsWhenCannotConnectToPacs() {
        // C-MOVE is now implemented using QrClient.
        // When there's no PACS server available, it should throw CMoveFailureException
        // wrapping a PacsConnectionException

        // Set up test values
        final String studyInstanceUid = RandomStringUtils.randomAlphabetic(20);
        final String seriesInstanceUid = RandomStringUtils.randomAlphabetic(20);
        final PacsSearchCriteria searchCriteria = PacsSearchCriteria.builder()
                .studyInstanceUid(studyInstanceUid)
                .seriesInstanceUid(seriesInstanceUid)
                .build();

        // Create mock result with SeriesInstanceUID
        final Attributes mockResult = new Attributes();
        mockResult.setString(Tag.StudyInstanceUID, VR.UI, studyInstanceUid);
        mockResult.setString(Tag.SeriesInstanceUID, VR.UI, seriesInstanceUid);

        // Create C-FIND/C-MOVE instance under test
        final CFindSCUSeriesLevelByIdWithCMove cFindSCUSeriesLevel = new CFindSCUSeriesLevelByIdWithCMoveMock(mockResult);

        // Call the method under test and assert that CMoveFailureException is thrown
        // when trying to connect to a non-existent PACS.
        // CMoveFailureException extends DqrRuntimeException, and its cause is PacsConnectionException
        assertThatThrownBy(() -> cFindSCUSeriesLevel.cfind(searchCriteria))
                .isInstanceOf(CMoveFailureException.class)
                .hasCauseInstanceOf(PacsConnectionException.class);
    }
}
