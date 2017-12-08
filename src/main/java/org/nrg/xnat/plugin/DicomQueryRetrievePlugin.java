package org.nrg.xnat.plugin;

import org.nrg.framework.annotations.XnatDataModel;
import org.nrg.framework.annotations.XnatPlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;

@XnatPlugin(value = "dicom_query_retrieve", name = "DICOM Query Retrieve Plugin", description = "Enables users to search for images in PACS, retrieve them, and push them.",
    entityPackages = "org.nrg.dqr.domain.entities")
@ComponentScan({"org.nrg.dqr.services", "org.nrg.dqr.daos", "org.nrg.dcm.scp", "org.nrg.dcm.edit.mizer",
        "org.nrg.dicom.dicomedit.mizer", "org.nrg.dicom.mizer.service.impl"})
public class DicomQueryRetrievePlugin {
}