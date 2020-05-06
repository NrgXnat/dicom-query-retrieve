/*
 * BasicOrmStrategy
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.dicom.strategy.orm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@Accessors(prefix = "_")
@AllArgsConstructor
public class BasicOrmStrategy implements OrmStrategy {
    @SuppressWarnings("unused")
    public BasicOrmStrategy() {
        this(DEFAULT_NAME, DEFAULT_DESCRIPTION, DEFAULT_PATIENT_NAME_STRATEGY, DEFAULT_RESULT_SET_LIMIT_STRATEGY);
    }

    public BasicOrmStrategy(final PatientNameStrategy patientNameStrategy, final ResultSetLimitStrategy resultSetLimitStrategy) {
        this(DEFAULT_NAME, DEFAULT_DESCRIPTION, patientNameStrategy, resultSetLimitStrategy);
    }

    @Override
    public int compareTo(final @NotNull OrmStrategy strategy) {
        return getName().compareTo(strategy.getName());
    }

    private static final String                 DEFAULT_NAME                      = BasicOrmStrategy.class.getName();
    private static final String                 DEFAULT_DESCRIPTION               = BasicOrmStrategy.class.getName();
    private static final PatientNameStrategy    DEFAULT_PATIENT_NAME_STRATEGY     = new BasicPatientNameStrategy();
    private static final ResultSetLimitStrategy DEFAULT_RESULT_SET_LIMIT_STRATEGY = new BasicResultSetLimitStrategy();

    private final String                 _name;
    private final String                 _description;
    private       PatientNameStrategy    _patientNameStrategy;
    private       ResultSetLimitStrategy _resultSetLimitStrategy;
}
