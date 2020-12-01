/*
 * dicom-query-retrieve: org.nrg.dqr.configuration.PacsAvailabilityServiceTestsConfig
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.dqr.configuration;

import org.nrg.dqr.daos.PacsDAO;
import org.nrg.dqr.services.HibernatePacsEntityService;
import org.nrg.dqr.services.PacsAvailabilityEntityService;
import org.nrg.dqr.services.PacsEntityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(PacsAvailabilityServiceTestsConfig.class)
public class PacsEntityServiceTestsConfig {
    @Bean
    public PacsDAO pacsDAO() {
        return new PacsDAO();
    }

    @Bean
    public PacsEntityService pacsEntityService(final PacsAvailabilityEntityService availabilityEntityService) {
        return new HibernatePacsEntityService(availabilityEntityService);
    }
}
