/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Nordix Foundation
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

package org.onap.cps.api;

import java.util.List;
import java.util.Map;
import org.onap.cps.api.parameters.FetchDescendantsOption;

public interface CpsFacade {

    /**
     * Get (the first) data node for a given dataspace, anchor and xpath
     *
     * @param dataspaceName            the name of the dataspace
     * @param anchorName               the name of the anchor
     * @param xpath                    the xpath
     * @param fetchDescendantsOption   control what level of descendants should be returned
     * @return                         a map represent the datanode and it descendants
     */
    Map<String, Object> getNodeByDataspaceAndAnchor(String dataspaceName,
                                                    String anchorName,
                                                    String xpath,
                                                    FetchDescendantsOption fetchDescendantsOption);

    /**
     * Get data nodes for a given dataspace, anchor and xpath
     *
     * @param dataspaceName            the name of the dataspace
     * @param anchorName               the name of the anchor
     * @param xpath                    the xpath
     * @param fetchDescendantsOption   control what level of descendants should be returned
     * @return                         a map represent the datanodes and their descendants
     */
    List<Map<String, Object>> getNodesByDataspaceAndAnchor(String dataspaceName,
                                                           String anchorName,
                                                           String xpath,
                                                           FetchDescendantsOption fetchDescendantsOption);
}
