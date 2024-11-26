/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation.
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

package org.onap.cps.utils;

import lombok.RequiredArgsConstructor;
import org.onap.cps.api.impl.YangTextSchemaSourceSetCache;
import org.onap.cps.api.model.Anchor;
import org.onap.cps.cpspath.parser.CpsPathPrefixType;
import org.onap.cps.cpspath.parser.CpsPathQuery;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.yang.YangTextSchemaSourceSet;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PrefixResolver {
    private final YangTextSchemaSourceSetCache yangTextSchemaSourceSetCache;

    /**
     * Get the module prefix for the given xpath under the given anchor.
     *
     * @param anchor the anchor the xpath belong to
     * @param xpath the xpath to prefix a prefix for
     * @return the prefix of the module the top level element of given xpath
     */
    public String getPrefix(final Anchor anchor, final String xpath) {
        final CpsPathQuery cpsPathQuery = CpsPathUtil.getCpsPathQuery(xpath);
        if (cpsPathQuery.getCpsPathPrefixType() != CpsPathPrefixType.ABSOLUTE) {
            return "";
        }
        final String topLevelContainerName = cpsPathQuery.getContainerNames().get(0);

        final YangTextSchemaSourceSet yangTextSchemaSourceSet =
                yangTextSchemaSourceSetCache.get(anchor.getDataspaceName(), anchor.getSchemaSetName());
        final SchemaContext schemaContext = yangTextSchemaSourceSet.getSchemaContext();

        return schemaContext.getChildNodes().stream()
                .filter(DataNodeContainer.class::isInstance)
                .map(SchemaNode::getQName)
                .filter(qname -> qname.getLocalName().equals(topLevelContainerName))
                .findFirst()
                .map(QName::getModule)
                .flatMap(schemaContext::findModule)
                .map(Module::getPrefix)
                .orElse("");
    }

}
