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

import com.google.common.io.BaseEncoding;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xapi.exceptions.ResourceAlreadyExistsException;
import org.nrg.xnat.entities.FileStoreInfo;
import org.nrg.xnatx.dqr.domain.entities.ProjectIrbInfo;
import org.nrg.xnatx.dqr.services.ProjectIrbInfoEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestProjectIrbInfoEntitiesConfig.class)
public class TestProjectIrbInfoEntities {
    @Autowired
    public TestProjectIrbInfoEntities(final ProjectIrbInfoEntityService service) {
        _service = service;
        _bytes = IntStream.range(0, 10).mapToObj(index -> RandomUtils.nextBytes(20)).collect(Collectors.toList());
    }

    @Test
    public void testProjectIrbInfo() throws NotFoundException {
        final ProjectIrbInfo info = new ProjectIrbInfo();
        info.setProjectId("bar");
        info.setIrbNumber("foo");
        _service.create(info);
        final ProjectIrbInfo found = _service.findIrbInfoForProject("bar");
        assertThat(found).hasFieldOrPropertyWithValue("projectId", "bar").hasFieldOrPropertyWithValue("irbNumber", "foo");
    }

    @Test
    public void testBasicEntities() throws NotFoundException, ResourceAlreadyExistsException, NoSuchAlgorithmException {
        final ProjectIrbInfo created = _service.createNewIrbInfo("foo", "bar", "foo-bar.txt", _bytes.get(0));
        final ProjectIrbInfo found   = _service.findIrbInfoForProject("foo");
        final FileStoreInfo  file    = found.getProjectIrbFiles().get(0);
        assertThat(created).hasFieldOrPropertyWithValue("projectId", "foo").hasFieldOrPropertyWithValue("irbNumber", "bar");
        assertThat(found).hasFieldOrPropertyWithValue("projectId", "foo").hasFieldOrPropertyWithValue("irbNumber", "bar").isEqualTo(created);
        assertThat(file).hasFieldOrPropertyWithValue("label", "foo-bar.txt").hasFieldOrPropertyWithValue("coordinates", "foo/bar/foo-bar.txt").hasFieldOrPropertyWithValue("checksum", checksum(getBytes(file.getStoreUri())));
        assertThat(file.getStoreUri().toString().endsWith(StringUtils.join(BaseEncoding.base16().encode(MessageDigest.getInstance("SHA-256").digest(file.getCoordinates().getBytes())).split("(?<=\\G.{8})"), "/")));
    }

    @Test
    public void testMultiFileIrbInfo() throws NotFoundException, ResourceAlreadyExistsException, NoSuchAlgorithmException {
        final AtomicInteger  index     = new AtomicInteger(1);
        final List<Resource> resources = _bytes.stream().map(bytes -> new ByteArrayResource(bytes, "test-" + index.getAndIncrement() + ".pdf")).collect(Collectors.toList());
        final ProjectIrbInfo created   = _service.createNewIrbInfo("A", "B", resources);
        final ProjectIrbInfo found     = _service.findIrbInfoForProject("A");
        assertThat(created).hasFieldOrPropertyWithValue("projectId", "A").hasFieldOrPropertyWithValue("irbNumber", "B");
        assertThat(created.getProjectIrbFiles()).size().isEqualTo(10);
        assertThat(found).hasFieldOrPropertyWithValue("projectId", "A").hasFieldOrPropertyWithValue("irbNumber", "B").isEqualTo(created);
        final String prefix = "A/B/";
        for (final FileStoreInfo file : found.getProjectIrbFiles()) {
            final String label = file.getLabel();
            assertThat(file).hasFieldOrPropertyWithValue("label", label).hasFieldOrPropertyWithValue("coordinates", prefix + label).hasFieldOrPropertyWithValue("size", 20L);
            assertThat(file.getStoreUri().toString().endsWith(StringUtils.join(BaseEncoding.base16().encode(MessageDigest.getInstance("SHA-256").digest(file.getCoordinates().getBytes())).split("(?<=\\G.{8})"), "/")));
        }
    }

    private static String checksum(final byte[] bytes) {
        try {
            return BaseEncoding.base16().encode(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("An error occurred trying to get the SHA-256 message digest", e);
        }
    }

    private static byte[] getBytes(final URI storeUri) {
        try (final InputStream input = new FileInputStream(Paths.get(storeUri).toFile())) {
            return IOUtils.readFully(input, 20);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final ProjectIrbInfoEntityService _service;
    private final List<byte[]>                _bytes;
}
