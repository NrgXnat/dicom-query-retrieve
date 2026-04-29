/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool.CFindSCUSeriesLevel
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2024, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.dcm4che3.QueryRetrieveLevel;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.dto.StudyDateRangeLimitResults;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class CFindSCUSeriesLevel extends CFindSCUSpecificLevel<Series> {

    public CFindSCUSeriesLevel(final DqrPreferences preferences,
                                final DicomConnectionProperties dicomConnectionProperties,
                                final CEchoSCU cechoSCU,
                                final OrmStrategy ormStrategy) {
        super(preferences, dicomConnectionProperties, cechoSCU, ormStrategy);
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
    protected Series mapAttributesToDomainObject(final Attributes attributes) {
        return Series.from(attributes);
    }

    @Override
    protected PacsSearchResults<Series> wrapResults(final Collection<Series> results,
                                                     final boolean hasLimitedResults,
                                                     final StudyDateRangeLimitResults studyDateRangeLimitResults) {
        return PacsSearchResults.<Series>builder()
                .results(results)
                .hasLimitedResultSetSize(hasLimitedResults)
                .studyDateRangeLimitResults(studyDateRangeLimitResults)
                .build();
    }

    private static final List<Integer> RETURN_TAG_PATHS = Arrays.asList(
            Tag.SeriesDescription,
            Tag.StudyDate,
            Tag.StudyID,
            Tag.AccessionNumber,
            Tag.PatientID,
            Tag.PatientName
    );
}
