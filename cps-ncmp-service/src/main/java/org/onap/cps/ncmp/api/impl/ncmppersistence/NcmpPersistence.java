/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2023 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.ncmppersistence;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;

/**
 * DmiRegistryConstants class to be strictly used for DMI Related constants only.
 */
public interface NcmpPersistence {

    String METHOD_NOT_SUPPORTED = "method is not yet supported";

    @Slf4j
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    class Slf4jLog {
        private static void log() {
            log.info(METHOD_NOT_SUPPORTED);
        }
    }

    String NCMP_DATASPACE_NAME = "NCMP-Admin";
    String NCMP_DMI_REGISTRY_ANCHOR = "ncmp-dmi-registry";
    String NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME = "NFP-Operational";
    String NCMP_DMI_REGISTRY_PARENT = "/dmi-registry";
    OffsetDateTime NO_TIMESTAMP = null;

    /**
     * Method to delete a list or a list element.
     *
     * @param listElementXpath list element xPath
     */
    default void deleteListOrListElement(String listElementXpath) {
        Slf4jLog.log();
    }

    /**
     * Method to delete a schema set.
     *
     * @param schemaSetName schema set name
     */
    default void deleteSchemaSetWithCascade(String schemaSetName) {
        Slf4jLog.log();
    }

    /**
     * Method to delete multiple schema sets.
     *
     * @param schemaSetNames schema set names
     */
    default void deleteSchemaSetsWithCascade(Collection<String> schemaSetNames) {
        Slf4jLog.log();
    }

    /**
     * Get data node via xpath.
     *
     * @param xpath xpath
     * @return data node
     */
    default Collection<DataNode> getDataNode(String xpath) {
        Slf4jLog.log();
        return Collections.emptyList();
    }

    /**
     * Get data node via xpath.
     *
     * @param xpath                  xpath
     * @param fetchDescendantsOption fetch descendants option
     * @return data node
     */
    default Collection<DataNode> getDataNode(String xpath, FetchDescendantsOption fetchDescendantsOption) {
        Slf4jLog.log();
        return Collections.emptyList();
    }

    /**
     * Get collection of data nodes via xpaths.
     *
     * @param xpaths collection of xpaths
     * @return collection of data nodes
     */
    default Collection<DataNode> getDataNodes(Collection<String> xpaths) {
        Slf4jLog.log();
        return Collections.emptyList();
    }

    /**
     * Get collection of data nodes via xpaths.
     *
     * @param xpaths                 collection of xpaths
     * @param fetchDescendantsOption fetch descendants option
     * @return collection of data nodes
     */
    default Collection<DataNode> getDataNodes(Collection<String> xpaths,
                                              FetchDescendantsOption fetchDescendantsOption) {
        Slf4jLog.log();
        return Collections.emptyList();
    }

    /**
     * Replaces list content by removing all existing elements and inserting the given new elements as data nodes.
     *
     * @param parentNodeXpath parent node xpath
     * @param dataNodes       datanodes representing the updated data
     */
    default void replaceListContent(String parentNodeXpath, Collection<DataNode> dataNodes) {
        Slf4jLog.log();
    }

    /**
     * Deletes data node.
     *
     * @param dataNodeXpath data node xpath
     */
    default void deleteDataNode(String dataNodeXpath) {
        Slf4jLog.log();
    }

    /**
     * Deletes multiple data nodes.
     *
     * @param dataNodeXpaths data node xpaths
     */
    default void deleteDataNodes(Collection<String> dataNodeXpaths) {
        Slf4jLog.log();
    }
}
