/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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
import org.onap.cps.TestUtils
import spock.lang.Specification

class GsonSpec extends Specification {

    def 'Iterate over JSON data with gson JsonReader'() {
        given: 'json data with two objects and JSON reader'
            def jsonData = TestUtils.getResourceFileContent('multiple-object-data.json')
            def objectUnderTest = new JsonReader(new StringReader(jsonData));
        when: 'data is iterated over with JsonReader methods'
            iterateWithJsonReader(objectUnderTest)
        then: 'no exception is thrown'
            noExceptionThrown()
    }

    def iterateWithJsonReader(JsonReader jsonReader) {
        switch(jsonReader.peek()) {
            case "STRING":
                print(jsonReader.nextString() + " ")
                break;
            case "BEGIN_OBJECT":
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    print(jsonReader.nextName() + " ")
                    iterateWithJsonReader(jsonReader)
                }
                jsonReader.endObject();
                println("")
                break;
            default:
                break;
        }
    }

}
