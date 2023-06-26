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
import org.onap.cps.ncmp.api.impl.events.NcmpCloudEventBuilder;
import org.onap.cps.ncmp.api.impl.utils.data.operation.NcmpEventResponseCode;
import org.onap.cps.ncmp.events.async1_0_0.Data;
import org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent;
import org.onap.cps.ncmp.events.async1_0_0.Response;

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
                                                      final Map<String, Map<NcmpEventResponseCode, List<String>>>
                                                              cmHandleIdsPerOperationIdPerResponseCode) {
        final DataOperationEvent dataOperationEvent = new DataOperationEvent();
        final Data data = createPayloadFromDataOperationResponses(cmHandleIdsPerOperationIdPerResponseCode);
        dataOperationEvent.setData(data);
        final Map<String, String> extensions = createDataOperationExtensions(requestId, clientTopic);
        return NcmpCloudEventBuilder.builder().type(DataOperationEvent.class.getName())
                .event(dataOperationEvent).extensions(extensions).setCloudEvent().build();
    }

    private static Data createPayloadFromDataOperationResponses(final Map<String, Map<NcmpEventResponseCode,
            List<String>>> cmHandleIdsPerOperationIdByResponseCode) {
        final Data data = new Data();
        final List<org.onap.cps.ncmp.events.async1_0_0.Response> responses = new ArrayList<>();
        cmHandleIdsPerOperationIdByResponseCode.entrySet().forEach(cmHandleIdsPerOperationIdByResponseCodeEntry ->
                cmHandleIdsPerOperationIdByResponseCodeEntry.getValue().entrySet()
                        .forEach(cmHandleIdsByResponseCodeEntry ->
                                responses.add(createResponseFromDataOperationResponses(
                                        cmHandleIdsPerOperationIdByResponseCodeEntry, cmHandleIdsByResponseCodeEntry)
                                )));
        data.setResponses(responses);
        return data;
    }

    private static Response createResponseFromDataOperationResponses(
            final Map.Entry<String, Map<NcmpEventResponseCode, List<String>>>
                    cmHandleIdsPerOperationIdByResponseCodeEntry,
            final Map.Entry<NcmpEventResponseCode, List<String>> cmHandleIdsByResponseCodeEntry) {
        final Response response = new Response();
        response.setOperationId(cmHandleIdsPerOperationIdByResponseCodeEntry.getKey());
        response.setIds(cmHandleIdsByResponseCodeEntry.getValue());
        response.setStatusCode(cmHandleIdsByResponseCodeEntry.getKey().getStatusCode());
        response.setStatusMessage(cmHandleIdsByResponseCodeEntry.getKey().getStatusMessage());
        return response;
    }

    private static Map<String, String> createDataOperationExtensions(final String requestId, final String clientTopic) {
        final Map<String, String> extensions = new HashMap<>();
        extensions.put("correlationid", requestId);
        extensions.put("destination", clientTopic);
        return extensions;
    }
}
