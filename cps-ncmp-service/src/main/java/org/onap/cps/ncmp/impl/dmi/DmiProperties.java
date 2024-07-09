/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.dmi;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class DmiProperties {
    @Value("${ncmp.dmi.auth.username}")
    private String authUsername;
    @Value("${ncmp.dmi.auth.password}")
    private String authPassword;
    @Getter(AccessLevel.NONE)
    @Value("${ncmp.dmi.api.base-path}")
    private String dmiBasePath;
    @Value("${ncmp.dmi.auth.enabled}")
    private boolean dmiBasicAuthEnabled;

    /**
     * Removes both leading and trailing slashes if they are present.
     *
     * @return dmi base path without any slashes ("/")
     */
    public String getDmiBasePath() {
        if (dmiBasePath.startsWith("/")) {
            dmiBasePath = dmiBasePath.substring(1);
        }
        if (dmiBasePath.endsWith("/")) {
            dmiBasePath = dmiBasePath.substring(0, dmiBasePath.length() - 1);
        }
        return dmiBasePath;
    }
}
