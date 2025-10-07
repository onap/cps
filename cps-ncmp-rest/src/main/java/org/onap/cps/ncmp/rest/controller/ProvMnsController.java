/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe
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

package org.onap.cps.ncmp.rest.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.data.models.OperationType;
import org.onap.cps.ncmp.api.inventory.models.CmHandleState;
import org.onap.cps.ncmp.impl.data.policyexecutor.PolicyExecutor;
import org.onap.cps.ncmp.impl.dmi.DmiRestClient;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.models.RequiredDmiService;
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher;
import org.onap.cps.ncmp.impl.utils.http.RestServiceUrlTemplateBuilder;
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters;
import org.onap.cps.ncmp.rest.provmns.model.ClassNameIdGetDataNodeSelectorParameter;
import org.onap.cps.ncmp.rest.provmns.model.ConfigurationManagementDeleteInput;
import org.onap.cps.ncmp.rest.provmns.model.Resource;
import org.onap.cps.ncmp.rest.provmns.model.Scope;
import org.onap.cps.ncmp.rest.util.ProvMnsRequestParameters;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("${rest.api.provmns-base-path}")
@RequiredArgsConstructor
public class ProvMnsController implements ProvMnS {

    private final InventoryPersistence inventoryPersistence;
    private final AlternateIdMatcher alternateIdMatcher;
    private final PolicyExecutor policyExecutor;
    private final DmiRestClient dmiRestClient;
    private final JsonObjectMapper jsonObjectMapper;

    /**
     * Replaces a complete single resource or creates it if it does not exist.
     *
     * @param httpServletRequest      URI request including path
     * @param resource                Resource representation of the resource to be created or replaced
     * @return {@code ResponseEntity} The representation of the updated resource is returned in the response
     *                                message body.
     */
    @Override
    public ResponseEntity<Resource> putMoi(final HttpServletRequest httpServletRequest, final Resource resource) {
        final ProvMnsRequestParameters provMnsRequestParameters =
            ProvMnsRequestParameters.toProvMnsRequestParameters(httpServletRequest);
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * Reads one or multiple resources.
     *
     * @param httpServletRequest      URI request including path
     * @param scope                   Extends the set of targeted resources beyond the base
     *                                resource identified with the authority and path component of
     *                                the URI.
     * @param filter                  Reduces the targeted set of resources by applying a filter to
     *                                the scoped set of resource representations. Only resources
     *                                representations for which the filter construct evaluates to
     *                                "true" are targeted.
     * @param attributes              Attributes of the scoped resources to be returned. The
     *                                value is a comma-separated list of attribute names.
     * @param fields                  Attribute fields of the scoped resources to be returned. The
     *                                value is a comma-separated list of JSON pointers to the
     *                                attribute fields.
     * @param dataNodeSelector        dataNodeSelector object
     * @return {@code ResponseEntity} The resources identified in the request for retrieval are returned
     *                                in the response message body.
     */
    @Override
    public ResponseEntity<Resource> getMoi(final HttpServletRequest httpServletRequest, final Scope scope,
                                                   final String filter, final List<String> attributes,
                                                   final List<String> fields,
                                                   final ClassNameIdGetDataNodeSelectorParameter dataNodeSelector) {
        final ProvMnsRequestParameters requestParameters =
            ProvMnsRequestParameters.toProvMnsRequestParameters(httpServletRequest);
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * Patches (Create, Update or Delete) one or multiple resources.
     *
     * @param httpServletRequest      URI request including path
     * @param resource                Resource representation of the resource to be created or replaced
     * @return {@code ResponseEntity} The updated resource representations are returned in the response message body.
     */
    @Override
    public ResponseEntity<Resource> patchMoi(final HttpServletRequest httpServletRequest, final Resource resource) {
        final ProvMnsRequestParameters requestParameters =
            ProvMnsRequestParameters.toProvMnsRequestParameters(httpServletRequest);
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * Delete one or multiple resources.
     *
     * @param httpServletRequest      URI request including path
     * @return {@code ResponseEntity} The response body is empty, HTTP status returned.
     */
    @Override
    public ResponseEntity<Void> deleteMoi(final HttpServletRequest httpServletRequest) {
        final ProvMnsRequestParameters requestParameters =
            ProvMnsRequestParameters.toProvMnsRequestParameters(httpServletRequest);

        final String cmHandleId = alternateIdMatcher.getCmHandleId(requestParameters.getClassName());
        final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(cmHandleId);

        if (!yangModelCmHandle.getDataProducerIdentifier().isEmpty()
                && Objects.equals(CmHandleState.READY, yangModelCmHandle.getCompositeState().getCmHandleState())) {

            final ConfigurationManagementDeleteInput configurationManagementDeleteInput =
                                                               getConfigurationManagementDeleteInput(requestParameters);

            policyExecutor.checkPermission(yangModelCmHandle,
                                           OperationType.DELETE,
                                null,
                                           requestParameters.getUriLdnFirstPart(),
                                           jsonObjectMapper.asJsonString(configurationManagementDeleteInput));

            final UrlTemplateParameters urlTemplateParameters = RestServiceUrlTemplateBuilder.newInstance()
                    .fixedPathSegment(configurationManagementDeleteInput.getTargetIdentifier())
                    .createUrlTemplateParameters(yangModelCmHandle.getDmiServiceName(),
                                                 "/ProvMns");

            dmiRestClient.synchronousDeleteOperationWithJsonData(RequiredDmiService.DATA,
                    urlTemplateParameters, OperationType.DELETE);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private static ConfigurationManagementDeleteInput getConfigurationManagementDeleteInput(
                                                                     final ProvMnsRequestParameters requestParameters) {
        final String targetIdentifier = String.format("/%s/%s=%s", requestParameters.getUriLdnFirstPart(),
                                                                   requestParameters.getClassName(),
                                                                   requestParameters.getId());

        return new ConfigurationManagementDeleteInput(OperationType.DELETE.getOperationName(),
                                                targetIdentifier);
    }
}
