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

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.nrg.xnatx.dqr.dicom.RetrieveLevel;
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
    public void whenDmsePacsWithoutHostIsConfigured_thenObjectIsNotSaved() {
        final Pacs pacs1 =  _service.create(Pacs.builder().queryRetrievePort(PACS_PORT_2).aeTitle("DUMMY").label("DUMMY").defaultQueryRetrievePacs(false).defaultStoragePacs(false).ormStrategySpringBeanId(PACS_ORM_STRATEGY_2).queryable(true).storable(true).build());
        assertThat(pacs1).isNull();
        final Pacs pacs2 =  _service.create(Pacs.builder().host("DUMMY").queryRetrievePort(PACS_PORT_2).aeTitle("DUMMY").label("DUMMY").defaultQueryRetrievePacs(false).defaultStoragePacs(false).ormStrategySpringBeanId(PACS_ORM_STRATEGY_2).queryable(true).storable(true).build());
        assertThat(pacs2).isNotNull();
    }

    @Test
    public void testDicomWebEnabledPacsTransaction() {
        final Pacs pacs1 = _service.create(Pacs.builder().aeTitle(PACS_AE_1).label(PACS_LABEL_5).dicomWebRootUrl(PACS_DICOMWEB_URL).anonymizationEnabled(true).defaultQueryRetrievePacs(false).defaultStoragePacs(false).dicomObjectIdentifier(PACS_DQR_OBJ_ID).dicomWebEnabled(true).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(false).build());
        assertThat(pacs1).isNotNull()
                .hasFieldOrPropertyWithValue("dicomWebEnabled", true)
                .hasFieldOrPropertyWithValue("dicomWebRootUrl", PACS_DICOMWEB_URL)
                .hasFieldOrPropertyWithValue("host", null)
                .hasFieldOrPropertyWithValue("queryRetrievePort", null)
                .hasFieldOrPropertyWithValue("aeTitle", PACS_AE_1)
                .hasFieldOrPropertyWithValue("label", PACS_LABEL_5)
                .hasFieldOrPropertyWithValue("defaultQueryRetrievePacs", false)
                .hasFieldOrPropertyWithValue("defaultStoragePacs", false)
                .hasFieldOrPropertyWithValue("ormStrategySpringBeanId", PACS_ORM_STRATEGY_1)
                .hasFieldOrPropertyWithValue("queryable", true)
                .hasFieldOrPropertyWithValue("storable", false);
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

    @Test
    public void testDefaultQueryRetrieveAndStoragePacsTransactions() {
        final Pacs pacs1 = _service.create(Pacs.builder().host(PACS_HOST_1).queryRetrievePort(PACS_PORT_1).aeTitle(PACS_AE_1).label(PACS_LABEL_1).defaultQueryRetrievePacs(true).defaultStoragePacs(false).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(true).build());
        assertThat(pacs1).isNotNull()
                         .hasFieldOrPropertyWithValue("host", PACS_HOST_1)
                         .hasFieldOrPropertyWithValue("queryRetrievePort", PACS_PORT_1)
                         .hasFieldOrPropertyWithValue("aeTitle", PACS_AE_1)
                         .hasFieldOrPropertyWithValue("label", PACS_LABEL_1)
                         .hasFieldOrPropertyWithValue("defaultQueryRetrievePacs", true)
                         .hasFieldOrPropertyWithValue("defaultStoragePacs", false)
                         .hasFieldOrPropertyWithValue("ormStrategySpringBeanId", PACS_ORM_STRATEGY_1)
                         .hasFieldOrPropertyWithValue("queryable", true)
                         .hasFieldOrPropertyWithValue("storable", true);
        final Pacs pacs2 = _service.create(Pacs.builder().host(PACS_HOST_2).queryRetrievePort(PACS_PORT_2).aeTitle(PACS_AE_2).label(PACS_LABEL_2).defaultQueryRetrievePacs(false).defaultStoragePacs(true).ormStrategySpringBeanId(PACS_ORM_STRATEGY_2).queryable(true).storable(true).build());
        assertThat(pacs2).isNotNull()
                         .hasFieldOrPropertyWithValue("host", PACS_HOST_2)
                         .hasFieldOrPropertyWithValue("queryRetrievePort", PACS_PORT_2)
                         .hasFieldOrPropertyWithValue("aeTitle", PACS_AE_2)
                         .hasFieldOrPropertyWithValue("label", PACS_LABEL_2)
                         .hasFieldOrPropertyWithValue("defaultQueryRetrievePacs", false)
                         .hasFieldOrPropertyWithValue("defaultStoragePacs", true)
                         .hasFieldOrPropertyWithValue("ormStrategySpringBeanId", PACS_ORM_STRATEGY_2)
                         .hasFieldOrPropertyWithValue("queryable", true)
                         .hasFieldOrPropertyWithValue("storable", true);

        final Pacs defaultQueryRetrievePacs1 = _service.findDefaultQueryRetrievePacs().orElse(null);
        final Pacs defaultStoragePacs1       = _service.findDefaultStoragePacs().orElse(null);
        assertThat(defaultQueryRetrievePacs1).isNotNull().hasFieldOrPropertyWithValue("id", pacs1.getId()).hasFieldOrPropertyWithValue("defaultQueryRetrievePacs", true);
        assertThat(defaultStoragePacs1).isNotNull().hasFieldOrPropertyWithValue("id", pacs2.getId()).hasFieldOrPropertyWithValue("defaultStoragePacs", true);

        // Different PACS, doesn't step on default Q/R or storable attributes.
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

        // Default Q/R and storage should not have changed.
        final Pacs defaultQueryRetrievePacs2 = _service.findDefaultQueryRetrievePacs().orElse(null);
        final Pacs defaultStoragePacs2       = _service.findDefaultStoragePacs().orElse(null);
        assertThat(defaultQueryRetrievePacs2).isNotNull().hasFieldOrPropertyWithValue("id", pacs1.getId()).hasFieldOrPropertyWithValue("defaultQueryRetrievePacs", true);
        assertThat(defaultStoragePacs2).isNotNull().hasFieldOrPropertyWithValue("id", pacs2.getId()).hasFieldOrPropertyWithValue("defaultStoragePacs", true);

        pacs3.setDefaultQueryRetrievePacs(true);
        pacs3.setDefaultStoragePacs(true);
        _service.update(pacs3);

        // Default Q/R and storage should now be pacs3
        final Pacs defaultQueryRetrievePacs3 = _service.findDefaultQueryRetrievePacs().orElse(null);
        final Pacs defaultStoragePacs3       = _service.findDefaultStoragePacs().orElse(null);
        assertThat(defaultQueryRetrievePacs3).isNotNull().hasFieldOrPropertyWithValue("id", pacs3.getId()).hasFieldOrPropertyWithValue("defaultQueryRetrievePacs", true);
        assertThat(defaultStoragePacs3).isNotNull().hasFieldOrPropertyWithValue("id", pacs3.getId()).hasFieldOrPropertyWithValue("defaultStoragePacs", true);

        final Pacs pacs4 = _service.create(Pacs.builder().host(PACS_HOST_2).queryRetrievePort(PACS_PORT_1).aeTitle(PACS_AE_1).label(PACS_LABEL_4).defaultQueryRetrievePacs(true).defaultStoragePacs(true).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(true).build());
        assertThat(pacs4).isNotNull()
                         .hasFieldOrPropertyWithValue("host", PACS_HOST_2)
                         .hasFieldOrPropertyWithValue("queryRetrievePort", PACS_PORT_1)
                         .hasFieldOrPropertyWithValue("aeTitle", PACS_AE_1)
                         .hasFieldOrPropertyWithValue("label", PACS_LABEL_4)
                         .hasFieldOrPropertyWithValue("defaultQueryRetrievePacs", true)
                         .hasFieldOrPropertyWithValue("defaultStoragePacs", true)
                         .hasFieldOrPropertyWithValue("ormStrategySpringBeanId", PACS_ORM_STRATEGY_1)
                         .hasFieldOrPropertyWithValue("queryable", true)
                         .hasFieldOrPropertyWithValue("storable", true);

        // Default Q/R and storage should now be pacs4
        final Pacs defaultQueryRetrievePacs4 = _service.findDefaultQueryRetrievePacs().orElse(null);
        final Pacs defaultStoragePacs4       = _service.findDefaultStoragePacs().orElse(null);
        assertThat(defaultQueryRetrievePacs4).isNotNull().hasFieldOrPropertyWithValue("id", pacs4.getId()).hasFieldOrPropertyWithValue("defaultQueryRetrievePacs", true);
        assertThat(defaultStoragePacs4).isNotNull().hasFieldOrPropertyWithValue("id", pacs4.getId()).hasFieldOrPropertyWithValue("defaultStoragePacs", true);
    }

    @Test
    public void whenNoRetrieveLevelIsConfigured_thenThePacsRetrievesPerSeries() {
        // A PACS saved before this setting existed must keep behaving the way it always has
        final Pacs pacs = _service.create(Pacs.builder().host(PACS_HOST_1).queryRetrievePort(PACS_PORT_1).aeTitle(PACS_AE_1).label(PACS_LABEL_1).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(true).build());
        assertThat(pacs).isNotNull().hasFieldOrPropertyWithValue("retrieveLevel", RetrieveLevel.SERIES);
    }

    @Test
    public void whenStudyLevelRetrieveIsConfiguredOnADimsePacs_thenItIsSaved() {
        final Pacs pacs = _service.create(Pacs.builder().host(PACS_HOST_1).queryRetrievePort(PACS_PORT_1).aeTitle(PACS_AE_1).label(PACS_LABEL_1).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(true).retrieveLevel(RetrieveLevel.STUDY).build());
        assertThat(pacs).isNotNull().hasFieldOrPropertyWithValue("retrieveLevel", RetrieveLevel.STUDY);
    }

    @Test
    public void whenStudyLevelRetrieveIsConfiguredOnADicomWebPacs_thenObjectIsNotSaved() {
        // Study-level retrieve is a C-MOVE feature; DICOMweb retrieval is per series
        final Pacs pacs = _service.create(Pacs.builder().aeTitle(PACS_AE_1).label(PACS_LABEL_5).dicomWebRootUrl(PACS_DICOMWEB_URL).dicomObjectIdentifier(PACS_DQR_OBJ_ID).dicomWebEnabled(true).ormStrategySpringBeanId(PACS_ORM_STRATEGY_1).queryable(true).storable(false).retrieveLevel(RetrieveLevel.STUDY).build());
        assertThat(pacs).isNull();
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
    private static final String PACS_LABEL_5        = "Pacs DICOMWeb 1";
    private static final String PACS_LABEL_6        = "Pacs DICOMWeb 2";

    private static final String PACS_DICOMWEB_URL   = "http://10.2.5.75:8042/dicom-web";
    private static final String PACS_DQR_OBJ_ID     = "dqrObjectIdentifier";

    private static final String PACS_ORM_STRATEGY_1 = "dicomOrmStrategy1";
    private static final String PACS_ORM_STRATEGY_2 = "dicomOrmStrategy2";

    private final PacsService _service;

}
