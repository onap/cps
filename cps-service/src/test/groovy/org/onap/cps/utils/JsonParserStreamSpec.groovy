/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
 *  Modifications Copyright (C) 2022 Deutsche Telekom AG
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

import com.google.gson.stream.JsonReader
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream
import org.opendaylight.yangtools.yang.data.impl.schema.Builders
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import spock.lang.Specification
import org.onap.cps.TestUtils

class JsonParserStreamSpec extends Specification{

    def 'Multiple data tree parsing with ODL JsonStreamParser'(){
        given: 'json data with two objects and JSON reader'
            def jsonData = TestUtils.getResourceFileContent('multiple-object-data.json')
            def jsonReader = new JsonReader(new StringReader(jsonData))
            def yangResourcesMap = TestUtils.getYangResourcesAsMap('multipleDataTree.yang')
        and: 'schema context'
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourcesMap).schemaContext()
        and: 'variable to store the result of parsing'
            DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> builder =
                    Builders.containerBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(schemaContext.getQName()))
            def normalizedNodeStreamWriter = ImmutableNormalizedNodeStreamWriter.from(builder)
            def jsonCodecFactory = JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02
                    .getShared((EffectiveModelContext) schemaContext)
        and: 'JSON parser stream'
            def jsonParserStream = JsonParserStream.create(normalizedNodeStreamWriter, jsonCodecFactory)
        when: 'parsing is invoked with the given JSON reader'
            jsonParserStream.parse(jsonReader)
            def result = builder.build()
        then: 'result is the correct size'
            result.size() == 2
        then: 'data container child is a type of normalized node'
            def dataContainerChild = result.body().getAt(index)
            dataContainerChild instanceof NormalizedNode == true
        then: 'qualified name created is as expected'
            dataContainerChild.identifier.nodeType == QName.create('org:onap:ccsdk:multiDataTree', '2020-09-15', nodeName)
        where:
            index   | nodeName
            0       | 'first-container'
            1       | 'last-container'

    }
}
