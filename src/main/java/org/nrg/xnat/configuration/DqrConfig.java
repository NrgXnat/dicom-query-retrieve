/*
 * web: org.nrg.xnat.configuration.DicomImportConfig
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.configuration;

import org.nrg.dqr.dicom.strategy.orm.BasicOrmStrategy;
import org.nrg.dqr.dicom.strategy.orm.dcm4chee.Dcm4cheeResultSetLimitStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ComponentScan({"org.nrg.dqr.services", "org.nrg.dqr.daos"})
public class DqrConfig {
    @Bean
    @Primary
    public BasicOrmStrategy dicomOrmStrategy() {
        BasicOrmStrategy strategy = new BasicOrmStrategy();
        strategy.setResultSetLimitStrategy(dicomOrmResultSetLimitStrategy());
        return strategy;
    }

    @Bean
    public Dcm4cheeResultSetLimitStrategy dicomOrmResultSetLimitStrategy() {
        return new Dcm4cheeResultSetLimitStrategy();
    }
}
