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

package org.onap.cps.ncmp.api.inventory.models;

import lombok.Getter;

@Getter
public enum TrustLevel {
    NONE(0), COMPLETE(99);
    private final int level;

    /**
     * Creates TrustLevel enum from a numeric value.
     *
     * @param       level numeric value between 0-99
     */
    TrustLevel(final int level) {
        this.level = level;
    }

    /**
     * Gets the lower trust level (effective) among two.
     *
     * @param       other the trust level compared with this
     * @return      the lower trust level
     */
    public final TrustLevel getEffectiveTrustLevel(final TrustLevel other) {
        if (other.level < this.level) {
            return other;
        }
        return this;
    }

}
