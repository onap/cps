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
import java.util.Collection;
import java.util.List;
import lombok.Getter;

@Getter
public class PersistenceCmHandlesList {

    @JsonProperty("cm-handles")
    private final List<PersistenceCmHandle> persistenceCmHandles = new ArrayList<>();

    /**
     * Create a PersistenceCmHandleList given all service names and a collection of cmHandles.
     * @param dmiServiceName the dmi service name
     * @param dmiDataServiceName the dmi data service name
     * @param dmiModelServiceName the dmi model service name
     * @param cmHandles cm handles
     * @return instance of PersistenceCmHandleList
     */
    public static PersistenceCmHandlesList toPersistenceCmHandlesList(final String dmiServiceName,
                                                                      final String dmiDataServiceName,
                                                                      final String dmiModelServiceName,
                                                                      final Collection<CmHandle> cmHandles) {
        final PersistenceCmHandlesList persistenceCmHandlesList = new PersistenceCmHandlesList();
        for (final CmHandle cmHandle : cmHandles) {
            final PersistenceCmHandle persistenceCmHandle =
                PersistenceCmHandle.toPersistenceCmHandle(
                    dmiServiceName,
                    dmiDataServiceName,
                    dmiModelServiceName,
                    cmHandle);
            persistenceCmHandlesList.add(persistenceCmHandle);
        }
        return persistenceCmHandlesList;
    }

    /**
     * Add a persistenceCmHandle.
     *
     * @param persistenceCmHandle the persistenceCmHandle to add
     */
    public void add(final PersistenceCmHandle persistenceCmHandle) {
        persistenceCmHandles.add(persistenceCmHandle);
    }
}
