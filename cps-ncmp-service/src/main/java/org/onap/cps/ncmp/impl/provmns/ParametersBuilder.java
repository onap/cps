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

import static org.onap.cps.ncmp.impl.models.RequiredDmiService.DATA;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.provmns.model.ClassNameIdGetDataNodeSelectorParameter;
import org.onap.cps.ncmp.impl.provmns.model.Scope;
import org.onap.cps.ncmp.impl.utils.http.RestServiceUrlTemplateBuilder;
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParametersBuilder {

    /**
     * Creates a UrlTemplateParameters object containing the relevant fields for read requests.
     *
     * @param yangModelCmHandle yangModelCmHandle object for resolved alternate ID
     * @param targetFdn         Target FDN for the resource
     * @param scope             Provided className parameter
     * @param filter            Filter string
     * @param attributes        Attributes List
     * @param fields            Fields list
     * @param dataNodeSelector  dataNodeSelector parameter
     * @return UrlTemplateParameters object.
     */
    public UrlTemplateParameters createUrlTemplateParametersForRead(final YangModelCmHandle yangModelCmHandle,
                                                                    final String targetFdn,
                                                                    final Scope scope,
                                                                    final String filter,
                                                                    final List<String> attributes,
                                                                    final List<String> fields,
                                                                    final ClassNameIdGetDataNodeSelectorParameter
                                                                        dataNodeSelector) {
        final String dmiServiceName = yangModelCmHandle.resolveDmiServiceName(DATA);
        final String targetFdnWithoutPrecedingSlash = targetFdn.substring(1);
        return RestServiceUrlTemplateBuilder.newInstance()
            .fixedPathSegment(targetFdnWithoutPrecedingSlash)
            .queryParameter("scopeType", scope.getScopeType() != null ? scope.getScopeType().getValue() : null)
            .queryParameter("scopeLevel", scope.getScopeLevel() != null ? scope.getScopeLevel().toString() : null)
            .queryParameter("filter", filter)
            .queryParameterAllowBlankValue("attributes", attributes == null ? null : String.join(",", attributes))
            .queryParameterAllowBlankValue("fields", fields == null ? null : String.join(",", fields))
            .queryParameter("dataNodeSelector", dataNodeSelector.getDataNodeSelector())
            .createUrlTemplateParameters(dmiServiceName, "ProvMnS");
    }

    /**
     * Creates a UrlTemplateParameters object containing the relevant fields for a write requests.
     *
     * @param yangModelCmHandle yangModelCmHandle object for resolved alternate ID
     * @return UrlTemplateParameters object.
     */
    public UrlTemplateParameters createUrlTemplateParametersForWrite(final YangModelCmHandle yangModelCmHandle,
                                                                     final String targetFdn) {
        final String dmiServiceName = yangModelCmHandle.resolveDmiServiceName(DATA);
        final String targetFdnWithoutPrecedingSlash = targetFdn.substring(1);
        return RestServiceUrlTemplateBuilder.newInstance()
            .fixedPathSegment(targetFdnWithoutPrecedingSlash)
            .createUrlTemplateParameters(dmiServiceName, "ProvMnS");
    }

}
