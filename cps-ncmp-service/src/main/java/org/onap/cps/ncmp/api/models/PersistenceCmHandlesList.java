/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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

package org.onap.cps.ncmp.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class PersistenceCmHandlesList {

    @JsonProperty("cm-handles")
    private List<PersistenceCmHandle> persistenceCmHandles;

    /**
     * Add a persistenceCmHandle.
     *
     * @param persistenceCmHandle the persistenceCmHandle to add
     */
    public void add(final PersistenceCmHandle persistenceCmHandle) {
        if (persistenceCmHandles == null) {
            persistenceCmHandles = new ArrayList<>();
        }
        persistenceCmHandles.add(persistenceCmHandle);
    }

    public List<PersistenceCmHandle> getPersistenceCmHandles() {
        return persistenceCmHandles;
    }
}
