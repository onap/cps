/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 TechMahindra Ltd.
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

package org.onap.cps.api;

import java.util.Collection;
import java.util.Map;
import org.onap.cps.api.model.Anchor;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.utils.ContentType;

public interface DataNodeBuilderService {

    Collection<DataNode> buildDataNodesWithAnchorAndXpathToNodeData(final Anchor anchor,
            final Map<String, String> nodesData, final ContentType contentType);

    Collection<DataNode> buildDataNodesWithAnchorXpathAndNodeData(final Anchor anchor, final String xpath,
            final String nodeData, final ContentType contentType);

    Collection<DataNode> buildDataNodesWithAnchorParentXpathAndNodeData(final Anchor anchor,
                                                                        final String parentNodeXpath,
                                                                        final String nodeData,
                                                                        final ContentType contentType);

    Collection<DataNode> buildDataNodesWithYangResourceXpathAndNodeData(
            final Map<String, String> yangResourcesNameToContentMap, final String xpath,
            final String nodeData, final ContentType contentType);

}
