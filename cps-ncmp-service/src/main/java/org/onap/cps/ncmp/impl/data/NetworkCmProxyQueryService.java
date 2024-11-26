/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
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

package org.onap.cps.ncmp.impl.data;

import java.util.Collection;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.parameters.FetchDescendantsOption;

/*
 * Datastore interface for handling cached CPS data query requests.
 */
public interface NetworkCmProxyQueryService {

    /**
     * Fetches operational resource data based on the provided CM handle identifier and CPS path.
     * This method retrieves data nodes from the specified path within the context of a given CM handle.
     * It supports options for fetching descendant nodes.
     *
     * @param cmHandleId             The CM handle identifier, which uniquely identifies the CM handle.
     *                               This parameter must not be null.
     * @param cpsPath                The CPS (Control Plane Service) path specifying the location of the
     *                               resource data within the CM handle. This parameter must not be null.
     * @param fetchDescendantsOption The option specifying whether to fetch descendant nodes along with the specified
     *                               resource data.
     * @return {@code Collection<DataNode>} A collection of DataNode objects representing the resource data
     *     retrieved from the specified path. The collection may include descendant nodes based on the
     *     fetchDescendantsOption.
     */
    Collection<DataNode> queryResourceDataOperational(String cmHandleId,
                                                      String cpsPath,
                                                      FetchDescendantsOption fetchDescendantsOption);
}
