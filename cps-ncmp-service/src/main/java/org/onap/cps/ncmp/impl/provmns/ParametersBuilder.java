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

package org.onap.cps.ncmp.impl.provmns;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.provmns.model.ClassNameIdGetDataNodeSelectorParameter;
import org.onap.cps.ncmp.impl.provmns.model.Resource;
import org.onap.cps.ncmp.impl.provmns.model.Scope;
import org.onap.cps.ncmp.impl.utils.http.RestServiceUrlTemplateBuilder;
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParametersBuilder {

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
    public UrlTemplateParameters createUrlTemplateParametersForGet(final Scope scope, final String filter,
                                                       final List<String> attributes,
                                                       final List<String> fields,
                                                       final ClassNameIdGetDataNodeSelectorParameter dataNodeSelector,
                                                       final YangModelCmHandle yangModelCmHandle) {
        return RestServiceUrlTemplateBuilder.newInstance()
            .fixedPathSegment(yangModelCmHandle.getAlternateId())
            .queryParameter("scopeType", scope.getScopeType() != null
                ? scope.getScopeType().getValue() : null)
            .queryParameter("scopeLevel", scope.getScopeLevel() != null
                ? scope.getScopeLevel().toString() : null)
            .queryParameter("filter", filter)
            .queryParameter("attributes", attributes != null ? attributes.toString() : null)
            .queryParameter("fields", fields != null ? fields.toString() : null)
            .queryParameter("dataNodeSelector", dataNodeSelector.getDataNodeSelector())
            .createUrlTemplateParameters(yangModelCmHandle.getDmiServiceName(), "ProvMnS");
    }

    /**
     * Creates a UrlTemplateParameters object containing the relevant fields for a put.
     *
     * @param resource            Provided resource parameter.
     * @param yangModelCmHandle   yangModelCmHandle object for resolved alternate ID
     * @return UrlTemplateParameters object.
     */
    public UrlTemplateParameters createUrlTemplateParametersForPut(final Resource resource,
                                                                   final YangModelCmHandle yangModelCmHandle) {

        return RestServiceUrlTemplateBuilder.newInstance()
            .fixedPathSegment(yangModelCmHandle.getAlternateId())
            .queryParameter("resource", resource.toString())
            .createUrlTemplateParameters(yangModelCmHandle.getDmiServiceName(), "ProvMnS");
    }

    /**
     * Creates a UrlTemplateParameters object containing the relevant fields for a delete action.
     *
     * @param yangModelCmHandle   yangModelCmHandle object for resolved alternate ID
     * @return UrlTemplateParameters object.
     */
    public UrlTemplateParameters createUrlTemplateParametersForDelete(final YangModelCmHandle yangModelCmHandle) {

        return RestServiceUrlTemplateBuilder.newInstance()
            .fixedPathSegment(yangModelCmHandle.getAlternateId())
            .createUrlTemplateParameters(yangModelCmHandle.getDmiServiceName(), "ProvMnS");
    }
}