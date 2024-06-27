/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.data.models;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.onap.cps.ncmp.api.data.exceptions.InvalidDatastoreException;

@Getter
public enum DatastoreType {

    OPERATIONAL("ncmp-datastore:operational"),
    PASSTHROUGH_RUNNING("ncmp-datastore:passthrough-running"),
    PASSTHROUGH_OPERATIONAL("ncmp-datastore:passthrough-operational");

    DatastoreType(final String datastoreName) {
        this.datastoreName = datastoreName;
    }

    private final String datastoreName;
    private static final Map<String, DatastoreType> datastoreNameToDatastoreType = new HashMap<>();

    static {
        Arrays.stream(DatastoreType.values()).forEach(
                type -> datastoreNameToDatastoreType.put(type.getDatastoreName(), type));
    }

    /**
     * From datastore name get datastore type.
     *
     * @param datastoreName the datastore name
     * @return the datastore type
     */
    public static DatastoreType fromDatastoreName(final String datastoreName) {

        final DatastoreType datastoreType = datastoreNameToDatastoreType.get(datastoreName);

        if (null == datastoreType) {
            throw new InvalidDatastoreException(datastoreName + " is an invalid datastore name");
        }

        return datastoreType;
    }

}

