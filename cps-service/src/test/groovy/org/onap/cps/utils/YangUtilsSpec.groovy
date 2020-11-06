/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
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

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.common.Revision
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException
import spock.lang.Specification
import spock.lang.Unroll

class YangUtilsSpec extends Specification{
    def 'Parsing a valid Yang Model'() {
        given: 'a yang model (file)'
            def file = new File(ClassLoader.getSystemClassLoader().getResource('bookstore.yang').getFile())
        when: 'the file is parsed'
            def result = YangUtils.parseYangModelFile(file)
        then: 'the result contain 1 module of the correct name and revision'
            result.modules.size() == 1
            def optionalModule = result.findModule('bookstore', Revision.of('2020-09-15'))
            optionalModule.isPresent()
    }

    @Unroll
    def 'parsing invalid yang file (#description)'() {
        given: 'a file with #description'
            File file = new File(ClassLoader.getSystemClassLoader().getResource(filename).getFile());
        when: 'the file is parsed'
            YangUtils.parseYangModelFile(file)
        then: 'an exception is thrown'
            thrown(expectedException)
        where: 'the following parameters are used'
             filename           | description          || expectedException
            'invalid.yang'      | 'no valid content'   || YangSyntaxErrorException
            'someOtherFile.txt' | 'no .yang extension' || IllegalArgumentException
    }

    def 'Parsing a valid Json String'() {
        given: 'a yang model (file)'
            def jsonData = org.onap.cps.TestUtils.getResourceFileContent('bookstore.json')
        and: 'a model for that data'
            def file = new File(ClassLoader.getSystemClassLoader().getResource('bookstore.yang').getFile())
            def schemaContext = YangUtils.parseYangModelFile(file)
        when: 'the json data is parsed'
            NormalizedNode<?, ?> result = YangUtils.parseJsonData(jsonData, schemaContext);
        then: 'the result is a normalized node of the correct type'
            result.nodeType == QName.create('org:onap:ccsdk:sample','2020-09-15','bookstore')
    }

    def 'Parsing an invalid Json String'() {
        given: 'a yang model (file)'
            def jsonData = '{incomplete json'
        and: 'a model'
            def file = new File(ClassLoader.getSystemClassLoader().getResource('bookstore.yang').getFile())
            def schemaContext = YangUtils.parseYangModelFile(file)
        when: 'the invalid json is parsed'
            YangUtils.parseJsonData(jsonData, schemaContext);
        then: ' an exception is thrown'
            thrown(IllegalStateException)
    }


}
