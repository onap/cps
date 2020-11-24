/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.api.javadoc.api;


import org.onap.cps.api.javadoc.model.Module;

/**
 * Model interface for handling CPS modules.
 */

public interface ModuleService {

    /**
     * Create a module set.
     *
     * @param dataspaceName dataspace name
     * @param moduleSetName module set name
     */
    void createModuleSet(String dataspaceName, String moduleSetName);

    /**
     * Add a module to a module set for the given dataspace.
     *
     * @param module        module
     * @param moduleSetName module set name
     * @param dataspaceName dataspace name
     */
    void addModuleToModuleSet(String dataspaceName, Module module, String moduleSetName);

    /**
     * Delete a module set.
     *
     * @param namespace     namespace
     * @param revision      revision
     * @param dataspaceName dataspace name
     */
    void deleteModule(String dataspaceName, String namespace, String revision);


    /**
     * Read all modules in the store for the given dataspace.
     *
     * @param dataspaceName dataspace name
     * @return module dataspaces and revisions as a comma separated list
     */
    Module getModulesForDataspace(String dataspaceName);

    /**
     * Read all module revisions in the store for the given dataspace and namespace.
     *
     * @param dataspaceName dataspace name
     * @param namespace     namespace
     * @return revisions as a comma separated list
     */
    Module getModuleRevisions(String dataspaceName, String namespace);
}