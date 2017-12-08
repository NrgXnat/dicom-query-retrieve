/*
 * web: org.nrg.xnat.configuration.DicomImportConfig
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.configuration;

import org.nrg.framework.orm.hibernate.HibernateEntityPackageList;
import org.nrg.dqr.dicom.strategy.orm.BasicOrmStrategy;
import org.nrg.dqr.dicom.strategy.orm.dcm4chee.Dcm4cheeResultSetLimitStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;

@Configuration
@ComponentScan({"org.nrg.dqr.services", "org.nrg.dqr.daos"})
public class DqrConfig {
    @Bean
    @Primary
    public BasicOrmStrategy dicomOrmStrategy() {
        BasicOrmStrategy strat = new BasicOrmStrategy();
        strat.setResultSetLimitStrategy(dicomOrmResultSetLimitStrategy());
        return strat;
    }

    @Bean
    public Dcm4cheeResultSetLimitStrategy dicomOrmResultSetLimitStrategy() {
        return new Dcm4cheeResultSetLimitStrategy();
    }

    @Bean
    public HibernateEntityPackageList dqrEntityPackages() {
        HibernateEntityPackageList list = new HibernateEntityPackageList();
        list.setItems(Arrays.asList("org.nrg.dqr.domain.entities"));
        return list;
    }

}
