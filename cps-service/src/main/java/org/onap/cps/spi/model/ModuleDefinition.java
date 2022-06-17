/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.spi.model;

import lombok.Builder;

public class ModuleDefinition extends ModuleReference {

    private static final long serialVersionUID = -6591435720836327732L;
    private final String content;

    @Builder (builderMethodName = "moduleDefinitionBuilder")
    public ModuleDefinition(final String moduleName, final String revision, final String content) {
        super(moduleName, revision);
        this.content = content;
    }
}