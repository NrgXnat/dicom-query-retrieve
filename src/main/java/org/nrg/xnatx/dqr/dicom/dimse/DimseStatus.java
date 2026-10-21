package org.nrg.xnatx.dqr.dicom.dimse;

import org.dcm4che3.net.Status;

/**
 * Interpretation of the DIMSE status codes a PACS returns for query and retrieve operations, as
 * defined in DICOM PS3.4 Annex C.
 * <p>
 * The distinction that matters operationally is between a failure that repeating the request might
 * clear (the PACS was momentarily out of resources) and one that it can't (the PACS rejected the
 * request as formed). Without it, a rejection is retried until the request's retry budget is spent
 * and the underlying reason never surfaces.
 */
public class DimseStatus {
    private DimseStatus() {
    }

    /**
     * Indicates whether the status means the PACS could not handle the request as it was formed,
     * most often because it doesn't support a retrieve at the requested query/retrieve level. This
     * is the signal to try the request a different way rather than to try it again.
     *
     * @param status The DIMSE status returned by the PACS.
     *
     * @return Returns <b>true</b> if the status indicates the request itself was unacceptable.
     */
    public static boolean indicatesUnsupportedRetrieve(final int status) {
        return status == Status.IdentifierDoesNotMatchSOPClass
               || status == Status.SOPclassNotSupported
               || isUnableToProcess(status);
    }

    /**
     * Indicates whether repeating the identical request has any prospect of succeeding. This
     * covers everything {@link #indicatesUnsupportedRetrieve(int)} covers, plus failures of
     * configuration such as a move destination the PACS doesn't recognize.
     *
     * @param status The DIMSE status returned by the PACS.
     *
     * @return Returns <b>true</b> if the failure is permanent.
     */
    public static boolean isPermanentFailure(final int status) {
        return indicatesUnsupportedRetrieve(status) || status == Status.MoveDestinationUnknown;
    }

    /**
     * Returns a description of the status suitable for an error message shown to an administrator,
     * falling back to the hexadecimal status for codes without a specific meaning here.
     *
     * @param status The DIMSE status returned by the PACS.
     *
     * @return A description of the status.
     */
    public static String describe(final int status) {
        switch (status) {
            case Status.Success:
                return "success";
            case Status.Cancel:
                return "the operation was cancelled";
            case Status.OutOfResources:
                return "the PACS was out of resources";
            case Status.UnableToCalculateNumberOfMatches:
                return "the PACS was out of resources and could not calculate the number of matches";
            case Status.UnableToPerformSubOperations:
                return "the PACS was out of resources and could not perform the sub-operations";
            case Status.MoveDestinationUnknown:
                return "the PACS does not recognize the move destination AE title";
            case Status.IdentifierDoesNotMatchSOPClass:
                return "the PACS rejected the identifier as not matching the SOP class, which usually means it does not support a retrieve at this query/retrieve level";
            case Status.SOPclassNotSupported:
                return "the PACS does not support the requested SOP class";
            case Status.OneOrMoreFailures:
                return "the sub-operations completed with one or more failures";
            default:
                return isUnableToProcess(status)
                       ? "the PACS was unable to process the request, which often means it does not support a retrieve at this query/retrieve level"
                       : "the PACS returned status " + toHex(status);
        }
    }

    /**
     * Formats a status as a four-digit hexadecimal value, the form used throughout the DICOM
     * standard and by most PACS logs.
     *
     * @param status The DIMSE status to format.
     *
     * @return The formatted status.
     */
    public static String toHex(final int status) {
        return String.format("0x%04X", status);
    }

    /**
     * The "unable to process" failures occupy the range 0xC000 to 0xCFFF rather than a single code.
     */
    private static boolean isUnableToProcess(final int status) {
        return (status & 0xF000) == Status.UnableToProcess;
    }
}
