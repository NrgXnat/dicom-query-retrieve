/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.strategy.orm;

public interface OrmStrategy extends Comparable<OrmStrategy> {
    String getName();

    String getDescription();

    PatientNameStrategy getPatientNameStrategy();

    ResultSetLimitStrategy getResultSetLimitStrategy();
}
