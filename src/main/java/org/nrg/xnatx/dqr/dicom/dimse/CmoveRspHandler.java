package org.nrg.xnatx.dqr.dicom.dimse;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Status;

@Slf4j
@Getter
public class CmoveRspHandler extends QrClientRspHandler {
    private int completed;
    private int warning;
    private int failed;

    public CmoveRspHandler(int msgId) {
        super(msgId);
    }

    @Override
    public void onDimseRSP(final Association as, final Attributes cmd, final Attributes data) {
        super.onDimseRSP(as, cmd, data);
        if (!Status.isPending(cmd.getInt(Tag.Status, -1))) {
            completed += cmd.getInt(Tag.NumberOfCompletedSuboperations, 0);
            warning += cmd.getInt(Tag.NumberOfWarningSuboperations, 0);
            failed += cmd.getInt(Tag.NumberOfFailedSuboperations, 0);
        }
    }
}

