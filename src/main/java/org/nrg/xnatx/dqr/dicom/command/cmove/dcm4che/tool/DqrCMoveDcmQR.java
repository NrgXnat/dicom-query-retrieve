/*
 * DqrCMoveDcmQR
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.dicom.command.cmove.dcm4che.tool;

import java.io.IOException;
import java.util.List;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.CommandUtils;
import org.dcm4che2.tool.dcmqr.DcmQR;
import org.nrg.xnatx.dqr.dicom.command.cmove.CMoveFailureException;

public class DqrCMoveDcmQR extends DcmQR {

    private boolean success;

    private String errorMessage;

    public DqrCMoveDcmQR(final String name) {
        super(name);
    }

    @Override
    protected void onMoveRSP(final Association as, final DicomObject cmd, final DicomObject data) {
        if (!CommandUtils.isPending(cmd)) {
            if (0 == cmd.getInt(Tag.Status)) {
                setSuccess(true);
            } else {
                // we're actually in the dcm4che response listener thread,
                // so don't throw an exception now.
                // record the error, and then main thread will see it once it's notified
                setSuccess(false);
                setErrorMessage(System.getProperty("line.separator") + cmd.toString());
            }
        }
    }

    @Override
    public void move(final List<DicomObject> findResults) throws IOException, InterruptedException {
        super.move(findResults);
        if (!isSuccess()) {
            throw new CMoveFailureException(getErrorMessage());
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
