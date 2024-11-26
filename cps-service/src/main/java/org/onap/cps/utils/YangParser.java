/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 Nordix Foundation.
 * Modifications Copyright (C) 2024 TechMahindra Ltd.
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

import static org.onap.cps.utils.YangParserHelper.VALIDATE_AND_PARSE;
import static org.onap.cps.utils.YangParserHelper.VALIDATE_ONLY;

import io.micrometer.core.annotation.Timed;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.api.impl.YangTextSchemaSourceSetCache;
import org.onap.cps.api.model.Anchor;
import org.onap.cps.yang.TimedYangTextSchemaSourceSetBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class YangParser {

    private final YangParserHelper yangParserHelper;
    private final YangTextSchemaSourceSetCache yangTextSchemaSourceSetCache;
    private final TimedYangTextSchemaSourceSetBuilder timedYangTextSchemaSourceSetBuilder;

    /**
     * Parses data into (normalized) ContainerNode according to schema context for the given anchor.
     *
     * @param nodeData  data string
     * @param anchor    the anchor for the node data
     * @return the NormalizedNode object
     */
    @Timed(value = "cps.utils.yangparser.nodedata.with.parent.parse",
        description = "Time taken to parse node data with a parent")
    public ContainerNode parseData(final ContentType contentType,
                                   final String nodeData,
                                   final Anchor anchor,
                                   final String parentNodeXpath) {
        final SchemaContext schemaContext = getSchemaContext(anchor);
        try {
            return yangParserHelper
                    .parseData(contentType, nodeData, schemaContext, parentNodeXpath, VALIDATE_AND_PARSE);
        } catch (final DataValidationException e) {
            invalidateCache(anchor);
        }
        return yangParserHelper.parseData(contentType, nodeData, schemaContext, parentNodeXpath, VALIDATE_AND_PARSE);
    }

    /**
     * Parses data into (normalized) ContainerNode according to schema context for the given yang resource.
     *
     * @param nodeData                         data string
     * @param yangResourcesNameToContentMap    yang resource to content map
     * @return                                 the NormalizedNode object
     */
    @Timed(value = "cps.utils.yangparser.nodedata.with.parent.with.yangResourceMap.parse",
            description = "Time taken to parse node data with a parent")
    public ContainerNode parseData(final ContentType contentType,
                                   final String nodeData,
                                   final Map<String, String> yangResourcesNameToContentMap,
                                   final String parentNodeXpath) {
        final SchemaContext schemaContext = getSchemaContext(yangResourcesNameToContentMap);
        return yangParserHelper.parseData(contentType, nodeData, schemaContext, parentNodeXpath, VALIDATE_AND_PARSE);
    }

    /**
     * Parses data to validate it, using the schema context for given anchor.
     *
     * @param anchor                    the anchor used for node data validation
     * @param parentNodeXpath           the xpath of the parent node
     * @param nodeData                  JSON or XML data string to validate
     * @param contentType               the content type of the data (e.g., JSON or XML)
     * @throws DataValidationException  if validation fails
     */
    public void validateData(final ContentType contentType,
                             final String nodeData,
                             final Anchor anchor,
                             final String parentNodeXpath) {
        final SchemaContext schemaContext = getSchemaContext(anchor);
        try {
            yangParserHelper.parseData(contentType, nodeData, schemaContext, parentNodeXpath, VALIDATE_ONLY);
        } catch (final DataValidationException e) {
            invalidateCache(anchor);
            log.error("Data validation failed for anchor: {}, xpath: {}, details: {}", anchor, parentNodeXpath,
                    e.getMessage());
        }
        yangParserHelper.parseData(contentType, nodeData, schemaContext, parentNodeXpath, VALIDATE_ONLY);
    }

    private SchemaContext getSchemaContext(final Anchor anchor) {
        return yangTextSchemaSourceSetCache.get(anchor.getDataspaceName(),
            anchor.getSchemaSetName()).getSchemaContext();
    }

    private SchemaContext getSchemaContext(final Map<String, String> yangResourcesNameToContentMap) {
        return timedYangTextSchemaSourceSetBuilder.getYangTextSchemaSourceSet(yangResourcesNameToContentMap)
                .getSchemaContext();
    }

    private void invalidateCache(final Anchor anchor) {
        yangTextSchemaSourceSetCache.removeFromCache(anchor.getDataspaceName(), anchor.getSchemaSetName());
    }

}
