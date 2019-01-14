/*
 * web: org.nrg.xnat.initialization.tasks.UpdateUserAuthTable
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.initialization.tasks;

import org.nrg.dqr.processors.UpdateRequestStatusArchiveProcessor;
import org.nrg.xnat.archive.operations.ProcessorGradualDicomImportOperation;
import org.nrg.xnat.entities.ArchiveProcessorInstance;
import org.nrg.xnat.processor.services.ArchiveProcessorInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@Component
public class SetupDqrProcessorInstances extends AbstractInitializingTask {
    @Autowired
    public SetupDqrProcessorInstances(final ArchiveProcessorInstanceService archiveProcessorInstanceService) {
        super();
        _archiveProcessorInstanceService = archiveProcessorInstanceService;
    }

    @Override
    public String getTaskName() {
        return "Update the processor instance table";
    }

    @Override
    protected void callImpl() throws InitializingTaskException {
        try{
            String dqrUpdateRequestStatusClassName = UpdateRequestStatusArchiveProcessor.class.getCanonicalName();
            List<ArchiveProcessorInstance> dqrProcessors = _archiveProcessorInstanceService.getAllSiteProcessorsForClass(dqrUpdateRequestStatusClassName);
            // Creates a DQR Update Request Status processor instance of none exists. Such an instance is necessary for request status to be updated (but the instance can be disabled if not desired for whatever reason).
            if(dqrProcessors==null || dqrProcessors.size()==0){
                //The processor instances table is new. Add default processor instances.
                ArchiveProcessorInstance defaultUpdateRequestStatusProcessor = new ArchiveProcessorInstance();
                defaultUpdateRequestStatusProcessor.setLocation(ProcessorGradualDicomImportOperation.NAME_OF_LOCATION_AT_BEGINNING_AFTER_DICOM_OBJECT_IS_READ);
                defaultUpdateRequestStatusProcessor.setLabel("Update Request");
                defaultUpdateRequestStatusProcessor.setPriority(10);
                defaultUpdateRequestStatusProcessor.setProcessorClass(UpdateRequestStatusArchiveProcessor.class.getCanonicalName());
                defaultUpdateRequestStatusProcessor.setScope("site");
                defaultUpdateRequestStatusProcessor.setParameters(new HashMap<String, String>());
                defaultUpdateRequestStatusProcessor.setScpBlacklist(new HashSet<String>());
                defaultUpdateRequestStatusProcessor.setScpWhitelist(new HashSet<String>());
                _archiveProcessorInstanceService.create(defaultUpdateRequestStatusProcessor);
            }
        } catch (Exception e) {
            throw new InitializingTaskException(InitializingTaskException.Level.Error, "An error occurred initializing the DQR Update Request Status Archive Processor Instance", e);
        }
    }

    private final ArchiveProcessorInstanceService _archiveProcessorInstanceService;
}
