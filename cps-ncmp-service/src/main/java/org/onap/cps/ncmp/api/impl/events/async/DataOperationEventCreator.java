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

package org.onap.cps.ncmp.api.impl.events.async;

import io.cloudevents.CloudEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.NcmpEventResponseCode;
import org.onap.cps.ncmp.api.impl.events.NcmpCloudEventBuilder;
import org.onap.cps.ncmp.events.async1_0_0.Data;
import org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent;
import org.onap.cps.ncmp.events.async1_0_0.Response;
import org.springframework.util.MultiValueMap;

@Slf4j
public class DataOperationEventCreator {

    /**
     * Creates data operation event.
     *
     * @param clientTopic                              topic the client wants to use for responses
     * @param requestId                                unique identifier per request
     * @param cmHandleIdsPerOperationIdPerResponseCode map of cm handles per operation response per response code
     * @return Cloud Event
     */
    public static CloudEvent createDataOperationEvent(final String clientTopic,
                                                      final String requestId,
                                                      final MultiValueMap<String, Map<NcmpEventResponseCode,
                                                              List<String>>> cmHandleIdsPerOperationIdPerResponseCode) {
        final DataOperationEvent dataOperationEvent = new DataOperationEvent();
        final Data data = createPayloadFromDataOperationResponses(cmHandleIdsPerOperationIdPerResponseCode);
        dataOperationEvent.setData(data);
        final Map<String, String> extensions = createDataOperationExtensions(requestId, clientTopic);
        return NcmpCloudEventBuilder.builder().type(DataOperationEvent.class.getName())
                .event(dataOperationEvent).extensions(extensions).setCloudEvent().build();
    }

    private static Data createPayloadFromDataOperationResponses(final MultiValueMap<String, Map<NcmpEventResponseCode,
            List<String>>> cmHandleIdsPerOperationIdPerResponseCode) {
        final Data data = new Data();
        final List<org.onap.cps.ncmp.events.async1_0_0.Response> responses = new ArrayList<>();
        cmHandleIdsPerOperationIdPerResponseCode.entrySet().forEach(cmHandleIdsPerOperationIdPerResponseCodeEntries ->
                cmHandleIdsPerOperationIdPerResponseCodeEntries.getValue().forEach(cmHandleIdsPerResponseCodeEntries ->
                        responses.addAll(createResponseFromDataOperationResponses(
                                cmHandleIdsPerOperationIdPerResponseCodeEntries.getKey(),
                                cmHandleIdsPerResponseCodeEntries)
                        )));
        data.setResponses(responses);
        return data;
    }

    private static List<Response> createResponseFromDataOperationResponses(
            final String operationId,
            final Map<NcmpEventResponseCode, List<String>> cmHandleIdsPerResponseCodeEntries) {
        final List<org.onap.cps.ncmp.events.async1_0_0.Response> responses = new ArrayList<>();
        cmHandleIdsPerResponseCodeEntries.entrySet()
                .forEach(cmHandleIdsPerResponseCodeEntry -> {
                    final Response response = new Response();
                    response.setOperationId(operationId);
                    response.setStatusCode(cmHandleIdsPerResponseCodeEntry.getKey().getStatusCode());
                    response.setStatusMessage(cmHandleIdsPerResponseCodeEntry.getKey().getStatusMessage());
                    response.setIds(cmHandleIdsPerResponseCodeEntry.getValue());
                    responses.add(response);
                });
        return responses;
    }

    private static Map<String, String> createDataOperationExtensions(final String requestId, final String clientTopic) {
        final Map<String, String> extensions = new HashMap<>();
        extensions.put("correlationid", requestId);
        extensions.put("destination", clientTopic);
        return extensions;
    }
}
