/*
 * dicom-query-retrieve: org.nrg.dqr.configuration.PacsAvailabilityServiceTestsConfig
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.dqr.configuration;

import org.nrg.dqr.daos.PacsAvailabilityDAO;
import org.nrg.dqr.services.HibernatePacsAvailabilityEntityService;
import org.nrg.dqr.services.PacsAvailabilityEntityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(DqrServiceTestsConfig.class)
public class PacsAvailabilityServiceTestsConfig {
    @Bean
    public PacsAvailabilityDAO pacsAvailabilityDAO() {
        return new PacsAvailabilityDAO();
    }

    @Bean
    public PacsAvailabilityEntityService pacsAvailabilityEntityService() {
        return new HibernatePacsAvailabilityEntityService();
    }
}
