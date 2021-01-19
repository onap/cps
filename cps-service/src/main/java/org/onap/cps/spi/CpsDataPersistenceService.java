/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

package org.onap.cps.spi;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.onap.cps.spi.model.DataNode;

/*
    Data Store interface that is responsible for handling yang data.
    Please follow guidelines in https://gerrit.nordix.org/#/c/onap/ccsdk/features/+/6698/19/cps/interface-proposal/src/main/java/cps/javadoc/spi/DataStoreService.java
    when adding methods.
 */
public interface CpsDataPersistenceService {

    /**
     * Store a datanode.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     * @param dataNode      data node
     */
    void storeDataNode(@NonNull String dataspaceName, @NonNull String anchorName,
        @NonNull DataNode dataNode);

    /**
     * Add another child to a FragmentEntity that has already at least one child.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     * @param dataNode      dataNode
     */
    void addChildDataNode(final String dataspaceName, final String anchorName, final String parentXpath,
        final DataNode dataNode);
}
