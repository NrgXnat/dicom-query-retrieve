/*
 * dicom-query-retrieve: org.nrg.dqr.configuration.PacsAvailabilityServiceTestsConfig
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.dqr.configuration;

import org.nrg.framework.orm.hibernate.HibernateEntityPackageList;
import org.nrg.framework.test.OrmTestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@TestPropertySource(locations = "classpath:/test.properties")
@Import(OrmTestConfiguration.class)
public class DqrServiceTestsConfig {
    @Bean
    public HibernateEntityPackageList dqrServiceEntities() {
        return new HibernateEntityPackageList("org.nrg.dqr.domain.entities");
    }
}
