/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.DicomQueryRetrievePlugin
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.extern.slf4j.Slf4j;
import org.nrg.dcm.id.DicomObjectIdentifierFactory;
import org.nrg.framework.annotations.XnatPlugin;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xnatx.dqr.dicom.converters.*;
import org.nrg.xnatx.dqr.dicom.id.DqrDicomObjectIdentifierFactory;
import org.nrg.xnatx.dqr.dicom.strategy.orm.*;
import org.nrg.xnatx.dqr.dicom.strategy.orm.dcm4chee.Dcm4cheeResultSetLimitStrategy;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.services.StudyIdStudyInstanceUidMappingService;
import org.nrg.xnatx.dqr.utils.DqrDateRange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@XnatPlugin(value = "dicom-query-retrieve",
            name = "DICOM Query Retrieve Plugin",
            description = "Enables users to search for images in PACS, retrieve them, and push them.",
            entityPackages = "org.nrg.xnatx.dqr.domain.entities",
            logConfigurationFile = "dqr-logback.xml")
@ComponentScan({"org.nrg.xnatx.dqr.domain.daos",
                "org.nrg.xnatx.dqr.events",
                "org.nrg.xnatx.dqr.events.listeners.methods",
                "org.nrg.xnatx.dqr.messaging",
                "org.nrg.xnatx.dqr.preferences",
                "org.nrg.xnatx.dqr.processors",
                "org.nrg.xnatx.dqr.rest",
                "org.nrg.xnatx.dqr.security",
                "org.nrg.xnatx.dqr.services",
                "org.nrg.xnatx.dqr.tasks"})
@Slf4j
public class DicomQueryRetrievePlugin {
    @Bean
    public OrmStrategy dicomOrmStrategy() {
        return new BasicOrmStrategy(patientNameStrategy(), resultSetLimitStrategy());
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
    public DicomObjectIdentifierFactory dqrDicomObjectIdentifierFactory(final StudyRoutingService studyRoutingService, final StudyIdStudyInstanceUidMappingService mappingService, final DqrPreferences preferences) {
        return new DqrDicomObjectIdentifierFactory( studyRoutingService, mappingService, preferences);
    }

    @Bean
    public Module dqrModule() {
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(DqrDateRange.class, new DqrDateRangeDeSerializer());
        module.addDeserializer(PacsSearchCriteria.class, new PacsSearchCriteriaDeserializer());
        module.addSerializer(new DqrDateRangeSerializer());
        module.addSerializer(new PacsSearchResultsSerializer());
        module.addSerializer(new PatientSerializer());
        module.addSerializer(new StudySerializer());
        return module;
    }
}
