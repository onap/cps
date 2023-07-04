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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.NcmpEventResponseCode;
import org.onap.cps.ncmp.api.impl.utils.EventDateTimeFormatter;
import org.onap.cps.ncmp.dmi.rest.stub.model.data.operational.CmHandle;
import org.onap.cps.ncmp.dmi.rest.stub.model.data.operational.DataOperationRequest;
import org.onap.cps.ncmp.dmi.rest.stub.model.data.operational.ResourceDataOperationRequests;
import org.onap.cps.ncmp.dmi.rest.stub.model.module.reference.ModuleSet;
import org.onap.cps.ncmp.dmi.rest.stub.model.module.resource.YangResources;
import org.onap.cps.ncmp.dmi.rest.stub.utils.ResourceFileReaderUtil;
import org.onap.cps.ncmp.events.async1_0_0.Data;
import org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent;
import org.onap.cps.ncmp.events.async1_0_0.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
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
     * @param cmHandle                The identifier for a network function, network element, subnetwork,
     *                                or any other cm object by managed Network CM Proxy
     * @param moduleReferencesRequest module references request body
     * @return (@ code ResponseEntity) response entity
     */
    @PostMapping("/v1/ch/{cmHandle}/modules")
    public ResponseEntity<ModuleSet> getModuleReferences(@PathVariable final String cmHandle,
                                                         @RequestBody final Object moduleReferencesRequest) {
        final String moduleResponseContent = ResourceFileReaderUtil
                .getResourceFileContent(applicationContext.getResource(
                        ResourceLoader.CLASSPATH_URL_PREFIX + "module/moduleResponse.json"));
        ModuleSet moduleSet = new ModuleSet();
        try {
            moduleSet = objectMapper.readValue(moduleResponseContent, ModuleSet.class);
        } catch (final JsonProcessingException jsonProcessingException) {
            log.info("Unable to parse set of module. cause : {}", jsonProcessingException.getMessage());
        }
        log.info("getModuleReferences >> {}", moduleSet);
        return ResponseEntity.ok(moduleSet);
    }

    /**
     * Get all module resources for given cm handle.
     *
     * @param cmHandle                   The identifier for a network function, network element, subnetwork,
     *                                   or any other cm object by managed Network CM Proxy
     * @param moduleResourcesReadRequest module resources read request body
     * @return (@ code ResponseEntity) response entity
     */
    @PostMapping("/v1/ch/{cmHandle}/moduleResources")
    public ResponseEntity<YangResources> retrieveModuleResources(
            @PathVariable final String cmHandle,
            @RequestBody final Object moduleResourcesReadRequest) {
        final String moduleResourcesResponseContent = ResourceFileReaderUtil
                .getResourceFileContent(applicationContext.getResource(
                        ResourceLoader.CLASSPATH_URL_PREFIX + "module/moduleResourcesResponse.json"));
        YangResources yangResources = new YangResources();
        try {
            yangResources = objectMapper.readValue(moduleResourcesResponseContent, YangResources.class);
        } catch (final JsonProcessingException jsonProcessingException) {
            log.info("Unable to parse yang resources. cause : {}", jsonProcessingException.getMessage());
        }
        log.info("retrieveModuleResources >> {}", yangResources);
        return new ResponseEntity<>(yangResources, HttpStatus.OK);
    }

    /**
     * This method is not implemented for ONAP DMI plugin.
     *
     * @param topic                         client given topic name
     * @param requestId                     requestId generated by NCMP as an ack for client
     * @param resourceDataOperationRequests list of operation details
     * @return (@ code ResponseEntity) response entity
     */
    @PostMapping("/v1/data")
    public ResponseEntity<Void> getResourceDataForCmHandleDataOperation(@RequestParam(value = "topic")
                                                                            final String topic,
                                                                        @RequestParam(value = "requestId")
                                                                        final String requestId,
                                                                        @RequestBody final ResourceDataOperationRequests
                                                                                    resourceDataOperationRequests) {
        log.info("Request received from the NCMP to DMI Plugin");
        resourceDataOperationRequests.forEach(resourceDataOperationRequest -> {
            CloudEvent cloudEvent = null;
            try {
                final DataOperationEvent dataOperationEvent = getDataOperationEvent(resourceDataOperationRequest);
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
            cloudEventKafkaTemplate.send(ncmpAsyncM2mTopic, UUID.randomUUID().toString(), cloudEvent);
        });
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    private DataOperationEvent getDataOperationEvent(final DataOperationRequest dataOperationRequest) {
        final Response response = new Response();
        response.setOperationId(dataOperationRequest.getOperationId());
        response.setStatusCode(NcmpEventResponseCode.SUCCESS.getStatusCode());
        response.setStatusMessage(NcmpEventResponseCode.SUCCESS.getStatusMessage());
        response.setIds(dataOperationRequest.getCmHandles().stream().map(CmHandle::getId).collect(Collectors.toList()));
        try {
            response.setResult(objectMapper.writeValueAsString(Map.of("some-key", "some-value")));
        } catch (final JsonProcessingException jsonProcessingException) {
            log.error("Unable to parse event result as json string. cause : {}", jsonProcessingException.getMessage());
        }
        final List<Response> responseList = new ArrayList<>();
        responseList.add(response);
        final Data data = new Data();
        data.setResponses(responseList);
        final DataOperationEvent dataOperationEvent = new DataOperationEvent();
        dataOperationEvent.setData(data);
        return dataOperationEvent;
    }
}
