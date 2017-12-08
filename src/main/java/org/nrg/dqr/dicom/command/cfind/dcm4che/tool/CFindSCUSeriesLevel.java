/*
 * CFindSCUSeriesLevel
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.dicom.command.cfind.dcm4che.tool;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.tool.dcmqr.DcmQR.QueryRetrieveLevel;
import org.nrg.dqr.domain.Study;
import org.nrg.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.dqr.domain.Series;
import org.nrg.dqr.dto.PacsSearchResults;
import org.nrg.dqr.dto.StudyDateRangeLimitResults;

public abstract class CFindSCUSeriesLevel extends CFindSCUSpecificLevel<Series> {

    private final static int[] RETURN_TAG_PATHS = new int[]{
            Tag.SeriesDescription
    };

    public CFindSCUSeriesLevel(final DicomConnectionProperties dicomConnectionProperties, final CEchoSCU cechoSCU,
                               final OrmStrategy ormStrategy) {
        super(dicomConnectionProperties, cechoSCU, ormStrategy);
    }

    @Override
    protected int[] getReturnTagPaths() {
        return RETURN_TAG_PATHS;
    }

    @Override
    protected QueryRetrieveLevel getQueryLevel() {
        return QueryRetrieveLevel.SERIES;
    }

    @Override
    protected Series mapDicomObjectToDomainObject(final DicomObject d) {
        final Series series = new Series();
        series.setStudy(new Study(StringUtils.trim(d.getString(Tag.StudyInstanceUID))));
        series.setSeriesInstanceUid(StringUtils.trim(d.getString(Tag.SeriesInstanceUID)));
        if (!StringUtils.isBlank(d.getString(Tag.SeriesNumber))) {
            series.setSeriesNumber(d.getInt(Tag.SeriesNumber));
        }
        series.setModality(StringUtils.trim(d.getString(Tag.Modality)));
        series.setSeriesDescription(StringUtils.trim(d.getString(Tag.SeriesDescription)));
        return series;
    }

    @Override
    protected PacsSearchResults<String, Series> wrapResults(final Map<String, Series> results,
                                                            final boolean hasLimitedResults, final StudyDateRangeLimitResults studyDateRangeLimitResults) {
        return new PacsSearchResults<String, Series>(results, hasLimitedResults, studyDateRangeLimitResults);
    }
}
