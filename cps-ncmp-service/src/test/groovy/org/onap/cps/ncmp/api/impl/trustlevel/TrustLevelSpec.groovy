/*
 * ============LICENSE_START========================================================
 * Copyright (c) 2023 Nordix Foundation.
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

package org.onap.cps.ncmp.api.impl.trustlevel

import org.onap.cps.ncmp.impl.inventory.models.TrustLevel
import spock.lang.Specification

class TrustLevelSpec extends Specification {

    def 'Get effective trust level between this and other.'() {
        expect: 'the lower of two is returned'
            assert effectiveLevel == current.getEffectiveTrustLevel(other)
        where: 'the following trust level is used'
            current             | other           || effectiveLevel
            TrustLevel.COMPLETE | TrustLevel.NONE || TrustLevel.NONE
            TrustLevel.NONE     | TrustLevel.COMPLETE || TrustLevel.NONE
            TrustLevel.COMPLETE | TrustLevel.COMPLETE || TrustLevel.COMPLETE
    }

}
