/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.utils

import com.google.common.collect.ImmutableMap
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.DataNodeBuilder
import spock.lang.Specification

import static java.util.Arrays.asList

class DataMapUtilsSpec extends Specification {

    DataNode dataNode = buildDataNode(
            "/parent",
            ImmutableMap.<String, Object> of("a", "b", "c", asList("d", "e")),
            asList(
                    buildDataNode(
                            "/parent/child-list[@name='x']",
                            ImmutableMap.<String, Object> of("name", "x"),
                            Collections.emptyList()),
                    buildDataNode(
                            "/parent/child-list[@name='y']",
                            ImmutableMap.<String, Object> of("name", "y"),
                            Collections.emptyList()),
                    buildDataNode(
                            "/parent/child-object",
                            ImmutableMap.<String, Object> of("m", "n"),
                            asList(
                                    buildDataNode(
                                            "/parent/child-object/grand-child",
                                            ImmutableMap.<String, Object> of("o", "p"),
                                            Collections.emptyList()
                                    )
                            )
                    ),
            ))

    static DataNode buildDataNode(String xpath, Map<String, Object> leaves, List<DataNode> children) {
        return new DataNodeBuilder().withXpath(xpath).withLeaves(leaves).withChildDataNodes(children).build()
    }

    def 'Data node structure conversion to map.'() {
        when: 'Data node structure converted to map'
            def result = DataMapUtils.toDataMap(dataNode)
        then: 'root node leaves are top level elements'
            assert result["parent"]["a"] == "b"
            assert ((Collection) result["parent"]["c"]).containsAll("d", "e")
        and: 'leaves of child list element are listed as structures under common identifier'
            assert ((Collection) result["parent"]["child-list"]).size() == 2
            assert ((Collection) result["parent"]["child-list"]).containsAll(["name": "x"], ["name": "y"])
        and: 'leaves for child and grand-child elements are populated under their node identifiers'
            assert result["parent"]["child-object"]["m"] == "n"
            assert result["parent"]["child-object"]["grand-child"]["o"] == "p"
    }

}
