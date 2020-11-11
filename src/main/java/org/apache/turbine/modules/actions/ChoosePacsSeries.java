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

import org.apache.commons.lang.StringUtils;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.dqr.domain.Series;
import org.nrg.dqr.messaging.PacsSeriesImportRequest;
import org.nrg.dqr.messaging.PacsStudyImportRequest;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.Destination;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class ChoosePacsSeries extends DqrSecureAction {
    public ChoosePacsSeries() {
        _template = XDAT.getContextService().getBean(JmsTemplate.class);
        _destination = XDAT.getContextService().getBean("pacsStudyImportRequest", Destination.class);
    }

    @Override
    public void doPerform(final RunData data, final Context context) {
        final String[] selectedSeriesInstanceUids = (String[]) TurbineUtils.GetPassedObjects("selectedSeries", data);
        if (null == selectedSeriesInstanceUids) {
            context.put("numberOfProcessedSeries", 0);
        } else {
            sendPacsStudyImportRequest(buildPacsStudyImportRequest(data, selectedSeriesInstanceUids));
            context.put("numberOfProcessedSeries", selectedSeriesInstanceUids.length);
            context.put("user", getUser());
            //noinspection InstantiationOfUtilityClass
            context.put("StringUtils", new StringUtils());
        }
        data.setScreenTemplate("PacsSessionFinder3.vm");
    }

    private PacsStudyImportRequest buildPacsStudyImportRequest(final RunData data, final String[] selectedSeriesInstanceUids) {
        final PacsStudyImportRequest pacsStudyImportRequest = new PacsStudyImportRequest();
        pacsStudyImportRequest.setPacs(getPacsFromSession(data));
        pacsStudyImportRequest.setStudy(getStudyFromSession(data));
        pacsStudyImportRequest.setDateRequested(new Date());
        pacsStudyImportRequest.setRequestingUser(getUser());
        // the client sends the UIDs with underscore separators so they'll be better HTML identifiers: translate back to the actual UID
        pacsStudyImportRequest.setSeries(Arrays.stream(selectedSeriesInstanceUids).map(seriesInstanceUid -> new Series(seriesInstanceUid.replace("_", "."))).map(series -> PacsSeriesImportRequest.builder().study(pacsStudyImportRequest.getStudy()).series(series).build()).collect(Collectors.toList()));
        return pacsStudyImportRequest;
    }

    private void sendPacsStudyImportRequest(final PacsStudyImportRequest pacsStudyImportRequest) {
        _template.convertAndSend(_destination, pacsStudyImportRequest);
    }

    private final JmsTemplate _template;
    private final Destination _destination;
}
