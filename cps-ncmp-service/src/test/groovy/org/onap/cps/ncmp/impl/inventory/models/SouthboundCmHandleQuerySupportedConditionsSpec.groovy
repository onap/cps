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

class SouthboundCmHandleQuerySupportedConditionsSpec extends Specification {

    def 'Southbound cm handle query condition names.'() {
        expect: '6 conditions with the correct names'
            assert SouthboundCmHandleQuerySupportedConditions.CONDITION_NAMES.size() == 6
            assert SouthboundCmHandleQuerySupportedConditions.CONDITION_NAMES.containsAll('hasAllModules',
                                                                            'hasAllProperties',
                                                                            'hasAllAdditionalProperties',
                                                                            'cmHandleWithCpsPath',
                                                                            'cmHandleWithDmiPlugin',
                                                                            'cmHandleWithTrustLevel')
    }
}
