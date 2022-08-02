package org.onap.cps.ncmp.rest.controller;
/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

import org.springframework.http.ResponseEntity;

public interface NcmpDataStoreHandler {
    /**
     * Handle response entity.
     *
     * @param cmHandle            the cm handle
     * @param resourceIdentifier  the resource identifier
     * @param optionsParamInQuery the options param in query
     * @param topicParamInQuery   the topic param in query
     * @param includeDescendants  the include descendants
     * @return the response entity
     */
    ResponseEntity<Object> handle(String cmHandle,
                                  String resourceIdentifier,
                                  String optionsParamInQuery,
                                  String topicParamInQuery,
                                  Boolean includeDescendants);
}
