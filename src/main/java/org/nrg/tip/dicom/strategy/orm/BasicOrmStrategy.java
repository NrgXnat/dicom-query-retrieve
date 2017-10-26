/*
 * org.nrg.tip.dicom.strategy.orm.BasicOrmStrategy
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

public class BasicOrmStrategy implements OrmStrategy {

    private final String _name;
    private final String _description;
    private PatientNameStrategy _patientNameStrategy;
    private ResultSetLimitStrategy _resultSetLimitStrategy;

    public BasicOrmStrategy() {
        _name = getClass().getName();
        _description = getClass().getName();
        _patientNameStrategy = new BasicPatientNameStrategy();
        _resultSetLimitStrategy = new BasicResultSetLimitStrategy();
    }

    public BasicOrmStrategy(final String name, final String description) {
        _name = name;
        _description = description;
        _patientNameStrategy = new BasicPatientNameStrategy();
        _resultSetLimitStrategy = new BasicResultSetLimitStrategy();
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public String getDescription() {
        return _description;
    }

    @Override
    public PatientNameStrategy getPatientNameStrategy() {
        return _patientNameStrategy;
    }

    @Override
    public void setPatientNameStrategy(PatientNameStrategy patientNameStrategy) {
        this._patientNameStrategy = patientNameStrategy;
    }

    @Override
    public ResultSetLimitStrategy getResultSetLimitStrategy() {
        return _resultSetLimitStrategy;
    }

    @Override
    public void setResultSetLimitStrategy(ResultSetLimitStrategy resultSetLimitStrategy) {
        this._resultSetLimitStrategy = resultSetLimitStrategy;
    }

    @Override
    public int compareTo(final Object o) {
        if (o == null || !(o instanceof OrmStrategy)) {
            return 0;
        }
        OrmStrategy other = (OrmStrategy) o;
        return getName().compareTo(other.getName());
    }
}
