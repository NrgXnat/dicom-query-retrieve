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
        final int status = cmd.getInt(Tag.Status, -1) & '\uff00';
        if (status != Status.Pending && status != Status.Success) {
            failureCodes.add(status);
        }
        super.onDimseRSP(as, cmd, data);
    }

    public boolean isSuccess() {
        return failureCodes.isEmpty();
    }
}
