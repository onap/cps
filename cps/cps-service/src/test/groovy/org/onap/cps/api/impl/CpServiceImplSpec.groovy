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

package org.onap.cps.api.impl

import org.onap.cps.spi.DataPersistencyService
import spock.lang.Specification;


class CpServiceImplSpec extends Specification {

    def dataPersistencyService = Mock(DataPersistencyService)
    def objectUnderTest = new CpServiceImpl()

    def setup() {
        // Insert mocked dependencies
        objectUnderTest.dataPersistencyService = dataPersistencyService;
    }

    def 'Cps Service provides to its client the id assigned by the system when storing a data structure'() {
        given: 'that data persistency service is giving id 123 to a data structure it is asked to store'
            dataPersistencyService.storeJsonStructure(_) >> 123

        expect: 'Cps service returns the same id when storing data structure'
            objectUnderTest.storeJsonStructure('') == 123
    }
}
