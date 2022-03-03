/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Pantheon.tech
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.onap.cps.spi.model.ModuleReference;
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
    @NonNull
    List<ModuleReference> getModuleReferences();

    /**
     *  Return SchemaContext for given YangSchema.
     * @return SchemaContext
     */
    @NonNull
    SchemaContext getSchemaContext();
}
