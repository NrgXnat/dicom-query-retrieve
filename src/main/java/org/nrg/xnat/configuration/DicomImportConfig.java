/*
 * web: org.nrg.xnat.configuration.DicomImportConfig
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.configuration;

import com.google.common.collect.ImmutableList;
import org.dcm4che2.data.Tag;
import org.nrg.dcm.DicomFileNamer;
import org.nrg.dcm.Extractor;
import org.nrg.dcm.TextExtractor;
import org.nrg.dcm.id.ClassicDicomObjectIdentifier;
import org.nrg.dcm.id.CompositeDicomObjectIdentifier;
import org.nrg.dcm.id.RoutedStudyDicomProjectIdentifier;
import org.nrg.dcm.id.TemplatizedDicomFileNamer;
import org.nrg.dcm.xnat.AttributeMapXnatImagesessiondataBeanFactory;
import org.nrg.dcm.xnat.ModalityMapXnatImagesessiondataBeanFactory;
import org.nrg.dcm.xnat.SOPMapXnatImagesessiondataBeanFactory;
import org.nrg.dqr.dicom.id.OverrideStudyIdExtractor;
import org.nrg.dqr.preferences.DqrPreferences;
import org.nrg.dqr.services.StudyIdStudyInstanceUidMappingService;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.user.XnatUserProvider;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xnat.DicomObjectIdentifier;
import org.nrg.xnat.services.cache.UserProjectCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@ComponentScan({"org.nrg.dcm.scp", "org.nrg.dcm.edit.mizer", "org.nrg.dicom.dicomedit.mizer", "org.nrg.dicom.mizer.service.impl", "org.nrg.xnat.services.messaging.archive"})
public class DicomImportConfig {
    @Autowired
    @Bean
    public RoutedStudyDicomProjectIdentifier dqrProjectIdent(final StudyRoutingService service) {
        return new RoutedStudyDicomProjectIdentifier(service);
    }

    @Bean
    public List<Extractor> dqrBaseSubjectIdent() {
        return dqrSubjectExtractors;
    }

    @Bean
    public List<Extractor> dqrSessionIdent(final DqrPreferences preferences, final StudyIdStudyInstanceUidMappingService service) {
        return Collections.singletonList(new OverrideStudyIdExtractor(preferences, service));
    }

    @Bean
    public List<Extractor> dqrBaseAAIdent() {
        return ClassicDicomObjectIdentifier.getAAExtractors();
    }

    @Bean
    public DicomObjectIdentifier<XnatProjectdata> dqrObjectIdentifier(final StudyRoutingService service, final DqrPreferences preferences, final StudyIdStudyInstanceUidMappingService idToUidMappingService) {
        return new CompositeDicomObjectIdentifier(new RoutedStudyDicomProjectIdentifier(service), dqrBaseSubjectIdent(), dqrSessionIdent(preferences, idToUidMappingService), dqrBaseAAIdent());
    }

    @Primary
    @Bean
    public DicomObjectIdentifier<XnatProjectdata> dicomObjectIdentifier(final MessageSource messageSource, final XnatUserProvider receivedFileUserProvider, final UserProjectCache userProjectCache) {
        final String name = messageSource.getMessage("dicomConfig.defaultObjectIdentifier", new Object[]{ClassicDicomObjectIdentifier.class.getSimpleName()}, "Default DICOM object identifier ({0})", Locale.getDefault());
        return new ClassicDicomObjectIdentifier(name, receivedFileUserProvider, userProjectCache);
    }

    @Bean
    public DicomFileNamer dicomFileNamer(final SiteConfigPreferences preferences) {
        return new TemplatizedDicomFileNamer(preferences.getDicomFileNameTemplate());
    }

    @Bean
    public ExecutorService dicomSCPExecutor() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public List<Class<? extends AttributeMapXnatImagesessiondataBeanFactory>> sessionDataFactoryClasses() {
        return Arrays.asList(SOPMapXnatImagesessiondataBeanFactory.class, ModalityMapXnatImagesessiondataBeanFactory.class);
    }

    @Bean
    public List<String> excludedDicomImportFields() {
        return Arrays.asList("SOURCE", "separatePetMr", "prearchivePath");
    }

    private static final ImmutableList<Extractor> dqrSubjectExtractors = new ImmutableList.Builder<Extractor>().add(new TextExtractor(Tag.PatientName)).build();
}
