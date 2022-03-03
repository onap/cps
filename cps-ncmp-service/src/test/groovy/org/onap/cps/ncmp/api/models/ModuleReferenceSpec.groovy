/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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
package org.onap.cps.ncmp.api.models

import org.onap.cps.spi.model.ModuleReference
import spock.lang.Specification

class ModuleReferenceSpec extends Specification {

    def 'lombok data annotation correctly implements toString() and hashCode() methods'() {
        given: 'two moduleReference objects'
            def moduleReference1 = new ModuleReference('module1', "some namespace", '1')
            def moduleReference2 = new ModuleReference('module1', "some namespace", '1')
        when: 'lombok generated methods are called'
        then: 'the methods exist and behaviour is accurate'
            assert moduleReference1.toString() == moduleReference2.toString()
            assert moduleReference1.hashCode() == moduleReference2.hashCode()
        and: 'therefore equals works as expected'
            assert moduleReference1.equals(moduleReference2)
    }

}
