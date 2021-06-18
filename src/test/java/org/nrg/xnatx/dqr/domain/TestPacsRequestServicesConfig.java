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
import org.nrg.xnatx.dqr.dicom.strategy.orm.*;
import org.nrg.xnatx.dqr.dicom.strategy.orm.dcm4chee.Dcm4cheeResultSetLimitStrategy;
import org.nrg.xnatx.dqr.domain.daos.ExecutedPacsRequestDAO;
import org.nrg.xnatx.dqr.domain.daos.PacsDAO;
import org.nrg.xnatx.dqr.domain.daos.QueuedPacsRequestDAO;
import org.nrg.xnatx.dqr.services.ExecutedPacsRequestService;
import org.nrg.xnatx.dqr.services.PacsService;
import org.nrg.xnatx.dqr.services.QueuedPacsRequestService;
import org.nrg.xnatx.dqr.services.impl.hibernate.HibernateExecutedPacsRequestService;
import org.nrg.xnatx.dqr.services.impl.hibernate.HibernatePacsService;
import org.nrg.xnatx.dqr.services.impl.hibernate.HibernateQueuedPacsRequestService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@Import(OrmTestConfiguration.class)
@EnableTransactionManagement
@TestPropertySource(locations = "classpath:/test.properties")
public class TestPacsRequestServicesConfig {
    @Bean
    public HibernateEntityPackageList dqrEntities() {
        return new HibernateEntityPackageList("org.nrg.xnatx.dqr.domain.entities");
    }

    @Bean
    public PacsDAO pacsDAO() {
        return new PacsDAO();
    }

    @Bean
    public QueuedPacsRequestDAO queuedPacsRequestDAO() {
        return new QueuedPacsRequestDAO();
    }

    @Bean
    public ExecutedPacsRequestDAO executedPacsRequestDAO() {
        return new ExecutedPacsRequestDAO();
    }

    @Bean
    public PatientNameStrategy patientNameStrategy() {
        return new BasicPatientNameStrategy();
    }

    @Bean
    public ResultSetLimitStrategy resultSetLimitStrategy() {
        return new Dcm4cheeResultSetLimitStrategy();
    }

    @Bean
    public OrmStrategy dicomOrmStrategy() {
        return new BasicOrmStrategy(patientNameStrategy(), resultSetLimitStrategy());
    }

    @Bean
    public PacsService pacsService() {
        return new HibernatePacsService();
    }

    @Bean
    public QueuedPacsRequestService queuedPacsRequestService(final NamedParameterJdbcTemplate template) {
        return new HibernateQueuedPacsRequestService(template);
    }

    @Bean
    public ExecutedPacsRequestService executedPacsRequestService(final NamedParameterJdbcTemplate template) {
        return new HibernateExecutedPacsRequestService(template);
    }
}
