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

    def 'Extract url template parameters for GET'() {
        when:'a set of given parameters from a call are passed in'
            def result = objectUnderTest.createUrlTemplateParametersForGet(new Scope(scopeLevel: 1, scopeType: 'BASE_ALL',),
                    'my-filter', ['my-attribute'], ['my-field'], new ClassNameIdGetDataNodeSelectorParameter(dataNodeSelector: 'some-dataSelector'),
                    new YangModelCmHandle(dmiServiceName: 'some-dmi-service'), new RequestPathParameters(uriLdnFirstPart:'somePathVariable=somePathValue', className: 'someClassName', id:'someId'))
        then:'verify object has been mapped correctly'
            result.urlVariables().get('filter') == 'my-filter'
            result.urlVariables().get('attributes') == 'my-attribute'
            result.urlVariables().get('fields') == 'my-field'
    }
}
