/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.utils;

import io.micrometer.core.annotation.Timed;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.springframework.stereotype.Component;

@Component
public class TimedYangParser {

    /**
     * Parses data into Collection of NormalizedNode according to given schema context.
     *
     * @param nodeData      data string
     * @param schemaContext schema context describing associated data model
     * @return the NormalizedNode object
     */
    @Timed(value = "cps.utils.yangparser.nodedata.parse",
        description = "Time taken to parse node data without a parent")
    public ContainerNode parseData(final ContentType contentType,
                                          final String nodeData,
                                          final SchemaContext schemaContext) {
        return YangUtils.parseData(contentType, nodeData, schemaContext);
    }

    /**
     * Parses data into NormalizedNode according to given schema context.
     *
     * @param nodeData      data string
     * @param schemaContext schema context describing associated data model
     * @return the NormalizedNode object
     */
    @Timed(value = "cps.utils.yangparser.nodedata.with.parent.parse",
        description = "Time taken to parse node data with a parent")
    public ContainerNode parseData(final ContentType contentType,
                                          final String nodeData,
                                          final SchemaContext schemaContext,
                                          final String parentNodeXpath) {
        return YangUtils.parseData(contentType, nodeData, schemaContext, parentNodeXpath);
    }
}
