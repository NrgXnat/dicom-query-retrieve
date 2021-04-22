/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.PacsEntityServiceTests
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2021, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.nrg.xnatx.dqr.domain.DqrOrmTestConfiguration;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitJupiterConfig;

@ExtendWith(SpringExtension.class)
@SpringJUnitJupiterConfig(TestPacsEntityService.class)
@ContextConfiguration(classes = DqrOrmTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Slf4j
public class TestPacsEntityService {
    @Autowired
    public TestPacsEntityService(final PacsEntityService service) {
        _service = service;
    }

    @Test
    public void sanityCheck() {
        log.info("This is a sanity check test. It should execute and exit successfully.");
        assertThat(_service).isNotNull();
    }

    @Test
    public void testSimplePacsTransaction() {
        final Pacs pacs1 = _service.create(Pacs.builder().host(PACS_HOST_1).queryRetrievePort(PACS_PORT_1).aeTitle(PACS_AE_1).label(PACS_LABEL_1).defaultQueryRetrievePacs(true).defaultStoragePacs(true).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(true).build());
        assertThat(pacs1).isNotNull()
                         .hasFieldOrPropertyWithValue("host", PACS_HOST_1)
                         .hasFieldOrPropertyWithValue("queryRetrievePort", PACS_PORT_1)
                         .hasFieldOrPropertyWithValue("aeTitle", PACS_AE_1)
                         .hasFieldOrPropertyWithValue("label", PACS_LABEL_1)
                         .hasFieldOrPropertyWithValue("defaultQueryRetrievePacs", true)
                         .hasFieldOrPropertyWithValue("defaultStoragePacs", true)
                         .hasFieldOrPropertyWithValue("ormStrategySpringBeanId", PACS_ORM_STRATEGY_1)
                         .hasFieldOrPropertyWithValue("queryable", true)
                         .hasFieldOrPropertyWithValue("storable", true);
        final Pacs pacs2 = _service.create(Pacs.builder().host(PACS_HOST_2).queryRetrievePort(PACS_PORT_2).aeTitle(PACS_AE_2).label(PACS_LABEL_2).defaultQueryRetrievePacs(false).defaultStoragePacs(false).ormStrategySpringBeanId(PACS_ORM_STRATEGY_2).queryable(true).storable(true).build());
        assertThat(pacs2).isNotNull()
                         .hasFieldOrPropertyWithValue("host", PACS_HOST_2)
                         .hasFieldOrPropertyWithValue("queryRetrievePort", PACS_PORT_2)
                         .hasFieldOrPropertyWithValue("aeTitle", PACS_AE_2)
                         .hasFieldOrPropertyWithValue("label", PACS_LABEL_2)
                         .hasFieldOrPropertyWithValue("defaultQueryRetrievePacs", false)
                         .hasFieldOrPropertyWithValue("defaultStoragePacs", false)
                         .hasFieldOrPropertyWithValue("ormStrategySpringBeanId", PACS_ORM_STRATEGY_2)
                         .hasFieldOrPropertyWithValue("queryable", true)
                         .hasFieldOrPropertyWithValue("storable", true);
    }

    @Test
    public void testNoDuplicatePacs() {
        final Pacs pacs1 = _service.create(Pacs.builder().host(PACS_HOST_1).queryRetrievePort(PACS_PORT_1).aeTitle(PACS_AE_1).label(PACS_LABEL_1).defaultQueryRetrievePacs(true).defaultStoragePacs(true).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(true).build());
        assertThat(pacs1).isNotNull()
                         .hasFieldOrPropertyWithValue("host", PACS_HOST_1)
                         .hasFieldOrPropertyWithValue("queryRetrievePort", PACS_PORT_1)
                         .hasFieldOrPropertyWithValue("aeTitle", PACS_AE_1)
                         .hasFieldOrPropertyWithValue("label", PACS_LABEL_1)
                         .hasFieldOrPropertyWithValue("defaultQueryRetrievePacs", true)
                         .hasFieldOrPropertyWithValue("defaultStoragePacs", true)
                         .hasFieldOrPropertyWithValue("ormStrategySpringBeanId", PACS_ORM_STRATEGY_1)
                         .hasFieldOrPropertyWithValue("queryable", true)
                         .hasFieldOrPropertyWithValue("storable", true);

        // Same attributes across the board should obviously fail.
        Assertions.assertThrows(ConstraintViolationException.class, () -> _service.create(Pacs.builder().host(PACS_HOST_1).queryRetrievePort(PACS_PORT_1).aeTitle(PACS_AE_1).label(PACS_LABEL_1).defaultQueryRetrievePacs(true).defaultStoragePacs(true).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(true).build()));

        // Replicates host, port, and AE title
        Assertions.assertThrows(ConstraintViolationException.class, () -> _service.create(Pacs.builder().host(PACS_HOST_1).queryRetrievePort(PACS_PORT_1).aeTitle(PACS_AE_1).label(PACS_LABEL_2).defaultQueryRetrievePacs(false).defaultStoragePacs(false).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(true).build()));

        // Replicates label
        Assertions.assertThrows(ConstraintViolationException.class, () -> _service.create(Pacs.builder().host(PACS_HOST_2).queryRetrievePort(PACS_PORT_2).aeTitle(PACS_AE_2).label(PACS_LABEL_1).defaultQueryRetrievePacs(false).defaultStoragePacs(false).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(true).build()));

        // Same host and port, different AE and label is valid.
        final Pacs pacs2 = _service.create(Pacs.builder().host(PACS_HOST_1).queryRetrievePort(PACS_PORT_1).aeTitle(PACS_AE_2).label(PACS_LABEL_2).defaultQueryRetrievePacs(false).defaultStoragePacs(false).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(true).build());
        assertThat(pacs2).isNotNull()
                         .hasFieldOrPropertyWithValue("host", PACS_HOST_1)
                         .hasFieldOrPropertyWithValue("queryRetrievePort", PACS_PORT_1)
                         .hasFieldOrPropertyWithValue("aeTitle", PACS_AE_2)
                         .hasFieldOrPropertyWithValue("label", PACS_LABEL_2)
                         .hasFieldOrPropertyWithValue("defaultQueryRetrievePacs", false)
                         .hasFieldOrPropertyWithValue("defaultStoragePacs", false)
                         .hasFieldOrPropertyWithValue("ormStrategySpringBeanId", PACS_ORM_STRATEGY_1)
                         .hasFieldOrPropertyWithValue("queryable", true)
                         .hasFieldOrPropertyWithValue("storable", true);

        // Same host and AE, different port and label is valid.
        final Pacs pacs3 = _service.create(Pacs.builder().host(PACS_HOST_1).queryRetrievePort(PACS_PORT_2).aeTitle(PACS_AE_1).label(PACS_LABEL_3).defaultQueryRetrievePacs(false).defaultStoragePacs(false).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(true).build());
        assertThat(pacs3).isNotNull()
                         .hasFieldOrPropertyWithValue("host", PACS_HOST_1)
                         .hasFieldOrPropertyWithValue("queryRetrievePort", PACS_PORT_2)
                         .hasFieldOrPropertyWithValue("aeTitle", PACS_AE_1)
                         .hasFieldOrPropertyWithValue("label", PACS_LABEL_3)
                         .hasFieldOrPropertyWithValue("defaultQueryRetrievePacs", false)
                         .hasFieldOrPropertyWithValue("defaultStoragePacs", false)
                         .hasFieldOrPropertyWithValue("ormStrategySpringBeanId", PACS_ORM_STRATEGY_1)
                         .hasFieldOrPropertyWithValue("queryable", true)
                         .hasFieldOrPropertyWithValue("storable", true);

        // Same port and AE, different host and label is valid.
        final Pacs pacs4 = _service.create(Pacs.builder().host(PACS_HOST_2).queryRetrievePort(PACS_PORT_1).aeTitle(PACS_AE_1).label(PACS_LABEL_4).defaultQueryRetrievePacs(false).defaultStoragePacs(false).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(true).build());
        assertThat(pacs4).isNotNull()
                         .hasFieldOrPropertyWithValue("host", PACS_HOST_2)
                         .hasFieldOrPropertyWithValue("queryRetrievePort", PACS_PORT_1)
                         .hasFieldOrPropertyWithValue("aeTitle", PACS_AE_1)
                         .hasFieldOrPropertyWithValue("label", PACS_LABEL_4)
                         .hasFieldOrPropertyWithValue("defaultQueryRetrievePacs", false)
                         .hasFieldOrPropertyWithValue("defaultStoragePacs", false)
                         .hasFieldOrPropertyWithValue("ormStrategySpringBeanId", PACS_ORM_STRATEGY_1)
                         .hasFieldOrPropertyWithValue("queryable", true)
                         .hasFieldOrPropertyWithValue("storable", true);
    }

    private static final String PACS_HOST_1         = "pacs1.xnat.org";
    private static final String PACS_HOST_2         = "pacs2.xnat.org";
    private static final int    PACS_PORT_1         = 4242;
    private static final int    PACS_PORT_2         = 8042;
    private static final String PACS_AE_1           = "PACS1";
    private static final String PACS_AE_2           = "PACS2";
    private static final String PACS_LABEL_1        = "Pacs 1";
    private static final String PACS_LABEL_2        = "Pacs 2";
    private static final String PACS_LABEL_3        = "Pacs 3";
    private static final String PACS_LABEL_4        = "Pacs 4";
    private static final String PACS_ORM_STRATEGY_1 = "dicomOrmStrategy1";
    private static final String PACS_ORM_STRATEGY_2 = "dicomOrmStrategy2";

    private final PacsEntityService _service;
}
