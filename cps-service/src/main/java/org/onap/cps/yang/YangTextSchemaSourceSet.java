/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Pantheon.tech
 *  Modifications Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.yang;

import java.util.List;
import org.onap.cps.api.model.ModuleReference;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * CPS YangTextSchemaSource.
 */
public interface YangTextSchemaSourceSet {

    /**
     * Returns list of modules references for given YangSchema.
     *
     * @return list of ModuleRef
     */
    List<ModuleReference> getModuleReferences();

    /**
     *  Return SchemaContext for given YangSchema.
     * @return SchemaContext
     */
    SchemaContext getSchemaContext();
}
