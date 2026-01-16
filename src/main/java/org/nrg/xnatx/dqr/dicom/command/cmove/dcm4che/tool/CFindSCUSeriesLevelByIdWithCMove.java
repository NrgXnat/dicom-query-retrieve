/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cmove.dcm4che.tool.CFindSCUSeriesLevelByIdWithCMove
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2024, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cmove.dcm4che.tool;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.cfind.SearchCriteriaTooVagueException;
import org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool.CFindSCUSeriesLevel;
import org.nrg.xnatx.dqr.dicom.command.cmove.CMoveFailureException;
import org.nrg.xnatx.dqr.dicom.dimse.QrClient;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

import java.util.List;

/**
 * C-FIND at SERIES level with C-MOVE on results.
 * Uses QrClient for C-MOVE operations via dcm4che3.
 */
@Slf4j
public class CFindSCUSeriesLevelByIdWithCMove extends CFindSCUSeriesLevel {

    private final DqrPreferences preferences;

    public CFindSCUSeriesLevelByIdWithCMove(final DqrPreferences preferences,
                                             final DicomConnectionProperties dicomConnectionProperties,
                                             final CEchoSCU cechoSCU,
                                             final OrmStrategy ormStrategy) {
        super(preferences, dicomConnectionProperties, cechoSCU, ormStrategy);
        this.preferences = preferences;
    }

    @Override
    protected void validatePacsSearchCriteria(final PacsSearchCriteria searchCriteria)
            throws SearchCriteriaTooVagueException {
        if (StringUtils.isBlank(searchCriteria.getSeriesInstanceUid())) {
            throw new SearchCriteriaTooVagueException();
        }
    }

    @Override
    protected boolean cMoveRequestedOnResults() {
        return true;
    }

    @Override
    protected void performCMoveOnResults(final PacsSearchCriteria searchCriteria, final List<Attributes> dicomResults) {
        final DicomConnectionProperties connProps = getConnectionProperties();
        final String localAe = StringUtils.defaultIfBlank(preferences.getDqrCallingAe(), connProps.getLocalAeTitle());
        final String destination = connProps.getLocalAeTitle();

        log.debug("Performing C-MOVE for {} results to destination {}", dicomResults.size(), destination);

        for (final Attributes result : dicomResults) {
            final String studyInstanceUid = result.getString(Tag.StudyInstanceUID);
            final String seriesInstanceUid = result.getString(Tag.SeriesInstanceUID);

            if (StringUtils.isBlank(seriesInstanceUid)) {
                log.warn("Skipping C-MOVE for result with missing SeriesInstanceUID");
                continue;
            }

            log.debug("C-MOVE for SeriesInstanceUID: {}", seriesInstanceUid);

            // Build the C-MOVE query attributes
            final Attributes moveKeys = new Attributes();
            moveKeys.setString(Tag.QueryRetrieveLevel, VR.CS, "SERIES");
            if (StringUtils.isNotBlank(studyInstanceUid)) {
                moveKeys.setString(Tag.StudyInstanceUID, VR.UI, studyInstanceUid);
            }
            moveKeys.setString(Tag.SeriesInstanceUID, VR.UI, seriesInstanceUid);

            try (final QrClient qrClient = QrClient.builder()
                    .localAe(localAe)
                    .remoteHost(connProps.getRemoteHost())
                    .remotePort(connProps.getRemotePort())
                    .remoteAe(connProps.getRemoteAeTitle())
                    .destination(destination)
                    .build()) {

                qrClient.move(moveKeys);
                log.debug("C-MOVE completed for SeriesInstanceUID: {}", seriesInstanceUid);

            } catch (Exception e) {
                log.error("C-MOVE failed for SeriesInstanceUID: {}", seriesInstanceUid, e);
                throw new CMoveFailureException("C-MOVE failed for series " + seriesInstanceUid, e);
            }
        }
    }
}
