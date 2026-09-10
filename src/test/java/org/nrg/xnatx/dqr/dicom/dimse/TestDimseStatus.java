package org.nrg.xnatx.dqr.dicom.dimse;

import org.dcm4che3.net.Status;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies how DIMSE statuses returned by a PACS are classified, since that classification decides
 * whether a failed retrieve is retried, reissued differently, or failed outright.
 */
class TestDimseStatus {

    @Test
    void identifierMismatchMeansTheRetrieveIsUnsupported() {
        // The status a PACS most commonly returns when it won't honor a retrieve at the requested level
        assertThat(DimseStatus.indicatesUnsupportedRetrieve(Status.IdentifierDoesNotMatchSOPClass)).isTrue();
        assertThat(DimseStatus.isPermanentFailure(Status.IdentifierDoesNotMatchSOPClass)).isTrue();
    }

    @Test
    void unsupportedSopClassMeansTheRetrieveIsUnsupported() {
        assertThat(DimseStatus.indicatesUnsupportedRetrieve(Status.SOPclassNotSupported)).isTrue();
        assertThat(DimseStatus.isPermanentFailure(Status.SOPclassNotSupported)).isTrue();
    }

    @Test
    void theWholeUnableToProcessRangeMeansTheRetrieveIsUnsupported() {
        // "Unable to process" is a range, 0xC000 through 0xCFFF, not a single status
        assertThat(DimseStatus.indicatesUnsupportedRetrieve(0xC000)).isTrue();
        assertThat(DimseStatus.indicatesUnsupportedRetrieve(0xC001)).isTrue();
        assertThat(DimseStatus.indicatesUnsupportedRetrieve(0xCFFF)).isTrue();
    }

    @Test
    void anUnknownMoveDestinationIsPermanentButNotAboutTheRetrieveLevel() {
        // Reissuing at a different level won't help: the PACS doesn't know our AE title
        assertThat(DimseStatus.isPermanentFailure(Status.MoveDestinationUnknown)).isTrue();
        assertThat(DimseStatus.indicatesUnsupportedRetrieve(Status.MoveDestinationUnknown)).isFalse();
    }

    @Test
    void outOfResourcesIsTransientSoItStaysRetryable() {
        assertThat(DimseStatus.isPermanentFailure(Status.OutOfResources)).isFalse();
        assertThat(DimseStatus.isPermanentFailure(Status.UnableToCalculateNumberOfMatches)).isFalse();
        assertThat(DimseStatus.isPermanentFailure(Status.UnableToPerformSubOperations)).isFalse();
    }

    @Test
    void unrecognizedAndAbsentStatusesStayRetryable() {
        // -1 is what the response handler reports when no failure status was recorded at all
        assertThat(DimseStatus.isPermanentFailure(-1)).isFalse();
        assertThat(DimseStatus.isPermanentFailure(Status.OneOrMoreFailures)).isFalse();
    }

    @Test
    void describeNamesTheCommonFailuresAndFallsBackToHex() {
        assertThat(DimseStatus.describe(Status.MoveDestinationUnknown)).contains("move destination");
        assertThat(DimseStatus.describe(Status.IdentifierDoesNotMatchSOPClass)).contains("query/retrieve level");
        assertThat(DimseStatus.describe(0xC123)).contains("query/retrieve level");
        assertThat(DimseStatus.describe(0x0107)).contains("0x0107");
    }

    @Test
    void statusesAreFormattedAsFourDigitHex() {
        assertThat(DimseStatus.toHex(Status.IdentifierDoesNotMatchSOPClass)).isEqualTo("0xA900");
        assertThat(DimseStatus.toHex(Status.Success)).isEqualTo("0x0000");
    }
}
