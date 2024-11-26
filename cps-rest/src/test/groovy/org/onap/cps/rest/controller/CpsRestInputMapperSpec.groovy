/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.rest.controller

import org.mapstruct.factory.Mappers
import org.onap.cps.rest.model.AnchorDetails
import org.onap.cps.rest.model.SchemaSetDetails
import org.onap.cps.api.model.Anchor
import org.onap.cps.rest.model.ModuleReferences
import org.onap.cps.api.model.ModuleReference
import org.onap.cps.api.model.SchemaSet
import spock.lang.Specification

class CpsRestInputMapperSpec extends Specification {

    def objectUnderTest = Mappers.getMapper(CpsRestInputMapper.class)

    def 'Convert a SchemaSet to a SchemaSetDetails a ModuleReference'() {
        given: 'a ModuleReference'
            def moduleReference = new ModuleReference()
        and: 'a SchemaSet containing the ModuleReference'
            def schemaSet = new SchemaSet(name: 'some-schema-set', dataspaceName: 'some-dataspace',
                moduleReferences: [moduleReference])
        when: 'to schemaSetDetails is called'
            def result = objectUnderTest.toSchemaSetDetails(schemaSet)
        then: 'the result returns a SchemaSetDetails'
            result.class == SchemaSetDetails.class
        and: 'the results ModuleReferences are of type ModuleReference'
            result.moduleReferences[0].class == ModuleReferences.class
    }

    def 'Convert a schemaSet to a SchemaSetDetails without an ModuleReference'() {
        given: 'a SchemaSet'
            def schemaSet = new SchemaSet()
        when: 'to schemaSetDetails is called'
            def result = objectUnderTest.toSchemaSetDetails(schemaSet)
        then: 'the result returns a SchemaSetDetails'
            result.class == SchemaSetDetails.class
        and: 'the ModuleReferences of SchemaSetDetails is an empty collection'
            result.moduleReferences.size() == 0
    }

    def 'Convert an Anchor to an AnchorDetails'() {
        given: 'an Anchor'
            def anchor = new Anchor()
        when: 'to anchorDetails is called'
            def result = objectUnderTest.toAnchorDetails(anchor)
        then: 'the result returns an AnchorDetails'
            result.class == AnchorDetails.class
    }
}
