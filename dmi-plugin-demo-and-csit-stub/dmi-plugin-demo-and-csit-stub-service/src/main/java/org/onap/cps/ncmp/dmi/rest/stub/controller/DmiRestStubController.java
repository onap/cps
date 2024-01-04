/*
 * ============LICENSE_START=======================================================
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

package org.onap.cps.ncmp.dmi.rest.stub.controller;

import static org.onap.cps.ncmp.api.NcmpResponseStatus.SUCCESS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.onap.cps.ncmp.api.impl.utils.EventDateTimeFormatter;
import org.onap.cps.ncmp.dmi.rest.stub.model.data.operational.CmHandle;
import org.onap.cps.ncmp.dmi.rest.stub.model.data.operational.DataOperationRequest;
import org.onap.cps.ncmp.dmi.rest.stub.model.data.operational.DmiDataOperationRequest;
import org.onap.cps.ncmp.dmi.rest.stub.utils.ResourceFileReaderUtil;
import org.onap.cps.ncmp.events.async1_0_0.Data;
import org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent;
import org.onap.cps.ncmp.events.async1_0_0.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.api.dmi-stub-base-path}")
@RequiredArgsConstructor
@Slf4j
public class DmiRestStubController {

    private final KafkaTemplate<String, CloudEvent> cloudEventKafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;

    @Value("${app.ncmp.async-m2m.topic}")
    private String ncmpAsyncM2mTopic;

    private String dataOperationEventType = "org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent";

    /**
     * Get all modules for given cm handle.
     *
     * @param cmHandleId              The identifier for a network function, network element, subnetwork,
     *                                or any other cm object by managed Network CM Proxy
     * @param moduleReferencesRequest module references request body
     * @return ResponseEntity response entity having module response as json string.
     */
    @PostMapping("/v1/ch/{cmHandleId}/modules")
    public ResponseEntity<String> getModuleReferences(@PathVariable final String cmHandleId,
                                                      @RequestBody final Object moduleReferencesRequest) {
        final String[] cmHandleIdParts = cmHandleId.split("-");
        final String requestedNodeType = cmHandleIdParts.length > 0 ? cmHandleIdParts[0] : "ietfYang";
        final String moduleResponseContentFilePath = String.format("module/%sModuleResponse.json", requestedNodeType);
        Resource moduleResponseResource = applicationContext.getResource(ResourceLoader.CLASSPATH_URL_PREFIX
                + moduleResponseContentFilePath);
        if (!moduleResponseResource.exists()) {
            moduleResponseResource = applicationContext.getResource(ResourceLoader.CLASSPATH_URL_PREFIX
                    + "module/ietfYangModuleResponse.json");
        }
        final String moduleResponseContent = ResourceFileReaderUtil.getResourceFileContent(moduleResponseResource);
        log.info("cm handle: {} requested for modules", cmHandleId);
        return ResponseEntity.ok(moduleResponseContent);
    }

    /**
     * Retrieves module resources for a given cmHandleId.
     *
     * @param cmHandleId                 The identifier for a network function, network element, subnetwork,
     *                                   or any other cm object by managed Network CM Proxy
     * @param moduleResourcesReadRequest module resources read request body
     * @return ResponseEntity response entity having module resources response as json string.
     */
    @PostMapping("/v1/ch/{cmHandleId}/moduleResources")
    public ResponseEntity<String> retrieveModuleResources(
            @PathVariable final String cmHandleId,
            @RequestBody final Object moduleResourcesReadRequest) {
        final String[] cmHandleIdParts = cmHandleId.split("-");
        final String requestedNodeType = cmHandleIdParts.length > 0 ? cmHandleIdParts[0] : "ietfYang";
        final String moduleResponseContentFilePath
                = String.format("module/%sModuleResourcesResponse.json", requestedNodeType);
        Resource moduleResponseResource = applicationContext.getResource(ResourceLoader.CLASSPATH_URL_PREFIX
                + moduleResponseContentFilePath);
        if (!moduleResponseResource.exists()) {
            moduleResponseResource = applicationContext.getResource(ResourceLoader.CLASSPATH_URL_PREFIX
                    + "module/ietfYangModuleResourcesResponse.json");
        }
        final String moduleResourcesResponseContent
                = ResourceFileReaderUtil.getResourceFileContent(moduleResponseResource);
        log.info("cm handle: {} requested for modules resources", cmHandleId);
        return ResponseEntity.ok(moduleResourcesResponseContent);
    }

    /**
     * This method is not implemented for ONAP DMI plugin.
     *
     * @param topic                   client given topic name
     * @param requestId               requestId generated by NCMP as an ack for client
     * @param dmiDataOperationRequest list of operation details
     * @return (@ code ResponseEntity) response entity
     */
    @PostMapping("/v1/data")
    public ResponseEntity<Void> getResourceDataForCmHandleDataOperation(@RequestParam(value = "topic")
                                                                            final String topic,
                                                                        @RequestParam(value = "requestId")
                                                                        final String requestId,
                                                                        @RequestBody final DmiDataOperationRequest
                                                                                    dmiDataOperationRequest) {
        try {
            log.info("Request received from the NCMP to DMI Plugin: {}",
                    objectMapper.writeValueAsString(dmiDataOperationRequest));
        } catch (final JsonProcessingException jsonProcessingException) {
            log.info("Unable to process dmi data operation request to json string");
        }
        dmiDataOperationRequest.getOperations().forEach(dmiDataOperation -> {
            final DataOperationEvent dataOperationEvent = getDataOperationEvent(dmiDataOperation);
            dmiDataOperation.getCmHandles().forEach(cmHandle -> {
                dataOperationEvent.getData().getResponses().get(0).setIds(List.of(cmHandle.getId()));
                final CloudEvent cloudEvent = buildAndGetCloudEvent(topic, requestId, dataOperationEvent);
                cloudEventKafkaTemplate.send(ncmpAsyncM2mTopic, UUID.randomUUID().toString(), cloudEvent);
            });
        });
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    private CloudEvent buildAndGetCloudEvent(final String topic, final String requestId,
                                             final DataOperationEvent dataOperationEvent) {
        CloudEvent cloudEvent = null;
        try {
            cloudEvent = CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withSource(URI.create("DMI"))
                    .withType(dataOperationEventType)
                    .withDataSchema(URI.create("urn:cps:" + dataOperationEventType + ":1.0.0"))
                    .withTime(EventDateTimeFormatter.toIsoOffsetDateTime(
                            EventDateTimeFormatter.getCurrentIsoFormattedDateTime()))
                    .withData(objectMapper.writeValueAsBytes(dataOperationEvent))
                    .withExtension("destination", topic)
                    .withExtension("correlationid", requestId)
                    .build();
        } catch (final JsonProcessingException jsonProcessingException) {
            log.error("Unable to parse event into bytes. cause : {}", jsonProcessingException.getMessage());
        }
        return cloudEvent;
    }

    private DataOperationEvent getDataOperationEvent(final DataOperationRequest dataOperationRequest) {
        final Response response = new Response();
        response.setOperationId(dataOperationRequest.getOperationId());
        response.setStatusCode(SUCCESS.getCode());
        response.setStatusMessage(SUCCESS.getMessage());
        response.setIds(dataOperationRequest.getCmHandles().stream().map(CmHandle::getId).toList());
        response.setResourceIdentifier(dataOperationRequest.getResourceIdentifier());
        response.setOptions(dataOperationRequest.getOptions());
        final String ietfNetworkTopologySample = ResourceFileReaderUtil
                .getResourceFileContent(applicationContext.getResource(
                        ResourceLoader.CLASSPATH_URL_PREFIX
                                + "data/operational/ietf-network-topology-sample-rfc8345.json"));
        final JSONParser jsonParser = new JSONParser();
        try {
            response.setResult(jsonParser.parse(ietfNetworkTopologySample));
        } catch (final ParseException parseException) {
            log.error("Unable to parse event result as json object. cause : {}", parseException.getMessage());
        }
        final List<Response> responseList = new ArrayList<>(1);
        responseList.add(response);
        final Data data = new Data();
        data.setResponses(responseList);
        final DataOperationEvent dataOperationEvent = new DataOperationEvent();
        dataOperationEvent.setData(data);
        return dataOperationEvent;
    }
}
