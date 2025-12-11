/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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
package org.onap.cps.ncmp.impl.provmns

import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.provmns.model.ClassNameIdGetDataNodeSelectorParameter
import org.onap.cps.ncmp.impl.provmns.model.Scope
import spock.lang.Specification

class ParametersBuilderSpec extends Specification{

    def objectUnderTest = new ParametersBuilder()

    def 'Create url template parameters for read operations (GET).'() {
        when: 'Creating URL parameters for GET with all possible items'
            def result = objectUnderTest.createUrlTemplateParametersForRead(
                new YangModelCmHandle(dmiServiceName: 'myDmiService'),
                '/target/fdn',
                new Scope(scopeLevel: 1, scopeType: 'BASE_SUBTREE'),
                'my filter',
                ['my attributes'],
                ['my fields'],
                new ClassNameIdGetDataNodeSelectorParameter(dataNodeSelector: 'my dataNodeSelector'),

            )
        then: 'the template has the correct result'
            assert result.urlTemplate.toString().startsWith('myDmiService/ProvMnS/v1//target/fdn?')
        and: 'all url variables have been set correctly'
            assert result.urlVariables.size() == 6
            assert result.urlVariables.scopeLevel == '1'
            assert result.urlVariables.scopeType == 'BASE_SUBTREE'
            assert result.urlVariables.filter == 'my filter'
            assert result.urlVariables.attributes == 'my attributes'
            assert result.urlVariables.fields == 'my fields'
            assert result.urlVariables.dataNodeSelector == 'my dataNodeSelector'
    }

    def 'Create url template parameters for GET without any optional parameter.'() {
        when: 'Creating URL parameters for GET with null=values where possible'
            def result = objectUnderTest.createUrlTemplateParametersForRead(
                new YangModelCmHandle(dmiServiceName: 'myDmiService'),
                'some target fdn',
                new Scope(),
                null,
                null,
                null,
                new ClassNameIdGetDataNodeSelectorParameter(dataNodeSelector: 'my dataNodeSelector'),
            )
        then: 'The url variables contains only a data node selector'
            assert result.urlVariables.size() == 1
            assert result.urlVariables.keySet()[0] == 'dataNodeSelector'
    }

    def 'Create url template parameters for write operations.'() {
        when: 'Creating URL parameters for PUT (or PATCH)'
            def result = objectUnderTest.createUrlTemplateParametersForWrite(new YangModelCmHandle(dmiServiceName: 'myDmiService'),'/target/fdn')
        then: 'the template has the correct correct'
            assert result.urlTemplate.toString().startsWith('myDmiService/ProvMnS/v1//target/fdn')
        and: 'no url variables have been set'
            assert result.urlVariables.isEmpty()
    }

}
