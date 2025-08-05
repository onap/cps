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

package org.onap.cps.ncmp.rest.util;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.exceptions.NcmpException;
import org.onap.cps.ncmp.impl.dmi.DmiServiceAuthenticationProperties;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.utils.http.RestServiceUrlTemplateBuilder;
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters;
import org.onap.cps.ncmp.rest.provmns.model.ClassNameIdGetDataNodeSelectorParameter;
import org.onap.cps.ncmp.rest.provmns.model.Resource;
import org.onap.cps.ncmp.rest.provmns.model.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProvMnSParametersMapper {

    private final DmiServiceAuthenticationProperties dmiServiceAuthenticationProperties;

    /**
     * Creates a UrlTemplateParameters object containing the relevant fields for a get.
     *
     * @param scope               Provided className parameter.
     * @param filter              Filter string.
     * @param attributes          Attributes List.
     * @param fields              Fields list
     * @param dataNodeSelector    dataNodeSelector parameter
     * @param yangModelCmHandle   yangModelCmHandle object for resolved alternate ID
     * @return UrlTemplateParameters object.
     */
    public UrlTemplateParameters getUrlTemplateParameters(final Scope scope, final String filter,
                                                      final List<String> attributes, final List<String> fields,
                                                      final ClassNameIdGetDataNodeSelectorParameter dataNodeSelector,
                                                      final YangModelCmHandle yangModelCmHandle,
                                                      final String urlPath) {

        return RestServiceUrlTemplateBuilder.newInstance()
            .queryParameter("scopeType", scope.getScopeType() != null
                ? scope.getScopeType().getValue() : null)
            .queryParameter("scopeLevel", scope.getScopeLevel() != null
                ? scope.getScopeLevel().toString() : null)
            .queryParameter("filter", filter)
            .queryParameter("attributes", attributes != null ? attributes.toString() : null)
            .queryParameter("fields", fields != null ? fields.toString() : null)
            .queryParameter("dataNodeSelector", dataNodeSelector.getDataNodeSelector() != null
                ? dataNodeSelector.getDataNodeSelector() : null)
            .createUrlTemplateParameters(yangModelCmHandle.getDmiServiceName(), "ProvMnS");
    }

    /**
     * Converts ResponseEntity object to ResponseEntity Resource object.
     *
     * @param response provided ResponseEntity object to be converted.
     * @return extracted alternate ID string
     */
    public ResponseEntity<Resource> convertToResource(final ResponseEntity<Object> response) {
        return new ResponseEntity<>(
            (Resource) response.getBody(), response.getHeaders(), response.getStatusCode());
    }

    /**
     * Check if dataProducerIdentifier is empty or null, if so throw exception.
     *
     * @param yangModelCmHandle given yangModelCmHandle.
     */
    public void checkDataProducerIdentifier(final YangModelCmHandle yangModelCmHandle) {
        if (yangModelCmHandle.getDataProducerIdentifier() == null
            || yangModelCmHandle.getDataProducerIdentifier().isEmpty()) {
            throw new NcmpException("Cm Handle not compatible", "Cm Handle " + yangModelCmHandle.getId()
                + " has empty data producer identifier");
        }
    }
}
