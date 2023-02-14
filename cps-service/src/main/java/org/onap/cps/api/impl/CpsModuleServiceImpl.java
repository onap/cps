/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2023 Nordix Foundation
 *  Modifications Copyright (C) 2020-2021 Pantheon.tech
 *  Modifications Copyright (C) 2022 Bell Canada
 *  Modifications Copyright (C) 2022 TechMahindra Ltd
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.api.impl;

import io.micrometer.core.annotation.Timed;
import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.spi.CascadeDeleteAllowed;
import org.onap.cps.spi.CpsModulePersistenceService;
import org.onap.cps.spi.exceptions.SchemaSetInUseException;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.model.ModuleDefinition;
import org.onap.cps.spi.model.ModuleReference;
import org.onap.cps.spi.model.SchemaSet;
import org.onap.cps.spi.utils.CpsValidator;
import org.onap.cps.yang.TimedYangTextSchemaSourceSetBuilder;
import org.onap.cps.yang.YangTextSchemaSourceSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("CpsModuleServiceImpl")
@RequiredArgsConstructor
public class CpsModuleServiceImpl implements CpsModuleService {

    private final CpsModulePersistenceService cpsModulePersistenceService;
    private final YangTextSchemaSourceSetCache yangTextSchemaSourceSetCache;
    private final CpsAdminService cpsAdminService;
    private final CpsValidator cpsValidator;
    private final TimedYangTextSchemaSourceSetBuilder timedYangTextSchemaSourceSetBuilder;

    @Override
    @Timed(value = "cps.module.service.schemaset.create",
        description = "Time taken to create (and store) a schemaset")
    public void createSchemaSet(final String dataspaceName, final String schemaSetName,
        final Map<String, String> yangResourcesNameToContentMap) {
        cpsValidator.validateNameCharacters(dataspaceName, schemaSetName);
        final YangTextSchemaSourceSet yangTextSchemaSourceSet =
            timedYangTextSchemaSourceSetBuilder.getYangTextSchemaSourceSet(yangResourcesNameToContentMap);
        cpsModulePersistenceService.storeSchemaSet(dataspaceName, schemaSetName, yangResourcesNameToContentMap);
        yangTextSchemaSourceSetCache.updateCache(dataspaceName, schemaSetName, yangTextSchemaSourceSet);
    }

    @Override
    public void createSchemaSetFromModules(final String dataspaceName, final String schemaSetName,
        final Map<String, String> newModuleNameToContentMap,
        final Collection<ModuleReference> allModuleReferences) {
        cpsValidator.validateNameCharacters(dataspaceName, schemaSetName);
        cpsModulePersistenceService.storeSchemaSetFromModules(dataspaceName, schemaSetName,
            newModuleNameToContentMap, allModuleReferences);

    }

    @Override
    public SchemaSet getSchemaSet(final String dataspaceName, final String schemaSetName) {
        cpsValidator.validateNameCharacters(dataspaceName, schemaSetName);
        final var yangTextSchemaSourceSet = yangTextSchemaSourceSetCache
            .get(dataspaceName, schemaSetName);
        return SchemaSet.builder().name(schemaSetName).dataspaceName(dataspaceName)
            .moduleReferences(yangTextSchemaSourceSet.getModuleReferences()).build();
    }

    @Override
    public Collection<SchemaSet> getSchemaSets(final String dataspaceName) {
        cpsValidator.validateNameCharacters(dataspaceName);
        final Collection<SchemaSet> schemaSets =
            cpsModulePersistenceService.getSchemaSetsByDataspaceName(dataspaceName);
        schemaSets.forEach(schemaSet -> setModuleReferences(schemaSet, dataspaceName));
        return schemaSets;
    }

    @Override
    @Transactional
    public void deleteSchemaSet(final String dataspaceName, final String schemaSetName,
                                final CascadeDeleteAllowed cascadeDeleteAllowed) {
        cpsValidator.validateNameCharacters(dataspaceName, schemaSetName);
        final Collection<Anchor> anchors = cpsAdminService.getAnchors(dataspaceName, schemaSetName);
        if (!anchors.isEmpty() && isCascadeDeleteProhibited(cascadeDeleteAllowed)) {
            throw new SchemaSetInUseException(dataspaceName, schemaSetName);
        }
        for (final Anchor anchor : anchors) {
            cpsAdminService.deleteAnchor(dataspaceName, anchor.getName());
        }
        cpsModulePersistenceService.deleteSchemaSet(dataspaceName, schemaSetName);
        yangTextSchemaSourceSetCache.removeFromCache(dataspaceName, schemaSetName);
        cpsModulePersistenceService.deleteUnusedYangResourceModules();
    }

    @Override
    @Transactional
    public void deleteSchemaSetsWithCascade(final String dataspaceName, final Collection<String> schemaSetNames) {
        cpsValidator.validateNameCharacters(dataspaceName);
        cpsValidator.validateNameCharacters(schemaSetNames);
        for (final String schemaSetName : schemaSetNames) {
            final Collection<Anchor> anchors = cpsAdminService.getAnchors(dataspaceName, schemaSetName);
            for (final Anchor anchor : anchors) {
                cpsAdminService.deleteAnchor(dataspaceName, anchor.getName());
            }
        }
        cpsModulePersistenceService.deleteUnusedYangResourceModules();
        cpsModulePersistenceService.deleteSchemaSets(dataspaceName, schemaSetNames);
        for (final String schemaSetName : schemaSetNames) {
            yangTextSchemaSourceSetCache.removeFromCache(dataspaceName, schemaSetName);
        }
    }

    @Override
    public Collection<ModuleReference> getYangResourceModuleReferences(final String dataspaceName) {
        cpsValidator.validateNameCharacters(dataspaceName);
        return cpsModulePersistenceService.getYangResourceModuleReferences(dataspaceName);
    }

    @Override
    public Collection<ModuleReference> getYangResourcesModuleReferences(final String dataspaceName,
        final String anchorName) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        return cpsModulePersistenceService.getYangResourceModuleReferences(dataspaceName, anchorName);
    }

    @Override
    public Collection<ModuleDefinition> getModuleDefinitionsByAnchorName(final String dataspaceName,
                                                                         final String anchorName) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        return cpsModulePersistenceService.getYangResourceDefinitions(dataspaceName, anchorName);
    }

    @Override
    public Collection<ModuleReference> identifyNewModuleReferences(
        final Collection<ModuleReference> moduleReferencesToCheck) {
        return cpsModulePersistenceService.identifyNewModuleReferences(moduleReferencesToCheck);
    }

    private boolean isCascadeDeleteProhibited(final CascadeDeleteAllowed cascadeDeleteAllowed) {
        return CascadeDeleteAllowed.CASCADE_DELETE_PROHIBITED == cascadeDeleteAllowed;
    }

    private void setModuleReferences(final SchemaSet schemaSet, final String dataspaceName) {
        final YangTextSchemaSourceSet yangTextSchemaSourceSet = yangTextSchemaSourceSetCache
            .get(dataspaceName, schemaSet.getName());
        schemaSet.setModuleReferences(yangTextSchemaSourceSet.getModuleReferences());
    }
}
