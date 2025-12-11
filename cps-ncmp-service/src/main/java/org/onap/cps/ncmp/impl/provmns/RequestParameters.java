/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe
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

package org.onap.cps.ncmp.impl.provmns;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestParameters {

    private String httpMethodName;
    private String uriLdnFirstPart;
    private String className;
    private String id;

    /**
     * Gets target FDN by combining URI-LDN-First-Part, className and id.
     *
     * @return String of FDN
     */
    public String toTargetFdn() {
        return uriLdnFirstPart + "/" + className + "=" + id;
    }
}
