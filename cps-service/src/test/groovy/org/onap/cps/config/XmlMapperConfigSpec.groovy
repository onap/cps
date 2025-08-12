/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 Deutsche Telekom AG
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

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import spock.lang.Specification

class XmlMapperConfigSpec extends Specification {

    def 'xmlMapper bean method returns a configured XmlMapper'() {
        given: 'an XmlMapper configuration'
            def config = new XmlMapperConfig()
        when: 'the xmlMapper bean method is invoked'
            XmlMapper mapper = config.xmlMapper()
        then: 'a XmlMapper is returned'
            mapper instanceof XmlMapper
    }
}