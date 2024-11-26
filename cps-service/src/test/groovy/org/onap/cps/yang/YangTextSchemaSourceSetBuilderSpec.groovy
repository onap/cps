/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2021 Pantheon.tech
 *  Modifications Copyright (C) 2020-2022 Nordix Foundation
 *  Modifications Copyright (C) 2021 Bell Canada.
 *  Modifications Copyright (C) 2022 TechMahindra Ltd.
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

package org.onap.cps.yang

import org.onap.cps.TestUtils
import org.onap.cps.api.exceptions.ModelValidationException
import org.opendaylight.yangtools.yang.common.Revision
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class YangTextSchemaSourceSetBuilderSpec extends Specification {

    def 'Building a valid YangTextSchemaSourceSet using #filenameCase filename.'() {
        given: 'a yang model (file)'
            def yangResourceNameToContent = [filename: TestUtils.getResourceFileContent('bookstore.yang')]
        when: 'the content is parsed'
            def result = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        then: 'it can be validated successfully'
            YangTextSchemaSourceSetBuilder.validate(yangResourceNameToContent)
        and: 'the result contains 1 module of the correct name and revision'
            result.modules.size() == 1
            def optionalModule = result.findModule('stores', Revision.of('2020-09-15'))
            optionalModule.isPresent()
        where:
            filenameCase           | filename
            'generic'              | 'bookstore'
            'RFC-6020 recommended' | 'bookstore-test@2020-09-15.YANG'
    }

    def 'Building YangTextSchemaSourceSet error case: #description.'() {
        given: 'a file with #description'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap(filename)
        when: 'the content is parsed'
            YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent)
        then: 'an exception is thrown'
            thrown(expectedException)
        where: 'the following parameters are used'
            filename                      | description            || expectedException
            'invalid.yang'                | 'invalid content'      || ModelValidationException
            'invalid-empty.yang'          | 'no valid content'     || ModelValidationException
            'invalid-missing-import.yang' | 'no dependency module' || ModelValidationException
    }

    def 'Convert yang source to a YangTextSchemaSource.'() {
        given: 'a yang source text'
            def yangSourceText = TestUtils.getResourceFileContent('bookstore.yang')
        when: 'convert it to a YangTextSchemaSource'
            def result = YangTextSchemaSourceSetBuilder.toYangTextSchemaSource('some name', yangSourceText)
        then: 'the converted object has correct properties'
            assert result.toString() == '{identifier=RevisionSourceIdentifier [name=some name]}'
            assert new String(result.openStream().readAllBytes(), StandardCharsets.UTF_8) ==  yangSourceText
        and: 'it has no symbolic name'
            assert result.getSymbolicName().isEmpty()
    }
}
