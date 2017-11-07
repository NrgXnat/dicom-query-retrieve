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
import org.nrg.framework.orm.hibernate.HibernateEntityPackageList;
import org.nrg.tip.dicom.id.StudyIdDicomSessionIdentifier;
import org.nrg.tip.dicom.strategy.orm.BasicOrmStrategy;
import org.nrg.tip.dicom.strategy.orm.dcm4chee.Dcm4cheeResultSetLimitStrategy;
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
@ComponentScan({"org.nrg.tip.services", "org.nrg.tip.daos"})
public class TipImageSearchConfig {
    @Bean
    @Primary
    public BasicOrmStrategy dicomOrmStrategy() {
        BasicOrmStrategy strat = new BasicOrmStrategy();
        strat.setResultSetLimitStrategy(dicomOrmResultSetLimitStrategy());
        return strat;
    }

    @Bean
    public Dcm4cheeResultSetLimitStrategy dicomOrmResultSetLimitStrategy() {
        return new Dcm4cheeResultSetLimitStrategy();
    }

    @Bean
    public HibernateEntityPackageList tipEntityPackages() {
        HibernateEntityPackageList list = new HibernateEntityPackageList();
        list.setItems(Arrays.asList("org.nrg.tip.domain.entities"));
        return list;
    }

}
