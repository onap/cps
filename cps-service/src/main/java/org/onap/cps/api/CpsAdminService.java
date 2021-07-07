/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  Modifications Copyright (C) 2020 Bell Canada.
 *  Modifications Copyright (C) 2021 Pantheon.tech
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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.exceptions.CpsException;
import org.onap.cps.spi.model.Anchor;

/**
 * CPS Admin Service.
 */
public interface CpsAdminService {

    /**
     * Create dataspace.
     *
     * @param dataspaceName dataspace name
     * @throws AlreadyDefinedException if dataspace with same name already exists
     */
    void createDataspace(@NonNull String dataspaceName);

    /**
     * Create an Anchor.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schema set name
     * @param anchorName    anchor name
     * @throws CpsException if input data is invalid.
     */
    void createAnchor(@NonNull String dataspaceName, @NonNull String schemaSetName, @NonNull String anchorName);

    /**
     * Read all anchors in the given dataspace.
     *
     * @param dataspaceName dataspace name
     * @return a collection of anchors
     */
    @NonNull
    Collection<Anchor> getAnchors(@NonNull String dataspaceName);

    /**
     * Get an anchor in the given dataspace using the anchor name.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     * @return an anchor
     */
    @NonNull
    Anchor getAnchor(@NonNull String dataspaceName, @NonNull String anchorName);

    /**
     * Delete anchor by name in given dataspace.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     */
    void deleteAnchor(@NonNull String dataspaceName, @NonNull String anchorName);
}
