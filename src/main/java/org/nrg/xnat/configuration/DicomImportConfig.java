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
import org.nrg.dcm.Extractor;
import org.nrg.dcm.id.ClassicDicomObjectIdentifier;
import org.nrg.dcm.id.CompositeDicomObjectIdentifier;
import org.nrg.dcm.id.RoutedStudyDicomProjectIdentifier;
import org.nrg.dcm.id.TemplatizedDicomFileNamer;
import org.nrg.dcm.xnat.AttributeMapXnatImagesessiondataBeanFactory;
import org.nrg.dcm.xnat.SOPMapXnatImagesessiondataBeanFactory;
import org.nrg.dqr.dicom.id.StudyIdDicomSessionIdentifier;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xnat.DicomObjectIdentifier;
import org.nrg.xnat.services.cache.UserProjectCache;
import org.nrg.xdat.security.user.XnatUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@ComponentScan({"org.nrg.dcm.scp", "org.nrg.dcm.edit.mizer", "org.nrg.dicom.dicomedit.mizer", "org.nrg.dicom.mizer.service.impl"})
public class DicomImportConfig {
    @Autowired
    @Bean
    public RoutedStudyDicomProjectIdentifier dqrProjectIdent(final StudyRoutingService service) throws Exception {
        return new RoutedStudyDicomProjectIdentifier(service);
    }

    @Autowired
    @Bean
    public List<Extractor> baseSubjectIdent(final StudyRoutingService service, final MessageSource messageSource, final XnatUserProvider receivedFileUserProvider, final UserProjectCache userProjectCache) throws Exception {
        final String name = messageSource.getMessage("dicomConfig.defaultObjectIdentifier", new Object[]{ClassicDicomObjectIdentifier.class.getSimpleName()}, "Default DICOM object identifier ({0})", Locale.getDefault());
        ClassicDicomObjectIdentifier classicDicomObjectIdentifier = new ClassicDicomObjectIdentifier(name, receivedFileUserProvider, userProjectCache);
        return classicDicomObjectIdentifier.getSubjectExtractors();
    }

    @Autowired
    @Bean
    public List<Extractor> dqrSessionIdent() throws Exception {
        return StudyIdDicomSessionIdentifier.getSessionExtractors();
    }

    @Autowired
    @Bean
    public List<Extractor> baseAAIdent(final StudyRoutingService service, final MessageSource messageSource, final XnatUserProvider receivedFileUserProvider, final UserProjectCache userProjectCache) throws Exception {
        final String name = messageSource.getMessage("dicomConfig.defaultObjectIdentifier", new Object[]{ClassicDicomObjectIdentifier.class.getSimpleName()}, "Default DICOM object identifier ({0})", Locale.getDefault());
        ClassicDicomObjectIdentifier classicDicomObjectIdentifier = new ClassicDicomObjectIdentifier(name, receivedFileUserProvider, userProjectCache);
        return classicDicomObjectIdentifier.getAAExtractors();
    }

    @Primary
    @Bean
    public DicomObjectIdentifier<XnatProjectdata> dicomObjectIdentifier(final StudyRoutingService service, final MessageSource messageSource, final XnatUserProvider receivedFileUserProvider, final UserProjectCache userProjectCache) throws Exception {
        final RoutedStudyDicomProjectIdentifier routedStudyDicomProjectIdentifier = new RoutedStudyDicomProjectIdentifier(service);
//        final String name = messageSource.getMessage("dicomConfig.defaultObjectIdentifier", new Object[]{ClassicDicomObjectIdentifier.class.getSimpleName()}, "Default DICOM object identifier ({0})", Locale.getDefault());
//        ClassicDicomObjectIdentifier classicDicomObjectIdentifier = new ClassicDicomObjectIdentifier(name, receivedFileUserProvider, userProjectCache);
//        return new CompositeDicomObjectIdentifier(routedStudyDicomProjectIdentifier, classicDicomObjectIdentifier.getSubjectExtractors(), StudyIdDicomSessionIdentifier.getSessionExtractors(), classicDicomObjectIdentifier.getAAExtractors());
        return new CompositeDicomObjectIdentifier(routedStudyDicomProjectIdentifier, baseSubjectIdent(service, messageSource, receivedFileUserProvider, userProjectCache), dqrSessionIdent(), baseAAIdent(service, messageSource, receivedFileUserProvider, userProjectCache));
    }

    @Bean
    public DicomFileNamer dicomFileNamer() throws Exception {
        return new TemplatizedDicomFileNamer("$XXX_XXX.${Modality}.XXX_XXX.${SeriesNumber}.${InstanceNumber}.19000101.120000.${HashSOPClassUIDWithInstanceNumber}");
    }

    @Bean
    public ExecutorService dicomSCPExecutor() throws Exception {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public List<Class<? extends AttributeMapXnatImagesessiondataBeanFactory>> sessionDataFactoryClasses() {
        return Arrays.asList(SOPMapXnatImagesessiondataBeanFactory.class, org.nrg.dcm.xnat.ModalityMapXnatImagesessiondataBeanFactory.class);
    }

    @Bean
    public List<String> excludedDicomImportFields() {
        return Arrays.asList("SOURCE", "separatePetMr", "prearchivePath");
    }
}
