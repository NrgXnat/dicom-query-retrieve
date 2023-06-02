/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool.CFindSCUSeriesLevel
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool;

import org.apache.commons.lang3.StringUtils;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.tool.dcmqr.DcmQR.QueryRetrieveLevel;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.dto.StudyDateRangeLimitResults;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.services.SeriesRetrievalStatusService;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class CFindSCUSeriesLevel extends CFindSCUSpecificLevel<Series> {
    public CFindSCUSeriesLevel(final DqrPreferences preferences,
                               final DicomConnectionProperties dicomConnectionProperties,
                               final CEchoSCU cechoSCU,
                               final OrmStrategy ormStrategy,
                               final SeriesRetrievalStatusService seriesRetrievalStatusService) {
        super(preferences, dicomConnectionProperties, cechoSCU, ormStrategy, seriesRetrievalStatusService);
    }

    @Override
    protected List<Integer> getReturnTagPaths() {
        return RETURN_TAG_PATHS;
    }

    @Override
    protected QueryRetrieveLevel getQueryLevel() {
        return QueryRetrieveLevel.SERIES;
    }

    @Override
    protected Series mapDicomObjectToDomainObject(final DicomObject dicomObject) {
        final Series.SeriesBuilder builder = Series.builder();
        builder.study(new Study(StringUtils.trim(dicomObject.getString(Tag.StudyInstanceUID))))
               .seriesInstanceUid(StringUtils.trim(dicomObject.getString(Tag.SeriesInstanceUID)))
               .modality(StringUtils.trim(dicomObject.getString(Tag.Modality)))
               .seriesDescription(StringUtils.trim(dicomObject.getString(Tag.SeriesDescription)))
               .studyDate(StringUtils.trim(dicomObject.getString(Tag.StudyDate)))
               .studyId(StringUtils.trim(dicomObject.getString(Tag.StudyID)))
               .accessionNumber(StringUtils.trim(dicomObject.getString(Tag.AccessionNumber)))
               .patientId(StringUtils.trim(dicomObject.getString(Tag.PatientID)))
               .patientName(StringUtils.trim(dicomObject.getString(Tag.PatientName)));
        if (!StringUtils.isBlank(dicomObject.getString(Tag.SeriesNumber))) {
            builder.seriesNumber(dicomObject.getInt(Tag.SeriesNumber));
        }
        return builder.build();
    }

    @Override
    protected PacsSearchResults<Series> wrapResults(final Collection<Series> results, final boolean hasLimitedResults, final StudyDateRangeLimitResults studyDateRangeLimitResults) {
        return PacsSearchResults.<Series>builder().results(results).hasLimitedResultSetSize(hasLimitedResults).studyDateRangeLimitResults(studyDateRangeLimitResults).build();
    }

    private static final List<Integer> RETURN_TAG_PATHS = Arrays.asList(Tag.SeriesDescription, Tag.StudyDate, Tag.StudyID, Tag.AccessionNumber, Tag.PatientID, Tag.PatientName);
}
