/*
 *  ============LICENSE_START=======================================================
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

package org.onap.cps.config

import org.modelmapper.ModelMapper
import spock.lang.Specification
import springfox.documentation.spring.web.plugins.Docket

class CpsConfigSpec extends Specification {
    def objectUnderTest = new CpsConfig()

    def 'CPS configuration has a Model Mapper'() {
        expect: 'the CPS configuration has a Model Mapper'
            objectUnderTest.modelMapper() instanceof ModelMapper
    }

    def 'CPS configuration has a Docket API'() {
        expect: 'the CPS configuration has a Docket API'
            objectUnderTest.api() instanceof Docket
    }
}
