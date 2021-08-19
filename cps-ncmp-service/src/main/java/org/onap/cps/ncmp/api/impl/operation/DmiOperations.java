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
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.operation;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class DmiOperations {

    @Getter
    public enum PassThroughEnum {
        OPERATIONAL("/ncmp-datastore:passthrough-operational/"),
        RUNNING("/ncmp-datastore:passthrough-running/");
        private String value;

        PassThroughEnum(final String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }
    }

    private DmiRestClient dmiRestClient;
    private static final String PARENT_CM_HANDLE_URI =
            "/v1/ch/{cmHandle}/data/ds";
    private static final String PASSTHROUGH_OPERATIONAL =
            "/ncmp-datastore:passthrough-operational/";
    private static final String PASSTHROUGH_RUNNING =
            "/ncmp-datastore:passthrough-running/";
    private final int indexCmHandleInUri;

    /**
     * Constructor for {@code DmiOperations}. This method also manipulates url properties.
     *
     * @param dmiRestClient {@code DmiRestClient}
     */
    public DmiOperations(final DmiRestClient dmiRestClient) {
        this.dmiRestClient = dmiRestClient;
        indexCmHandleInUri = PARENT_CM_HANDLE_URI.indexOf("{cmHandle}");
    }

    /**
     * This method fetches the resource data from operational data store for given cm handle
     * identifier on given resource using dmi client.
     *
     * @param dmiBasePath dmi base path
     * @param cmHandle network resource identifier
     * @param resourceId resource identifier
     * @param fieldsQuery fields query
     * @param depthQuery depth query
     * @param acceptParam accept parameter
     * @param jsonBody json body for put operation
     * @return {@code ResponseEntity} response entity
     */
    public ResponseEntity<Object> getResouceDataOperationalFromDmi(final String dmiBasePath,
                                                                   final String cmHandle,
                                                                   final String resourceId,
                                                                   final String fieldsQuery,
                                                                   final Integer depthQuery,
                                                                   final String acceptParam,
                                                                   final String jsonBody) {
        final StringBuilder builder = getDmiResourceDataUrl(dmiBasePath, cmHandle, resourceId,
                fieldsQuery, depthQuery, PassThroughEnum.OPERATIONAL);
        final HttpHeaders httpHeaders = prepareHeader(acceptParam);
        return dmiRestClient.putOperationWithJsonData(builder.toString(), jsonBody, httpHeaders);
    }

    /**
     * This method fetches the resource data from pass-through running data store for given cm handle
     * identifier on given resource using dmi client.
     *
     * @param dmiBasePath dmi base path
     * @param cmHandle network resource identifier
     * @param resourceId resource identifier
     * @param fieldsQuery fields query
     * @param depthQuery depth query
     * @param acceptParam accept parameter
     * @param jsonBody json body for put operation
     * @return {@code ResponseEntity} response entity
     */
    public ResponseEntity<Object> getResouceDataPassThroughRunningFromDmi(final String dmiBasePath,
                                                                   final String cmHandle,
                                                                   final String resourceId,
                                                                   final String fieldsQuery,
                                                                   final Integer depthQuery,
                                                                   final String acceptParam,
                                                                   final String jsonBody) {
        final StringBuilder builder = getDmiResourceDataUrl(dmiBasePath, cmHandle, resourceId,
                fieldsQuery, depthQuery, PassThroughEnum.RUNNING);
        final HttpHeaders httpHeaders = prepareHeader(acceptParam);
        return dmiRestClient.putOperationWithJsonData(builder.toString(), jsonBody, httpHeaders);
    }

    @NotNull
    private StringBuilder getDmiResourceDataUrl(final String dmiBasePath,
                                                final String cmHandle,
                                                final String resourceId,
                                                final String fieldsQuery,
                                                final Integer depthQuery,
                                       final PassThroughEnum passThrough) {
        final StringBuilder builder =  new StringBuilder(PARENT_CM_HANDLE_URI);
        builder.replace(indexCmHandleInUri,
                indexCmHandleInUri + "{cmHandle}".length(), cmHandle);
        builder.append(passThrough.getValue());
        builder.insert(builder.length(), resourceId);
        appendFieldsAndDepth(fieldsQuery, depthQuery, builder);
        builder.insert(0, dmiBasePath);
        return builder;
    }

    private void appendFieldsAndDepth(final String fieldsQuery, final Integer depthQuery, final StringBuilder builder) {
        final boolean doesFieldExists = (fieldsQuery != null && !fieldsQuery.isEmpty());
        if (doesFieldExists) {
            builder.append("?").append("fields=").append(fieldsQuery);
        }
        if (depthQuery != null) {
            if (!doesFieldExists) {
                builder.append("?");
            } else {
                builder.append("&");
            }
            builder.append("depth=").append(depthQuery);
        }
    }

    private HttpHeaders prepareHeader(final String acceptParam) {
        final HttpHeaders httpHeaders = new HttpHeaders();
        if (acceptParam != null && !acceptParam.isEmpty()) {
            httpHeaders.set(HttpHeaders.ACCEPT, acceptParam);
        }
        return httpHeaders;
    }
}
