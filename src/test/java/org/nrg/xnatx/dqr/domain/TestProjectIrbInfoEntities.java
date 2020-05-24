/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.TestProjectIrbInfoEntities
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xnatx.dqr.domain.entities.ProjectIrbFile;
import org.nrg.xnatx.dqr.domain.entities.ProjectIrbInfo;
import org.nrg.xnatx.dqr.services.ProjectIrbInfoEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestProjectIrbInfoEntitiesConfig.class)
public class TestProjectIrbInfoEntities {
    @Autowired
    public TestProjectIrbInfoEntities(final ProjectIrbInfoEntityService service) {
        _service = service;
        _bytes = RandomUtils.nextBytes(20);
    }

    @Test
    public void testProjectIrbInfo() throws NotFoundException {
        final ProjectIrbInfo info = new ProjectIrbInfo();
        info.setProjectId("foo");
        info.setIrbNumber("bar");
        _service.create(info);
        final ProjectIrbInfo found = _service.findIrbInfoForProject("foo");
        assertThat(found).hasFieldOrPropertyWithValue("projectId", "foo").hasFieldOrPropertyWithValue("irbNumber", "bar");
    }

    @Test
    @Disabled
    public void testBasicEntities() throws NotFoundException {
        final ProjectIrbInfo info = new ProjectIrbInfo();
        info.setProjectId("foo");
        info.setIrbNumber("bar");
        info.addIrbFile("xxx", _bytes);
        _service.create(info);
        final ProjectIrbInfo found = _service.findIrbInfoForProject("foo");
        final ProjectIrbFile file  = found.getProjectIrbFiles().get(0);
        assertThat(found).hasFieldOrPropertyWithValue("projectId", "foo").hasFieldOrPropertyWithValue("irbNumber", "bar");
        assertThat(file).hasFieldOrPropertyWithValue("irbFileName", "xxx").hasFieldOrPropertyWithValue("irbFile", _bytes);
    }

    private final ProjectIrbInfoEntityService _service;
    private final byte[]                      _bytes;
}
