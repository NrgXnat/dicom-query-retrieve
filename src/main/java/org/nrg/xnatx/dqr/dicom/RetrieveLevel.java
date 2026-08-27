/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.RetrieveLevel
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2025, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

/**
 * The DICOM query/retrieve level at which data is retrieved from a PACS.
 * <p>
 * This is the level a site or a request asks for, which is a narrower thing than the
 * {@link org.nrg.xnatx.dqr.dicom.command.dcm4che3.QueryRetrieveLevel} used to build DIMSE messages:
 * only whole studies and individual series can be requested, and the choice between them is a
 * configuration decision rather than a protocol detail.
 * <p>
 * {@link #SERIES} is the default because it's how the plugin has always retrieved. {@link #STUDY}
 * suits a PACS that only supports study-level retrieves, or one where opening an association per
 * series is an unwelcome load.
 */
public enum RetrieveLevel {
    /**
     * Retrieve an entire study in a single operation. The series that make up the study can't be
     * filtered, so this applies only when the whole study is wanted.
     */
    STUDY,

    /**
     * Retrieve each series individually, which allows a subset of a study to be requested.
     */
    SERIES;

    /**
     * The level used when neither a request nor a PACS specifies one.
     */
    public static final RetrieveLevel DEFAULT = SERIES;

    /**
     * Parses a submitted value, ignoring case and surrounding whitespace. A blank value means "not
     * specified" and yields null rather than a default, so that callers can tell the difference
     * between a level that was chosen and one that was omitted.
     *
     * @param value The value to parse.
     *
     * @return The corresponding level, or null if the value was blank.
     *
     * @throws IllegalArgumentException When the value isn't a recognized level.
     */
    @JsonCreator
    @Nullable
    public static RetrieveLevel forValue(@Nullable final String value) {
        return StringUtils.isBlank(value) ? null : valueOf(StringUtils.upperCase(StringUtils.trim(value)));
    }

    /**
     * Resolves the level to use for a request. A level specified on the request wins over the one
     * configured for the PACS, which in turn wins over {@link #DEFAULT}. Every path that needs to
     * know the level for a request should call this so that the queueing and execution paths can't
     * arrive at different answers.
     *
     * @param requested  The level asked for by the request, or null if it didn't ask for one.
     * @param pacsDefault The level configured for the PACS, or null if it isn't configured.
     *
     * @return The level to use.
     */
    public static RetrieveLevel resolve(@Nullable final RetrieveLevel requested, @Nullable final RetrieveLevel pacsDefault) {
        if (requested != null) {
            return requested;
        }
        return pacsDefault != null ? pacsDefault : DEFAULT;
    }
}
