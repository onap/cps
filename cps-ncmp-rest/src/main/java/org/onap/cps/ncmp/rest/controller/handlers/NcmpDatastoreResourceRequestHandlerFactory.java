/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.ncmp.rest.controller.handlers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NcmpDatastoreResourceRequestHandlerFactory {
    private final NcmpCachedResourceRequestHandler ncmpCachedResourceRequestHandler;
    private final NcmpPassthroughResourceRequestHandler ncmpPassthroughResourceRequestHandler;

    /**
     * Gets ncmp datastore handler.
     *
     * @param datastoreType the datastore type
     * @return the ncmp datastore handler
     */
    public NcmpDatastoreRequestHandler getNcmpResourceRequestHandler(final DatastoreType datastoreType) {

        switch (datastoreType) {
            case OPERATIONAL:
                ncmpCachedResourceRequestHandler.setDataStoreName(datastoreType.getDatastoreName());
                return ncmpCachedResourceRequestHandler;
            case PASSTHROUGH_RUNNING:
            case PASSTHROUGH_OPERATIONAL:
            default:
                ncmpPassthroughResourceRequestHandler.setDataStoreName(datastoreType.getDatastoreName());
                return ncmpPassthroughResourceRequestHandler;
        }
    }
}
