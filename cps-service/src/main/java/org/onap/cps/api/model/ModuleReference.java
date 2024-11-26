/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2022 Nordix Foundation.
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

package org.onap.cps.api.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleReference implements Serializable {

    private static final long serialVersionUID = -1761408847591042599L;
    private String moduleName;
    private String revision;
    @Builder.Default
    private String namespace = "";

    /**
     * Constructor for module references without namespace (will remain blank).
     *
     * @param moduleName module names.
     * @param revision   revision of module.
     */
    public ModuleReference(final String moduleName, final String revision) {
        this.moduleName = moduleName;
        this.revision = revision;
        this.namespace = "";
    }

}
