/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.impl.basic.DqrRoleRepositoryService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services.impl.basic;

import static org.nrg.xdat.turbine.utils.PropertiesHelper.RetrievePropertyObjects;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.nrg.framework.utilities.BasicXnatResourceLocator;
import org.nrg.framework.utilities.Reflection;
import org.nrg.xdat.security.services.RoleRepositoryServiceI;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DqrRoleRepositoryService implements RoleRepositoryServiceI {
    public DqrRoleRepositoryService() {
        final Set<String> propFiles = Reflection.findResources(ROLE_DEFINITION_PACKAGE, ROLE_DEFINITION_PROPERTIES);
        if (propFiles != null) {
            for (final String props : propFiles) {
                _allRoles.addAll(RetrievePropertyObjects(props, PROP_OBJECT_IDENTIFIER, PROP_OBJECT_FIELDS).values().stream().filter(Objects::nonNull).map(PROPS_TO_ROLE_DEF_FUNCTION).collect(Collectors.toList()));
            }
        }
        try {
            final List<Resource> resources = BasicXnatResourceLocator.getResources("classpath*:META-INF/conf/roles/**/*-definition.properties");
            for (final Resource resource : resources) {
                _allRoles.addAll(RetrievePropertyObjects(resource.getURL(), PROP_OBJECT_IDENTIFIER, PROP_OBJECT_FIELDS).values().stream().map(PROPS_TO_ROLE_DEF_FUNCTION).collect(Collectors.toList()));
            }
        } catch (IOException e) {
            log.warn("An error occurred trying to find role definitions from resources matching the pattern \"classpath*:META-INF/conf/roles/**/*-definition.properties\"", e);
        }
    }

    @Override
    public Collection<RoleDefinitionI> getRoles() {
        return _allRoles;
    }

    @Getter
    @Builder
    private static class POJORole implements RoleDefinitionI {
        private final String name;
        private final String description;
        private final String key;
        private final String warning;
    }

    private static final String   ROLE_DEFINITION_PACKAGE    = "config.roles";
    private static final Pattern  ROLE_DEFINITION_PROPERTIES = Pattern.compile(".*-role-definition\\.properties");
    private static final String   NAME                       = "name";
    private static final String   DESC                       = "description";
    private static final String   KEY                        = "key";
    private static final String   WARNING                    = "warning";
    private static final String[] PROP_OBJECT_FIELDS         = new String[]{NAME, DESC, KEY, WARNING};
    private static final String   PROP_OBJECT_IDENTIFIER     = "org.nrg.Role";

    private static final Function<Map<String, Object>, RoleDefinitionI> PROPS_TO_ROLE_DEF_FUNCTION = properties -> POJORole.builder()
                                                                                                                           .key((String) properties.get(KEY))
                                                                                                                           .name(properties.get(NAME).toString())
                                                                                                                           .description(properties.get(DESC).toString())
                                                                                                                           .warning(properties.get(WARNING).toString())
                                                                                                                           .build();

    private final Set<RoleDefinitionI> _allRoles = new TreeSet<>(Comparator.comparing(RoleDefinitionI::getKey));
}
