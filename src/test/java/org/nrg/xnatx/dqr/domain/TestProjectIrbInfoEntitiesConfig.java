/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.TestProjectIrbInfoEntities
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain;

import static org.mockito.Mockito.when;

import org.mockito.Mockito;
import org.nrg.framework.orm.hibernate.HibernateEntityPackageList;
import org.nrg.framework.services.SerializerService;
import org.nrg.framework.test.OrmTestConfiguration;
import org.nrg.prefs.services.NrgPreferenceService;
import org.nrg.xnat.preferences.FileStorePreferences;
import org.nrg.xnat.services.archive.FileStore;
import org.nrg.xnat.services.archive.impl.hibernate.FileStoreInfoDAO;
import org.nrg.xnat.services.archive.impl.hibernate.HibernateFileStoreService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@Import(OrmTestConfiguration.class)
@ComponentScan({"org.nrg.xnatx.dqr.domain.daos", "org.nrg.xnatx.dqr.services.impl.hibernate"})
@TestPropertySource(locations = "classpath:/test.properties")
public class TestProjectIrbInfoEntitiesConfig {
    @Bean
    public HibernateEntityPackageList dqrEntities() {
        return new HibernateEntityPackageList("org.nrg.xnatx.dqr.domain.entities", "org.nrg.xnat.entities");
    }

    @Bean
    public NrgPreferenceService nrgPreferenceService() {
        return Mockito.mock(NrgPreferenceService.class);
    }

    @Bean
    public FileStoreInfoDAO fileStoreInfoDAO() {
        return new FileStoreInfoDAO();
    }

    @Bean
    public FileStorePreferences fileStorePreferences() throws IOException {
        final Path temp = Files.createTempDirectory("testProjectIrbInfoEntitiesConfig-");
        temp.toFile().deleteOnExit();
        final FileStorePreferences preferences = Mockito.mock(FileStorePreferences.class);
        when(preferences.getFileStorePath()).thenReturn(temp.toString());
        return preferences;
    }

    @Bean
    public FileStore fileStore() throws IOException {
        return new HibernateFileStoreService(fileStorePreferences());
    }

    @Bean
    public SerializerService serializerService() {
        return Mockito.mock(SerializerService.class);
    }
}
