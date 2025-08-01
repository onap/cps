/*
 * ============LICENSE_START========================================================
 * Copyright (c) 2022-2025 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.inventory.models


import spock.lang.Specification

class NorthboundCmHandleQuerySupportedConditionsSpec extends Specification {

    def 'Northbound cm handle query condition names.'() {
        expect: '4 conditions with the correct names'
            assert NorthboundCmHandleQuerySupportedConditions.CONDITION_NAMES.size() == 4
            assert NorthboundCmHandleQuerySupportedConditions.CONDITION_NAMES.containsAll('hasAllProperties',
                                                                           'hasAllModules',
                                                                           'cmHandleWithCpsPath',
                                                                           'cmHandleWithTrustLevel')
    }

}
