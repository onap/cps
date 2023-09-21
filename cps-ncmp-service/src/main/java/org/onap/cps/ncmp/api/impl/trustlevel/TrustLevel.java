/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.trustlevel;

public enum TrustLevel {
    NONE, COMPLETE;

    /**
     * Finds the value of the given enum.
     *
     * @param trustLevelValue value of the enum
     * @return TrustLevel
     */
    public static TrustLevel fromString(final String trustLevelValue) {
        for (final TrustLevel currentValue : TrustLevel.values()) {
            if (currentValue.name().equals(trustLevelValue)) {
                return currentValue;
            }
        }
        return null;
    }
}