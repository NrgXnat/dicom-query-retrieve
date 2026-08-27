package org.nrg.xnatx.dqr.dicom;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The resolution order decides how every import is retrieved, and the default decides what happens
 * to sites that never touch the new setting, so both are worth pinning down.
 */
class TestRetrieveLevel {

    @Test
    void seriesIsTheDefaultSoExistingSitesAreUnaffected() {
        assertThat(RetrieveLevel.DEFAULT).isEqualTo(RetrieveLevel.SERIES);
        assertThat(RetrieveLevel.resolve(null, null)).isEqualTo(RetrieveLevel.SERIES);
    }

    @Test
    void thePacsSettingAppliesWhenTheRequestDoesNotAskForALevel() {
        assertThat(RetrieveLevel.resolve(null, RetrieveLevel.STUDY)).isEqualTo(RetrieveLevel.STUDY);
        assertThat(RetrieveLevel.resolve(null, RetrieveLevel.SERIES)).isEqualTo(RetrieveLevel.SERIES);
    }

    @Test
    void theRequestOverridesThePacsSettingInBothDirections() {
        assertThat(RetrieveLevel.resolve(RetrieveLevel.STUDY, RetrieveLevel.SERIES)).isEqualTo(RetrieveLevel.STUDY);
        assertThat(RetrieveLevel.resolve(RetrieveLevel.SERIES, RetrieveLevel.STUDY)).isEqualTo(RetrieveLevel.SERIES);
    }

    @Test
    void submittedValuesAreParsedWithoutRegardToCaseOrWhitespace() {
        assertThat(RetrieveLevel.forValue("STUDY")).isEqualTo(RetrieveLevel.STUDY);
        assertThat(RetrieveLevel.forValue("study")).isEqualTo(RetrieveLevel.STUDY);
        assertThat(RetrieveLevel.forValue("  Series  ")).isEqualTo(RetrieveLevel.SERIES);
    }

    @Test
    void aBlankValueMeansUnspecifiedRatherThanTheDefault() {
        // The distinction matters: an omitted level on a request lets the PACS setting apply,
        // whereas a defaulted one would silently override it
        assertThat(RetrieveLevel.forValue(null)).isNull();
        assertThat(RetrieveLevel.forValue("")).isNull();
        assertThat(RetrieveLevel.forValue("   ")).isNull();
    }

    @Test
    void anUnrecognizedValueIsRejected() {
        assertThatThrownBy(() -> RetrieveLevel.forValue("INSTANCE")).isInstanceOf(IllegalArgumentException.class);
    }
}
