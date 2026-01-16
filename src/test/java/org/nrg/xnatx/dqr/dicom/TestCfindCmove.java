package org.nrg.xnatx.dqr.dicom;

import org.apache.commons.lang3.RandomStringUtils;
import org.dcm4che3.data.Attributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.cmove.dcm4che.tool.CFindSCUSeriesLevelByIdWithCMove;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.BasicResultSetLimitStrategy;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

import java.util.Collections;
import java.util.List;

import org.nrg.xnatx.dqr.exceptions.DqrRuntimeException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestCfindCmove {

    @Mock Attributes attributes;
    @Mock DqrPreferences preferences;
    @Mock CEchoSCU cechoSCU;
    @Mock OrmStrategy ormStrategy;
    @Mock DicomConnectionProperties dicomConnectionProperties;

    @BeforeEach
    void setUp() {
        // Set up the mock connection properties needed by the constructor
        when(dicomConnectionProperties.getLocalAeTitle()).thenReturn("LOCAL_AE");
        when(dicomConnectionProperties.getRemoteHost()).thenReturn("remotehost");
        when(dicomConnectionProperties.getRemoteAeTitle()).thenReturn("REMOTE_AE");
        when(dicomConnectionProperties.getRemotePort()).thenReturn(104);
        when(ormStrategy.getResultSetLimitStrategy()).thenReturn(new BasicResultSetLimitStrategy());
    }

    /**
     * Mock class to allow us to test CFindSCUSeriesLevelByIdWithCMove.cfind() and mock out protected methods.
     * Note: C-MOVE is not yet implemented for dcm4che3, so this test verifies that UnsupportedOperationException is thrown.
     */
    private class CFindSCUSeriesLevelByIdWithCMoveMock extends CFindSCUSeriesLevelByIdWithCMove {
        public CFindSCUSeriesLevelByIdWithCMoveMock() {
            super(preferences, dicomConnectionProperties, cechoSCU, ormStrategy);
        }

        @Override
        protected List<Attributes> setParamsAndSendQuery(final PacsSearchCriteria searchCriteria) throws Exception {
            return Collections.singletonList(attributes);
        }
    }

    @Test
    void testImportFromPacsRequestCmoveNotImplemented() {
        // Since C-MOVE is not yet implemented for dcm4che3, we test that UnsupportedOperationException is thrown

        // Set up test values
        final String studyInstanceUid = RandomStringUtils.randomAlphabetic(5);
        final String seriesInstanceUid = RandomStringUtils.randomAlphabetic(5);
        final PacsSearchCriteria searchCriteria = PacsSearchCriteria.builder()
                .studyInstanceUid(studyInstanceUid)
                .seriesInstanceUid(seriesInstanceUid)
                .build();

        // Create C-FIND/C-MOVE instance under test
        final CFindSCUSeriesLevelByIdWithCMove cFindSCUSeriesLevel = new CFindSCUSeriesLevelByIdWithCMoveMock();

        // Call the method under test and assert that DqrRuntimeException is thrown
        // wrapping UnsupportedOperationException because C-MOVE is not yet implemented for dcm4che3
        assertThatThrownBy(() -> cFindSCUSeriesLevel.cfind(searchCriteria))
                .isInstanceOf(DqrRuntimeException.class)
                .hasCauseInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("C-MOVE not yet implemented");
    }
}
