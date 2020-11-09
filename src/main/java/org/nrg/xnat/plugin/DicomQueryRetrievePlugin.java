package org.nrg.xnat.plugin;

import lombok.extern.slf4j.Slf4j;
import org.nrg.framework.annotations.XnatPlugin;
import org.nrg.xnat.configuration.DicomImportConfig;
import org.nrg.xnat.configuration.DqrConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@XnatPlugin(value = "dicom_query_retrieve",
            name = "DICOM Query Retrieve Plugin",
            description = "Enables users to search for images in PACS, retrieve them, and push them.",
            entityPackages = "org.nrg.dqr.domain.entities",
            logConfigurationFile = "dqr-logback.xml")
@ComponentScan({"org.nrg.dcm.edit.mizer", "org.nrg.dcm.scp", "org.nrg.dicom.dicomedit.mizer",
                "org.nrg.dicom.mizer.service.impl", "org.nrg.dqr.daos", "org.nrg.dqr.events", "org.nrg.dqr.preferences",
                "org.nrg.dqr.processors", "org.nrg.dqr.services", "org.nrg.xapi.authorization"})
@Import({DicomImportConfig.class, DqrConfig.class})
@Slf4j
public class DicomQueryRetrievePlugin {
    public DicomQueryRetrievePlugin() {
        log.info("I'm now creating the DicomQueryRetrievePlugin object");
    }
}