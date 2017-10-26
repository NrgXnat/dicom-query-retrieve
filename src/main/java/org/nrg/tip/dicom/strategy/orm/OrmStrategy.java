/*
 * org.nrg.tip.dicom.strategy.orm.OrmStrategy
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.tip.dicom.strategy.orm;

public interface OrmStrategy extends Comparable {

    String getName();
    String getDescription();
    PatientNameStrategy getPatientNameStrategy();
    void setPatientNameStrategy(PatientNameStrategy patientNameStrategy);
    ResultSetLimitStrategy getResultSetLimitStrategy();
    void setResultSetLimitStrategy(ResultSetLimitStrategy resultSetLimitStrategy);
}
