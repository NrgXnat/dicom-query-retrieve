/*
 * web: org.nrg.xnat.configuration.DicomImportConfig
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.configuration;

import org.nrg.dcm.DicomFileNamer;
import org.nrg.dcm.id.ClassicDicomObjectIdentifier;
import org.nrg.dcm.id.CompositeDicomObjectIdentifier;
import org.nrg.dcm.id.RoutedStudyDicomProjectIdentifier;
import org.nrg.dcm.id.TemplatizedDicomFileNamer;
import org.nrg.tip.dicom.id.StudyIdDicomSessionIdentifier;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xnat.DicomObjectIdentifier;
import org.nrg.xnat.services.cache.UserProjectCache;
import org.nrg.xnat.utils.XnatUserProvider;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Configuration
@ComponentScan({"org.nrg.dcm.scp", "org.nrg.dcm.edit.mizer", "org.nrg.dicom.dicomedit.mizer", "org.nrg.dicom.mizer.service.impl"})
public class DicomImportConfig {
    @Bean
    @Primary
    public DicomObjectIdentifier<XnatProjectdata> dicomObjectIdentifier(final StudyRoutingService service, final MessageSource messageSource, final XnatUserProvider receivedFileUserProvider, final UserProjectCache userProjectCache) {
        final RoutedStudyDicomProjectIdentifier routedStudyDicomProjectIdentifier = new RoutedStudyDicomProjectIdentifier(service);
        final String name = messageSource.getMessage("dicomConfig.defaultObjectIdentifier", new Object[]{ClassicDicomObjectIdentifier.class.getSimpleName()}, "Default DICOM object identifier ({0})", Locale.getDefault());
        ClassicDicomObjectIdentifier classicDicomObjectIdentifier = new ClassicDicomObjectIdentifier(name, receivedFileUserProvider, userProjectCache);
        return new CompositeDicomObjectIdentifier(routedStudyDicomProjectIdentifier, classicDicomObjectIdentifier.getSubjectExtractors(), StudyIdDicomSessionIdentifier.getSessionExtractors(), classicDicomObjectIdentifier.getAAExtractors());
    }

    @Bean
    public DicomFileNamer dicomFileNamer() throws Exception {
        return new TemplatizedDicomFileNamer("$XXX_XXX.${Modality}.XXX_XXX.${SeriesNumber}.${InstanceNumber}.19000101.120000.${HashSOPClassUIDWithInstanceNumber}");
    }

    @Bean
    public List<String> sessionDataFactoryClasses() {
        return Arrays.asList("org.nrg.dcm.xnat.SOPMapXnatImagesessiondataBeanFactory", "org.nrg.dcm.xnat.ModalityMapXnatImagesessiondataBeanFactory");
    }

    @Bean
    public List<String> excludedDicomImportFields() {
        return Arrays.asList("SOURCE", "separatePetMr", "prearchivePath");
    }
}
