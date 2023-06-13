/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.TestPacsService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2021, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nrg.xnatx.dqr.domain.entities.Pacs.PROP_AE_TITLE;
import static org.nrg.xnatx.dqr.domain.entities.Pacs.PROP_DEFAULT_QUERY_RETRIEVE_PACS;
import static org.nrg.xnatx.dqr.domain.entities.Pacs.PROP_DEFAULT_STORAGE_PACS;
import static org.nrg.xnatx.dqr.domain.entities.Pacs.PROP_HOST;
import static org.nrg.xnatx.dqr.domain.entities.Pacs.PROP_LABEL;
import static org.nrg.xnatx.dqr.domain.entities.Pacs.PROP_ORM_STRATEGY_SPRING_BEAN_ID;
import static org.nrg.xnatx.dqr.domain.entities.Pacs.PROP_QUERYABLE;
import static org.nrg.xnatx.dqr.domain.entities.Pacs.PROP_QUERY_RETRIEVE_PORT;
import static org.nrg.xnatx.dqr.domain.entities.Pacs.PROP_STORABLE;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.nrg.xnatx.dqr.domain.TestPacsServiceConfig;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitJupiterConfig;

@ExtendWith(SpringExtension.class)
@SpringJUnitJupiterConfig(TestPacsService.class)
@ContextConfiguration(classes = TestPacsServiceConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Slf4j
public class TestPacsService {
    @Autowired
    public TestPacsService(final PacsService service) {
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
                         .hasFieldOrPropertyWithValue(PROP_HOST, PACS_HOST_1)
                         .hasFieldOrPropertyWithValue(PROP_QUERY_RETRIEVE_PORT, PACS_PORT_1)
                         .hasFieldOrPropertyWithValue(PROP_AE_TITLE, PACS_AE_1)
                         .hasFieldOrPropertyWithValue(PROP_LABEL, PACS_LABEL_1)
                         .hasFieldOrPropertyWithValue(PROP_DEFAULT_QUERY_RETRIEVE_PACS, true)
                         .hasFieldOrPropertyWithValue(PROP_DEFAULT_STORAGE_PACS, true)
                         .hasFieldOrPropertyWithValue(PROP_ORM_STRATEGY_SPRING_BEAN_ID, PACS_ORM_STRATEGY_1)
                         .hasFieldOrPropertyWithValue(PROP_QUERYABLE, true)
                         .hasFieldOrPropertyWithValue(PROP_STORABLE, true);
        final Pacs pacs2 = _service.create(Pacs.builder().host(PACS_HOST_2).queryRetrievePort(PACS_PORT_2).aeTitle(PACS_AE_2).label(PACS_LABEL_2).defaultQueryRetrievePacs(false).defaultStoragePacs(false).ormStrategySpringBeanId(PACS_ORM_STRATEGY_2).queryable(true).storable(true).build());
        assertThat(pacs2).isNotNull()
                         .hasFieldOrPropertyWithValue(PROP_HOST, PACS_HOST_2)
                         .hasFieldOrPropertyWithValue(PROP_QUERY_RETRIEVE_PORT, PACS_PORT_2)
                         .hasFieldOrPropertyWithValue(PROP_AE_TITLE, PACS_AE_2)
                         .hasFieldOrPropertyWithValue(PROP_LABEL, PACS_LABEL_2)
                         .hasFieldOrPropertyWithValue(PROP_DEFAULT_QUERY_RETRIEVE_PACS, false)
                         .hasFieldOrPropertyWithValue(PROP_DEFAULT_STORAGE_PACS, false)
                         .hasFieldOrPropertyWithValue(PROP_ORM_STRATEGY_SPRING_BEAN_ID, PACS_ORM_STRATEGY_2)
                         .hasFieldOrPropertyWithValue(PROP_QUERYABLE, true)
                         .hasFieldOrPropertyWithValue(PROP_STORABLE, true);
    }

    @Test
    public void testNoDuplicatePacs() {
        final Pacs pacs1 = _service.create(Pacs.builder().host(PACS_HOST_1).queryRetrievePort(PACS_PORT_1).aeTitle(PACS_AE_1).label(PACS_LABEL_1).defaultQueryRetrievePacs(true).defaultStoragePacs(true).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(true).build());
        assertThat(pacs1).isNotNull()
                         .hasFieldOrPropertyWithValue(PROP_HOST, PACS_HOST_1)
                         .hasFieldOrPropertyWithValue(PROP_QUERY_RETRIEVE_PORT, PACS_PORT_1)
                         .hasFieldOrPropertyWithValue(PROP_AE_TITLE, PACS_AE_1)
                         .hasFieldOrPropertyWithValue(PROP_LABEL, PACS_LABEL_1)
                         .hasFieldOrPropertyWithValue(PROP_DEFAULT_QUERY_RETRIEVE_PACS, true)
                         .hasFieldOrPropertyWithValue(PROP_DEFAULT_STORAGE_PACS, true)
                         .hasFieldOrPropertyWithValue(PROP_ORM_STRATEGY_SPRING_BEAN_ID, PACS_ORM_STRATEGY_1)
                         .hasFieldOrPropertyWithValue(PROP_QUERYABLE, true)
                         .hasFieldOrPropertyWithValue(PROP_STORABLE, true);

        // Same attributes across the board should obviously fail.
        Assertions.assertThrows(ConstraintViolationException.class, () -> _service.create(Pacs.builder().host(PACS_HOST_1).queryRetrievePort(PACS_PORT_1).aeTitle(PACS_AE_1).label(PACS_LABEL_1).defaultQueryRetrievePacs(true).defaultStoragePacs(true).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(true).build()));

        // Replicates host, port, and AE title
        Assertions.assertThrows(ConstraintViolationException.class, () -> _service.create(Pacs.builder().host(PACS_HOST_1).queryRetrievePort(PACS_PORT_1).aeTitle(PACS_AE_1).label(PACS_LABEL_2).defaultQueryRetrievePacs(false).defaultStoragePacs(false).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(true).build()));

        // Replicates label
        Assertions.assertThrows(ConstraintViolationException.class, () -> _service.create(Pacs.builder().host(PACS_HOST_2).queryRetrievePort(PACS_PORT_2).aeTitle(PACS_AE_2).label(PACS_LABEL_1).defaultQueryRetrievePacs(false).defaultStoragePacs(false).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(true).build()));

        // Same host and port, different AE and label is valid.
        final Pacs pacs2 = _service.create(Pacs.builder().host(PACS_HOST_1).queryRetrievePort(PACS_PORT_1).aeTitle(PACS_AE_2).label(PACS_LABEL_2).defaultQueryRetrievePacs(false).defaultStoragePacs(false).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(true).build());
        assertThat(pacs2).isNotNull()
                         .hasFieldOrPropertyWithValue(PROP_HOST, PACS_HOST_1)
                         .hasFieldOrPropertyWithValue(PROP_QUERY_RETRIEVE_PORT, PACS_PORT_1)
                         .hasFieldOrPropertyWithValue(PROP_AE_TITLE, PACS_AE_2)
                         .hasFieldOrPropertyWithValue(PROP_LABEL, PACS_LABEL_2)
                         .hasFieldOrPropertyWithValue(PROP_DEFAULT_QUERY_RETRIEVE_PACS, false)
                         .hasFieldOrPropertyWithValue(PROP_DEFAULT_STORAGE_PACS, false)
                         .hasFieldOrPropertyWithValue(PROP_ORM_STRATEGY_SPRING_BEAN_ID, PACS_ORM_STRATEGY_1)
                         .hasFieldOrPropertyWithValue(PROP_QUERYABLE, true)
                         .hasFieldOrPropertyWithValue(PROP_STORABLE, true);

        // Same host and AE, different port and label is valid.
        final Pacs pacs3 = _service.create(Pacs.builder().host(PACS_HOST_1).queryRetrievePort(PACS_PORT_2).aeTitle(PACS_AE_1).label(PACS_LABEL_3).defaultQueryRetrievePacs(false).defaultStoragePacs(false).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(true).build());
        assertThat(pacs3).isNotNull()
                         .hasFieldOrPropertyWithValue(PROP_HOST, PACS_HOST_1)
                         .hasFieldOrPropertyWithValue(PROP_QUERY_RETRIEVE_PORT, PACS_PORT_2)
                         .hasFieldOrPropertyWithValue(PROP_AE_TITLE, PACS_AE_1)
                         .hasFieldOrPropertyWithValue(PROP_LABEL, PACS_LABEL_3)
                         .hasFieldOrPropertyWithValue(PROP_DEFAULT_QUERY_RETRIEVE_PACS, false)
                         .hasFieldOrPropertyWithValue(PROP_DEFAULT_STORAGE_PACS, false)
                         .hasFieldOrPropertyWithValue(PROP_ORM_STRATEGY_SPRING_BEAN_ID, PACS_ORM_STRATEGY_1)
                         .hasFieldOrPropertyWithValue(PROP_QUERYABLE, true)
                         .hasFieldOrPropertyWithValue(PROP_STORABLE, true);

        // Same port and AE, different host and label is valid.
        final Pacs pacs4 = _service.create(Pacs.builder().host(PACS_HOST_2).queryRetrievePort(PACS_PORT_1).aeTitle(PACS_AE_1).label(PACS_LABEL_4).defaultQueryRetrievePacs(false).defaultStoragePacs(false).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(true).build());
        assertThat(pacs4).isNotNull()
                         .hasFieldOrPropertyWithValue(PROP_HOST, PACS_HOST_2)
                         .hasFieldOrPropertyWithValue(PROP_QUERY_RETRIEVE_PORT, PACS_PORT_1)
                         .hasFieldOrPropertyWithValue(PROP_AE_TITLE, PACS_AE_1)
                         .hasFieldOrPropertyWithValue(PROP_LABEL, PACS_LABEL_4)
                         .hasFieldOrPropertyWithValue(PROP_DEFAULT_QUERY_RETRIEVE_PACS, false)
                         .hasFieldOrPropertyWithValue(PROP_DEFAULT_STORAGE_PACS, false)
                         .hasFieldOrPropertyWithValue(PROP_ORM_STRATEGY_SPRING_BEAN_ID, PACS_ORM_STRATEGY_1)
                         .hasFieldOrPropertyWithValue(PROP_QUERYABLE, true)
                         .hasFieldOrPropertyWithValue(PROP_STORABLE, true);
    }

    @Test
    public void testDefaultQueryRetrieveAndStoragePacsTransactions() {
        final Pacs pacs1 = _service.create(Pacs.builder().host(PACS_HOST_1).queryRetrievePort(PACS_PORT_1).aeTitle(PACS_AE_1).label(PACS_LABEL_1).defaultQueryRetrievePacs(true).defaultStoragePacs(false).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(true).build());
        assertThat(pacs1).isNotNull()
                         .hasFieldOrPropertyWithValue(PROP_HOST, PACS_HOST_1)
                         .hasFieldOrPropertyWithValue(PROP_QUERY_RETRIEVE_PORT, PACS_PORT_1)
                         .hasFieldOrPropertyWithValue(PROP_AE_TITLE, PACS_AE_1)
                         .hasFieldOrPropertyWithValue(PROP_LABEL, PACS_LABEL_1)
                         .hasFieldOrPropertyWithValue(PROP_DEFAULT_QUERY_RETRIEVE_PACS, true)
                         .hasFieldOrPropertyWithValue(PROP_DEFAULT_STORAGE_PACS, false)
                         .hasFieldOrPropertyWithValue(PROP_ORM_STRATEGY_SPRING_BEAN_ID, PACS_ORM_STRATEGY_1)
                         .hasFieldOrPropertyWithValue(PROP_QUERYABLE, true)
                         .hasFieldOrPropertyWithValue(PROP_STORABLE, true);
        final Pacs pacs2 = _service.create(Pacs.builder().host(PACS_HOST_2).queryRetrievePort(PACS_PORT_2).aeTitle(PACS_AE_2).label(PACS_LABEL_2).defaultQueryRetrievePacs(false).defaultStoragePacs(true).ormStrategySpringBeanId(PACS_ORM_STRATEGY_2).queryable(true).storable(true).build());
        assertThat(pacs2).isNotNull()
                         .hasFieldOrPropertyWithValue(PROP_HOST, PACS_HOST_2)
                         .hasFieldOrPropertyWithValue(PROP_QUERY_RETRIEVE_PORT, PACS_PORT_2)
                         .hasFieldOrPropertyWithValue(PROP_AE_TITLE, PACS_AE_2)
                         .hasFieldOrPropertyWithValue(PROP_LABEL, PACS_LABEL_2)
                         .hasFieldOrPropertyWithValue(PROP_DEFAULT_QUERY_RETRIEVE_PACS, false)
                         .hasFieldOrPropertyWithValue(PROP_DEFAULT_STORAGE_PACS, true)
                         .hasFieldOrPropertyWithValue(PROP_ORM_STRATEGY_SPRING_BEAN_ID, PACS_ORM_STRATEGY_2)
                         .hasFieldOrPropertyWithValue(PROP_QUERYABLE, true)
                         .hasFieldOrPropertyWithValue(PROP_STORABLE, true);

        final Pacs defaultQueryRetrievePacs1 = _service.findDefaultQueryRetrievePacs().orElse(null);
        final Pacs defaultStoragePacs1       = _service.findDefaultStoragePacs().orElse(null);
        assertThat(defaultQueryRetrievePacs1).isNotNull().hasFieldOrPropertyWithValue("id", pacs1.getId()).hasFieldOrPropertyWithValue(PROP_DEFAULT_QUERY_RETRIEVE_PACS, true);
        assertThat(defaultStoragePacs1).isNotNull().hasFieldOrPropertyWithValue("id", pacs2.getId()).hasFieldOrPropertyWithValue(PROP_DEFAULT_STORAGE_PACS, true);

        // Different PACS, doesn't step on default Q/R or storable attributes.
        final Pacs pacs3 = _service.create(Pacs.builder().host(PACS_HOST_1).queryRetrievePort(PACS_PORT_2).aeTitle(PACS_AE_1).label(PACS_LABEL_3).defaultQueryRetrievePacs(false).defaultStoragePacs(false).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(true).build());
        assertThat(pacs3).isNotNull()
                         .hasFieldOrPropertyWithValue(PROP_HOST, PACS_HOST_1)
                         .hasFieldOrPropertyWithValue(PROP_QUERY_RETRIEVE_PORT, PACS_PORT_2)
                         .hasFieldOrPropertyWithValue(PROP_AE_TITLE, PACS_AE_1)
                         .hasFieldOrPropertyWithValue(PROP_LABEL, PACS_LABEL_3)
                         .hasFieldOrPropertyWithValue(PROP_DEFAULT_QUERY_RETRIEVE_PACS, false)
                         .hasFieldOrPropertyWithValue(PROP_DEFAULT_STORAGE_PACS, false)
                         .hasFieldOrPropertyWithValue(PROP_ORM_STRATEGY_SPRING_BEAN_ID, PACS_ORM_STRATEGY_1)
                         .hasFieldOrPropertyWithValue(PROP_QUERYABLE, true)
                         .hasFieldOrPropertyWithValue(PROP_STORABLE, true);

        // Default Q/R and storage should not have changed.
        final Pacs defaultQueryRetrievePacs2 = _service.findDefaultQueryRetrievePacs().orElse(null);
        final Pacs defaultStoragePacs2       = _service.findDefaultStoragePacs().orElse(null);
        assertThat(defaultQueryRetrievePacs2).isNotNull().hasFieldOrPropertyWithValue("id", pacs1.getId()).hasFieldOrPropertyWithValue(PROP_DEFAULT_QUERY_RETRIEVE_PACS, true);
        assertThat(defaultStoragePacs2).isNotNull().hasFieldOrPropertyWithValue("id", pacs2.getId()).hasFieldOrPropertyWithValue(PROP_DEFAULT_STORAGE_PACS, true);

        pacs3.setDefaultQueryRetrievePacs(true);
        pacs3.setDefaultStoragePacs(true);
        _service.update(pacs3);

        // Default Q/R and storage should now be pacs3
        final Pacs defaultQueryRetrievePacs3 = _service.findDefaultQueryRetrievePacs().orElse(null);
        final Pacs defaultStoragePacs3       = _service.findDefaultStoragePacs().orElse(null);
        assertThat(defaultQueryRetrievePacs3).isNotNull().hasFieldOrPropertyWithValue("id", pacs3.getId()).hasFieldOrPropertyWithValue(PROP_DEFAULT_QUERY_RETRIEVE_PACS, true);
        assertThat(defaultStoragePacs3).isNotNull().hasFieldOrPropertyWithValue("id", pacs3.getId()).hasFieldOrPropertyWithValue(PROP_DEFAULT_STORAGE_PACS, true);

        final Pacs pacs4 = _service.create(Pacs.builder().host(PACS_HOST_2).queryRetrievePort(PACS_PORT_1).aeTitle(PACS_AE_1).label(PACS_LABEL_4).defaultQueryRetrievePacs(true).defaultStoragePacs(true).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(true).build());
        assertThat(pacs4).isNotNull()
                         .hasFieldOrPropertyWithValue(PROP_HOST, PACS_HOST_2)
                         .hasFieldOrPropertyWithValue(PROP_QUERY_RETRIEVE_PORT, PACS_PORT_1)
                         .hasFieldOrPropertyWithValue(PROP_AE_TITLE, PACS_AE_1)
                         .hasFieldOrPropertyWithValue(PROP_LABEL, PACS_LABEL_4)
                         .hasFieldOrPropertyWithValue(PROP_DEFAULT_QUERY_RETRIEVE_PACS, true)
                         .hasFieldOrPropertyWithValue(PROP_DEFAULT_STORAGE_PACS, true)
                         .hasFieldOrPropertyWithValue(PROP_ORM_STRATEGY_SPRING_BEAN_ID, PACS_ORM_STRATEGY_1)
                         .hasFieldOrPropertyWithValue(PROP_QUERYABLE, true)
                         .hasFieldOrPropertyWithValue(PROP_STORABLE, true);

        // Default Q/R and storage should now be pacs4
        final Pacs defaultQueryRetrievePacs4 = _service.findDefaultQueryRetrievePacs().orElse(null);
        final Pacs defaultStoragePacs4       = _service.findDefaultStoragePacs().orElse(null);
        assertThat(defaultQueryRetrievePacs4).isNotNull().hasFieldOrPropertyWithValue("id", pacs4.getId()).hasFieldOrPropertyWithValue(PROP_DEFAULT_QUERY_RETRIEVE_PACS, true);
        assertThat(defaultStoragePacs4).isNotNull().hasFieldOrPropertyWithValue("id", pacs4.getId()).hasFieldOrPropertyWithValue(PROP_DEFAULT_STORAGE_PACS, true);
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

    private final PacsService _service;
}
