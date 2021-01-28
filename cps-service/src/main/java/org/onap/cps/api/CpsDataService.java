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

package org.onap.cps.api;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.onap.cps.spi.exceptions.DataValidationException;

/*
 * Datastore interface for handling CPS data.
 */
public interface CpsDataService {
    /**
     * Persists data for the given anchor and dataspace.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     * @param jsonData      json data
     * @throws DataValidationException when json data is invalid
     */
    void saveData(@NonNull String dataspaceName, @NonNull String anchorName, @NonNull String jsonData);
}
