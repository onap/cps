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
import org.onap.cps.ncmp.rest.api.NetworkCmProxyApi;
import org.onap.cps.ncmp.rest.model.CmHandleQueryParameters;
import org.onap.cps.ncmp.rest.model.RestModuleDefinition;
import org.onap.cps.ncmp.rest.model.RestModuleReference;
import org.onap.cps.ncmp.rest.model.RestOutputCmHandle;
import org.onap.cps.ncmp.rest.model.RestOutputCmHandleCompositeState;
import org.onap.cps.ncmp.rest.model.RestOutputCmHandlePublicProperties;
import org.onap.cps.spi.FetchDescendantsOption;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public interface NcmpRequestHandler extends NetworkCmProxyApi {

    String NO_REQUEST_ID = null;
    String NO_TOPIC = null;
    String ASYNC_REQUEST_ID = "requestId";

    default Supplier<Object> getTaskSupplier(final String cmHandle, final String resourceIdentifier,
                                             final String optionsParamInQuery, final String topicParamInQuery,
                                             final String requestId, final Boolean includeDescendant) {
        return Optional::empty;

    }

    default Supplier<Object> getTaskSupplier(final List<String> cmHandleIds, final String resourceIdentifier,
                                             final String optionsParamInQuery, final String topicParamInQuery,
                                             final String requestId, final Boolean includeDescendant) {
        return Optional::empty;
    }

    static FetchDescendantsOption getFetchDescendantsOption(final Boolean includeDescendant) {
        return Boolean.TRUE.equals(includeDescendant) ? FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
                : FetchDescendantsOption.OMIT_DESCENDANTS;
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

    @Override
    default ResponseEntity<Object> getResourceDataForCmHandle(final String datastoreName, final String cmHandle,
                                                              final String resourceIdentifier,
                                                              final String optionsParamInQuery,
                                                              final String topicParamInQuery,
                                                              final Boolean includeDescendants) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    default ResponseEntity<List<RestOutputCmHandle>> searchCmHandles(
            final CmHandleQueryParameters cmHandleQueryParameters) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    default ResponseEntity<Object> getResourceDataForCmHandles(final String resourceIdentifier,
                                                               final String datastoreName,
                                                               final Object requestBody,
                                                               final String optionsParamInQuery,
                                                               final String topicParamInQuery,
                                                               final Boolean includeDescendants) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    default ResponseEntity<Void> createResourceDataRunningForCmHandle(final String datastoreName,
                                                                      final String resourceIdentifier,
                                                                      final String cmHandleId,
                                                                      final Object requestBody,
                                                                      final String contentType) {
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    default ResponseEntity<Void> deleteResourceDataRunningForCmHandle(final String datastoreName,
                                                                      final String cmHandleId,
                                                                      final String resourceIdentifier,
                                                                      final String contentType) {
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    default ResponseEntity<Object> setDataSyncEnabledFlagForCmHandle(final String cmHandleId,
                                                                     final Boolean dataSyncEnabled) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    default ResponseEntity<List<String>> searchCmHandleIds(final CmHandleQueryParameters cmHandleQueryParameters) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    default ResponseEntity<RestOutputCmHandlePublicProperties> getCmHandlePublicPropertiesByCmHandleId(
            final String cmHandleId) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    default ResponseEntity<RestOutputCmHandleCompositeState> getCmHandleStateByCmHandleId(final String cmHandle) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    default ResponseEntity<List<RestModuleDefinition>> getModuleDefinitionsByCmHandleId(final String cmHandle) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    default ResponseEntity<List<RestModuleReference>> getModuleReferencesByCmHandle(final String cmHandleId) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    default ResponseEntity<Object> patchResourceDataRunningForCmHandle(final String datastoreName,
                                                                       final String resourceIdentifier,
                                                                       final String cmHandleId,
                                                                       final Object requestBody,
                                                                       final String contentType) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    default ResponseEntity<Object> queryResourceDataForCmHandle(final String datastoreName,
                                                                final String cmHandle,
                                                                final String cpsPath,
                                                                final String optionsParamInQuery,
                                                                final String topicParamInQuery,
                                                                final Boolean includeDescendants) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    default ResponseEntity<RestOutputCmHandle> retrieveCmHandleDetailsById(final String cmHandleId) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    default ResponseEntity<Object> updateResourceDataRunningForCmHandle(final String datastoreName,
                                                                        final String resourceIdentifier,
                                                                        final String cmHandleId,
                                                                        final Object requestBody,
                                                                        final String contentType) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

}
