/*
 * ChoosePacsSeries
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.apache.turbine.app.tip_xnat.modules.actions;

import java.util.Date;

import javax.jms.Destination;

import org.apache.commons.lang.StringUtils;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.tip.domain.Series;
import org.nrg.tip.messaging.PacsSeriesImportRequest;
import org.nrg.tip.messaging.PacsStudyImportRequest;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.restlet.extensions.PacsNotFoundException;
import org.springframework.jms.core.JmsTemplate;

public class ChoosePacsSeries extends TipSecureAction {

    @Override
    public void doPerform(final RunData data, final Context context) throws PacsNotFoundException {

        final String[] selectedSeriesInstanceUids = (String[]) TurbineUtils.GetPassedObjects("selectedSeries", data);
        if (null == selectedSeriesInstanceUids) {
            context.put("numberOfProcessedSeries", 0);
        } else {
            final PacsStudyImportRequest pacsStudyImportRequest = buildPacsStudyImportRequest(data,
                    selectedSeriesInstanceUids);
            sendPacsStudyImportRequest(pacsStudyImportRequest);
            context.put("numberOfProcessedSeries", selectedSeriesInstanceUids.length);
            context.put("user", TurbineUtils.getUser(data));
            context.put("StringUtils", new StringUtils());
        }
        data.setScreenTemplate("PacsSessionFinder3.vm");
    }

    private PacsStudyImportRequest buildPacsStudyImportRequest(final RunData data,
                                                               final String[] selectedSeriesInstanceUids) {
        final PacsStudyImportRequest pacsStudyImportRequest = new PacsStudyImportRequest();
        pacsStudyImportRequest.setPacs(getPacsFromSession(data));
        pacsStudyImportRequest.setStudy(getStudyFromSession(data));
        pacsStudyImportRequest.setDateRequested(new Date());
        pacsStudyImportRequest.setRequestingUser(TurbineUtils.getUser(data));
        for (final String seriesInstanceUid : selectedSeriesInstanceUids) {
            // the client sends the UIDs with underscore separators so they'll be better HTML identifiers
            // translate back to the actual UID
            final PacsSeriesImportRequest pacsSeriesImportRequest = new PacsSeriesImportRequest();
            pacsSeriesImportRequest.setStudy(pacsStudyImportRequest.getStudy());
            pacsSeriesImportRequest.setSeries(new Series(seriesInstanceUid.replace("_", ".")));
            pacsStudyImportRequest.addSeries(pacsSeriesImportRequest);
        }
        return pacsStudyImportRequest;
    }

    private void sendPacsStudyImportRequest(final PacsStudyImportRequest pacsStudyImportRequest) {
        final Destination destination = XDAT.getContextService().getBean("pacsStudyImportRequest", Destination.class);
        final JmsTemplate jmsTemplate = XDAT.getContextService().getBean(JmsTemplate.class);
        jmsTemplate.convertAndSend(destination, pacsStudyImportRequest);
    }
}
