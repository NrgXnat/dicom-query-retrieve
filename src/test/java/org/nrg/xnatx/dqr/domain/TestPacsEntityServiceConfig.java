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
import org.nrg.xnatx.dqr.domain.daos.PacsDAO;
import org.nrg.xnatx.dqr.services.PacsEntityService;
import org.nrg.xnatx.dqr.services.impl.hibernate.HibernatePacsEntityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@Configuration
@Import(OrmTestConfiguration.class)
@TestPropertySource(locations = "classpath:/test.properties")
public class TestPacsEntityServiceConfig {
    @Bean
    public HibernateEntityPackageList dqrEntities() {
        return new HibernateEntityPackageList("org.nrg.xnatx.dqr.domain.entities");
    }

    @Bean
    public PacsDAO pacsDAO() {
        return new PacsDAO();
    }

    @Bean
    public PacsEntityService pacsEntityService() {
        return new HibernatePacsEntityService();
    }
}
