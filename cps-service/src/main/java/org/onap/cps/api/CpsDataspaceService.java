/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2023 Nordix Foundation
 *  Modifications Copyright (C) 2020-2022 Bell Canada.
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2022 TechMahindra Ltd.
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

import java.util.Collection;
import org.onap.cps.api.exceptions.AlreadyDefinedException;
import org.onap.cps.api.model.Dataspace;

/**
 * CPS Admin Service.
 */
public interface CpsDataspaceService {

    /**
     * Create dataspace.
     *
     * @param dataspaceName dataspace name
     * @throws AlreadyDefinedException if dataspace with same name already exists
     */
    void createDataspace(String dataspaceName);

    /**
     * Delete dataspace.
     *
     * @param dataspaceName the name of the dataspace to delete
     */
    void deleteDataspace(String dataspaceName);

    /**
     * Get dataspace by given dataspace name.
     *
     * @param dataspaceName dataspace name
     * @return a dataspace
     */
    Dataspace getDataspace(String dataspaceName);

    /**
     * Get All Dataspaces.
     *
     *
     * @return a collection of dataspaces
     */
    Collection<Dataspace> getAllDataspaces();

}
