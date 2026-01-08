/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Deutsche Telekom AG
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

package org.onap.cps.utils;

import spock.lang.Specification;
import org.springframework.http.MediaType


class ContentTypeSpec extends Specification {

    def 'Should return correct ContentType based on given input.'() {
        given: 'contentType fromString method converts the input string as expectedContentType'
             ContentType.fromString(contentTypeString) == expectedContentType
        where:
             contentTypeString                || expectedContentType
             MediaType.APPLICATION_XML_VALUE  || ContentType.XML
             MediaType.APPLICATION_JSON_VALUE || ContentType.JSON
    }

}
