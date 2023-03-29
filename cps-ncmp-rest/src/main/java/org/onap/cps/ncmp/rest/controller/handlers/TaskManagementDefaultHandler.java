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

package org.onap.cps.ncmp.rest.controller.handlers;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.onap.cps.spi.FetchDescendantsOption;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public interface TaskManagementDefaultHandler {

    String NO_REQUEST_ID = null;
    String NO_TOPIC = null;

    default Supplier<Object> getTaskSupplier(final String cmHandleId, final String resourceIdentifier,
                                             final String optionsParamInQuery, final String topicParamInQuery,
                                             final String requestId, final Boolean includeDescendant) {
        return Optional::empty;

    }

    default Supplier<Object> getTaskSupplier(final List<String> cmHandleIds, final String resourceIdentifier,
                                             final String optionsParamInQuery, final String topicParamInQuery,
                                             final String requestId, final Boolean includeDescendant) {
        return Optional::empty;
    }

    default ResponseEntity<Object> executeRequest(final String cmHandleId, final String resourceIdentifier,
                                                  final String optionsParamInQuery, final String topicParamInQuery,
                                                  final Boolean includeDescendants) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    default ResponseEntity<Object> executeRequest(final List<String> cmHandleIds, final String resourceIdentifier,
                                                  final String optionsParamInQuery, final String topicParamInQuery,
                                                  final Boolean includeDescendants) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    static FetchDescendantsOption getFetchDescendantsOption(final Boolean includeDescendant) {
        return Boolean.TRUE.equals(includeDescendant) ? FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
                : FetchDescendantsOption.OMIT_DESCENDANTS;
    }
}
