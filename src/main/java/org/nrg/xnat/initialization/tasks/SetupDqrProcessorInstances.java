/*
 * web: org.nrg.xnat.initialization.tasks.UpdateUserAuthTable
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.initialization.tasks;

import org.nrg.dqr.processors.DqrAnonArchiveProcessor;
import org.nrg.xnat.initialization.tasks.AbstractInitializingTask;
import org.nrg.xnat.initialization.tasks.InitializingTaskException;
import org.nrg.xnat.archive.operations.ProcessorGradualDicomImportOperation;
import org.nrg.xnat.entities.ArchiveProcessorInstance;
import org.nrg.xnat.processor.services.ArchiveProcessorInstanceService;
import org.nrg.xnat.processors.MizerArchiveProcessor;
import org.nrg.xnat.processors.StudyRemappingArchiveProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.nrg.xdat.XDAT;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@SuppressWarnings("SqlDialectInspection")
@Component
public class SetupDqrProcessorInstances extends AbstractInitializingTask {
    @Autowired
    public SetupDqrProcessorInstances(final JdbcTemplate template, final ArchiveProcessorInstanceService archiveProcessorInstanceService) {
        super();
        _template = template;
        _archiveProcessorInstanceService = archiveProcessorInstanceService;
    }

    @Override
    public String getTaskName() {
        return "Update the user authentication table";
    }

    @Override
    protected void callImpl() throws InitializingTaskException {
        try{
            String dqrAnonClassName = DqrAnonArchiveProcessor.class.getCanonicalName();
            List<ArchiveProcessorInstance> dqrProcessors = _archiveProcessorInstanceService.getAllSiteProcessorsForClass(dqrAnonClassName);
            // Creates a DQR Anon processor instance of none exists. Such an instance is necessary for all DQR functionality to work properly (but the instance can be disabled if not desired for whatever reason).
            if(dqrProcessors==null || dqrProcessors.size()==0){
                //The processor instances table is new. Add default processor instances.
                ArchiveProcessorInstance defaultDqrAnonProcessor = new ArchiveProcessorInstance();
                defaultDqrAnonProcessor.setLocation(ProcessorGradualDicomImportOperation.NAME_OF_LOCATION_AFTER_PROJECT_HAS_BEEN_ASSIGNED);
                defaultDqrAnonProcessor.setLabel("DQR Anonymization");
                defaultDqrAnonProcessor.setPriority(20);
                defaultDqrAnonProcessor.setProcessorClass(dqrAnonClassName);
                defaultDqrAnonProcessor.setScope("site");
                defaultDqrAnonProcessor.setParameters(new HashMap<String, String>());
                defaultDqrAnonProcessor.setScpBlacklist(new HashSet<String>());
                defaultDqrAnonProcessor.setScpWhitelist(new HashSet<String>());
                _archiveProcessorInstanceService.create(defaultDqrAnonProcessor);
            }
        } catch (Exception e) {
            throw new InitializingTaskException(InitializingTaskException.Level.Error, "An error occurred initializing the DQR Anon Archive Processor Instance", e);
        }
    }

    private static final Logger _log = LoggerFactory.getLogger(SetupDqrProcessorInstances.class);

    private final JdbcTemplate _template;
    private final ArchiveProcessorInstanceService _archiveProcessorInstanceService;
}
