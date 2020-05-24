/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.TestProjectIrbInfoEntities
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain;

import org.nrg.framework.orm.hibernate.HibernateEntityPackageList;
import org.nrg.framework.test.OrmTestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@Import(DqrOrmTestConfiguration.class)
@ComponentScan({"org.nrg.xnatx.dqr.domain.daos", "org.nrg.xnatx.dqr.services.impl.hibernate"})
public class TestProjectIrbInfoEntitiesConfig {
    @Bean
    public HibernateEntityPackageList dqrEntities() {
        return new HibernateEntityPackageList("org.nrg.xnatx.dqr.domain.entities");
    }
}