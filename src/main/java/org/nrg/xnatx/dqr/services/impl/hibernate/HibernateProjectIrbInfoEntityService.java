/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.impl.hibernate.HibernateProjectIrbInfoEntityService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services.impl.hibernate;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xapi.exceptions.ResourceAlreadyExistsException;
import org.nrg.xnat.entities.FileStoreInfo;
import org.nrg.xnat.services.archive.FileStore;
import org.nrg.xnatx.dqr.domain.daos.ProjectIrbInfoDAO;
import org.nrg.xnatx.dqr.domain.entities.ProjectIrbInfo;
import org.nrg.xnatx.dqr.services.ProjectIrbInfoEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class HibernateProjectIrbInfoEntityService extends AbstractHibernateEntityService<ProjectIrbInfo, ProjectIrbInfoDAO> implements ProjectIrbInfoEntityService {
    @Autowired
    public HibernateProjectIrbInfoEntityService(final FileStore fileStore) {
        _fileStore = fileStore;
    }

    @Override
    public ProjectIrbInfo findIrbInfoForProject(final String projectId) throws NotFoundException {
        return getDao().findIrbInfoForProject(projectId);
    }

    @Override
    public String findIrbNumberForProject(final String projectId) throws NotFoundException {
        return getDao().findIrbNumberForProject(projectId);
    }

    @Override
    public List<String> findIrbFileNamesForProject(final String projectId) throws NotFoundException {
        return getDao().findIrbFileNamesForProject(projectId);
    }

    @Override
    public List<FileStoreInfo> findIrbFilesForProject(final String projectId) throws NotFoundException {
        return getDao().findIrbFilesForProject(projectId);
    }

    @Override
    public void addIrbFile(final ProjectIrbInfo info, final String fileName, final byte[] bytes) throws ResourceAlreadyExistsException {
        info.addIrbFile(storeIrbFile(info.getProjectId(), info.getIrbNumber(), fileName, bytes));
        getDao().saveOrUpdate(info);
    }

    @Override
    public ProjectIrbInfo createNewIrbInfo(final String projectId, final String irbNumber, final String fileName, final byte[] bytes) throws ResourceAlreadyExistsException {
        return createNewIrbInfo(projectId, irbNumber, new ByteArrayResource(bytes, fileName));
    }

    @Override
    public ProjectIrbInfo createNewIrbInfo(final String projectId, final String irbNumber, final Resource... resources) throws ResourceAlreadyExistsException {
        return createNewIrbInfo(projectId, irbNumber, Arrays.asList(resources));
    }

    @Override
    public ProjectIrbInfo createNewIrbInfo(final String projectId, final String irbNumber, final Collection<? extends Resource> resources) throws ResourceAlreadyExistsException {
        final ProjectIrbInfo.ProjectIrbInfoBuilder builder = ProjectIrbInfo.builder().projectId(projectId).irbNumber(irbNumber);
        for (final Resource resource : resources) {
            final String resourceName = getResourceName(resource);
            try (final InputStream input = resource.getInputStream()) {
                builder.projectIrbFile(storeIrbFile(projectId, irbNumber, resourceName, input));
            } catch (IOException e) {
                log.error("An error occurred creating IRB info for project {} IRB #{} while trying to store the resource {}", projectId, irbNumber, resourceName, e);
            }
        }
        getDao().saveOrUpdate(builder.build());
        try {
            return getDao().findIrbInfoForProject(projectId);
        } catch (NotFoundException e) {
            throw new RuntimeException("Couldn't find IRB info for project " + projectId + " but I just saved a new IRB info object with the IRB number " + irbNumber + " and files " + resources.stream().map(HibernateProjectIrbInfoEntityService::getResourceName).collect(Collectors.joining(", ")), e);
        }
    }

    private FileStoreInfo storeIrbFile(final String projectId, final String irbNumber, final String fileName, final byte[] bytes) throws ResourceAlreadyExistsException {
        try (final InputStream input = new ByteArrayInputStream(bytes)) {
            return storeIrbFile(projectId, irbNumber, fileName, input);
        } catch (IOException e) {
            throw new RuntimeException("An error occurred trying to create a file entry for project " + projectId + " IRB #" + irbNumber, e);
        }
    }

    private FileStoreInfo storeIrbFile(final String projectId, final String irbNumber, final String fileName, final InputStream input) throws ResourceAlreadyExistsException {
        return _fileStore.create(input, projectId, irbNumber, fileName);
    }

    private static String getResourceName(final Resource resource) {
        final String fileName = resource.getFilename();
        if (StringUtils.isNotBlank(fileName)) {
            return fileName;
        }
        final String description = resource.getDescription();
        if (StringUtils.isNotBlank(description)) {
            final Matcher matcher = RESOURCE_DESCRIPTION.matcher(description);
            return matcher.find() ? matcher.group("resourceName") : description;
        }
        try {
            final File file = resource.getFile();
            if (file != null) {
                return file.getName();
            }
        } catch (IOException ignored) {
            // For many resources this throws an exception.
        }
        try {
            final URI uri = resource.getURI();
            if (uri != null) {
                return Paths.get(uri).getFileName().toString();
            }
        } catch (IOException ignored) {
            // For many resources this throws an exception.
        }
        try {
            final URL url = resource.getURL();
            if (url != null) {
                return url.getFile();
            }
        } catch (IOException ignored) {
            // For many resources this throws an exception.
        }
        throw new RuntimeException("There is nothing available as a resource name for the resource " + resource);
    }

    private static final Pattern RESOURCE_DESCRIPTION = Pattern.compile("^.*(resource|file|path|URL)\\s+\\[(?<resourceName>[^]]+)]$");

    private final FileStore _fileStore;
}
