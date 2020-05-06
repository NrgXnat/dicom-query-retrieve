/*
 * ChoosePacsSeries
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.apache.turbine.modules.actions;

import java.util.Date;
import javax.jms.Destination;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.messaging.PacsScanImportRequest;
import org.nrg.xnatx.dqr.messaging.PacsStudyImportRequest;
import org.springframework.jms.core.JmsTemplate;

@SuppressWarnings("unused")
public class ChoosePacsSeries extends DqrSecureAction {
    @Override
    public void doPerform(final RunData data, final Context context) {
        final String[] selectedSeriesInstanceUids = (String[]) TurbineUtils.GetPassedObjects("selectedSeries", data);
        if (null == selectedSeriesInstanceUids) {
            context.put("numberOfProcessedSeries", 0);
        } else {
            final UserI                  user                   = XDAT.getUserDetails();
            final PacsStudyImportRequest pacsStudyImportRequest = buildPacsStudyImportRequest(user, data, selectedSeriesInstanceUids);
            sendPacsStudyImportRequest(pacsStudyImportRequest);
            context.put("numberOfProcessedSeries", selectedSeriesInstanceUids.length);
            context.put("user", user);
        }
        data.setScreenTemplate("PacsSessionFinder3.vm");
    }

    private PacsStudyImportRequest buildPacsStudyImportRequest(final UserI user, final RunData data, final String[] selectedSeriesInstanceUids) {
        final Study study = getStudyFromSession(data);

        final PacsStudyImportRequest.PacsStudyImportRequestBuilder builder = PacsStudyImportRequest.builder();
        builder.pacs(getPacsFromSession(data)).study(study).dateRequested(new Date()).requestingUser(user.getUsername());
        for (final String seriesInstanceUid : selectedSeriesInstanceUids) {
            // the client sends the UIDs with underscore separators so they'll be better HTML identifiers
            // translate back to the actual UID
            builder.scan(PacsScanImportRequest.builder().study(study).series(Series.builder().seriesInstanceUid(seriesInstanceUid.replace("_", ".")).build()).build());
        }
        return builder.build();
    }

    private void sendPacsStudyImportRequest(final PacsStudyImportRequest pacsStudyImportRequest) {
        getJmsTemplate().convertAndSend(getDestination(), pacsStudyImportRequest);
    }

    private Destination getDestination() {
        if (_destination == null) {
            _destination = XDAT.getContextService().getBean("pacsStudyImportRequest", Destination.class);
        }
        return _destination;
    }

    private JmsTemplate getJmsTemplate() {
        if (_jmsTemplate == null) {
            _jmsTemplate = XDAT.getContextService().getBean(JmsTemplate.class);
        }
        return _jmsTemplate;
    }

    private static Destination _destination;
    private static JmsTemplate _jmsTemplate;
}
