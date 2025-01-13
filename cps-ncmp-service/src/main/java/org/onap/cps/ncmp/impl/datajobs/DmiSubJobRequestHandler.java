/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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

package org.onap.cps.ncmp.impl.datajobs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.data.models.OperationType;
import org.onap.cps.ncmp.api.datajobs.models.DataJobMetadata;
import org.onap.cps.ncmp.api.datajobs.models.DmiWriteOperation;
import org.onap.cps.ncmp.api.datajobs.models.ProducerKey;
import org.onap.cps.ncmp.api.datajobs.models.SubJobWriteRequest;
import org.onap.cps.ncmp.api.datajobs.models.SubJobWriteResponse;
import org.onap.cps.ncmp.impl.dmi.DmiProperties;
import org.onap.cps.ncmp.impl.dmi.DmiRestClient;
import org.onap.cps.ncmp.impl.models.RequiredDmiService;
import org.onap.cps.ncmp.impl.utils.http.RestServiceUrlTemplateBuilder;
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DmiSubJobRequestHandler {

    private final DmiRestClient dmiRestClient;
    private final DmiProperties dmiProperties;
    private final JsonObjectMapper jsonObjectMapper;

    /**
     * Sends sub-job write requests to the DMI Plugin.
     *
     * @param authorization                    the authorization header from the REST request
     * @param dataJobId                        data job identifier
     * @param dataJobMetadata                  the data job's metadata
     * @param dmiWriteOperationsPerProducerKey a collection of write requests per producer key
     * @return a list of sub-job write responses
     */
    public List<SubJobWriteResponse> sendRequestsToDmi(final String authorization,
                                                       final String dataJobId,
                                                       final DataJobMetadata dataJobMetadata,
                                  final Map<ProducerKey, List<DmiWriteOperation>> dmiWriteOperationsPerProducerKey) {
        final List<SubJobWriteResponse> subJobWriteResponses = new ArrayList<>(dmiWriteOperationsPerProducerKey.size());
        dmiWriteOperationsPerProducerKey.forEach((producerKey, dmi3ggpWriteOperations) -> {
            final SubJobWriteRequest subJobWriteRequest = new SubJobWriteRequest(dataJobMetadata.destination(),
                    dataJobMetadata.dataAcceptType(),
                    dataJobMetadata.dataContentType(),
                    producerKey.dataProducerIdentifier(),
                    dataJobId,
                    dmi3ggpWriteOperations);

            final UrlTemplateParameters urlTemplateParameters = getUrlTemplateParameters(dataJobMetadata.destination(),
                    producerKey);
            final ResponseEntity<Object> responseEntity = dmiRestClient.synchronousPostOperationWithJsonData(
                    RequiredDmiService.DATA,
                    urlTemplateParameters,
                    jsonObjectMapper.asJsonString(subJobWriteRequest),
                    OperationType.CREATE,
                    authorization);
            final Map<String, String> subJobId = jsonObjectMapper
                    .convertToValueType(responseEntity.getBody(),
                            Map.class);
            final SubJobWriteResponse subJobWriteResponse = new SubJobWriteResponse(subJobId.get("subJobId"),
                    producerKey.dmiServiceName(), producerKey.dataProducerIdentifier());
            log.debug("Sub job write response: {}", subJobWriteResponse);
            subJobWriteResponses.add(subJobWriteResponse);
        });
        return subJobWriteResponses;
    }

    private UrlTemplateParameters getUrlTemplateParameters(final String destination, final ProducerKey producerKey) {
        return RestServiceUrlTemplateBuilder.newInstance()
                .fixedPathSegment("cmwriteJob")
                .queryParameter("destination", destination)
                .createUrlTemplateParameters(producerKey.dmiServiceName(), dmiProperties.getDmiBasePath());
    }
}
