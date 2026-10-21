package org.nrg.xnatx.dqr.dicom.dimse;

import lombok.Getter;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.DimseRSPHandler;
import org.dcm4che3.net.Status;

import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class QrClientRspHandler extends DimseRSPHandler {
    private boolean responseReceived = false;
    private final List<Integer> failureCodes = new ArrayList<>();

    public QrClientRspHandler(int msgId) {
        super(msgId);
    }

    @Override
    public void onDimseRSP(final Association as, final Attributes cmd, final Attributes data) {
        responseReceived = true;
        final int status = cmd.getInt(Tag.Status, -1);
        // Whether a status is pending, successful, or a failure is determined by its high byte, but
        // the full status is what identifies the specific failure, so that's what gets recorded.
        final int category = status & 0xFF00;
        if (category != Status.Pending && category != Status.Success) {
            failureCodes.add(status);
        }
        super.onDimseRSP(as, cmd, data);
    }

    /**
     * Returns the first failure status reported by the PACS, which is the one that identifies why
     * the operation failed. Returns -1 when the operation didn't fail.
     *
     * @return The first failure status, or -1 if there wasn't one.
     */
    public int getFirstFailureCode() {
        return failureCodes.isEmpty() ? -1 : failureCodes.get(0);
    }

    public boolean isSuccess() {
        return failureCodes.isEmpty();
    }
}
