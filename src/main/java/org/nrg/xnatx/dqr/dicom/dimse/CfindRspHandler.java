package org.nrg.xnatx.dqr.dicom.dimse;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Status;

import java.io.IOException;
import java.util.function.Consumer;

@Slf4j
@Getter
public class CfindRspHandler extends QrClientRspHandler {
    private final Consumer<Attributes> callback;
    private int numMatches;
    private final int cancelAfter;

    public CfindRspHandler(final int msgId, final int cancelAfter, final Consumer<Attributes> callback) {
        super(msgId);
        this.callback = callback;
        this.cancelAfter = cancelAfter;
    }

    @Override
    public void onDimseRSP(final Association as, final Attributes cmd, final Attributes data) {
        super.onDimseRSP(as, cmd, data);

        if (Status.isPending(cmd.getInt(Tag.Status, -1))) {
            if (null != callback) {
                callback.accept(data);
            }
            ++numMatches;
            if (numMatches >= cancelAfter) {
                try {
                    cancel(as);
                } catch (IOException e) {
                    log.error("Failed to cancel association", e);
                }
            }
        }
    }
}
