/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
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

package org.onap.cps.api;

import java.io.File;
import org.onap.cps.exceptions.CpsValidationException;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Responsible for managing module sets.
 */
public interface ModuleStoreService {

    /**
     * Parse and validate a string representing a yang model to generate a schema context.
     *
     * @param yangModelContent the input stream
     * @return the schema context
     */
    SchemaContext parseAndValidateModel(String yangModelContent);

    /**
     * Parse and validate a file representing a yang model to generate a schema context.
     *
     * @param yangModelFile the yang file
     * @return the schema context
     */
    SchemaContext parseAndValidateModel(File yangModelFile);

    /**
     * Store schema context for a yang model.
     *
     * @param schemaContext the schema context
     * @param dataspaceName the dataspace name
     * @throws CpsValidationException if input data already exists.
     */
    void storeSchemaContext(SchemaContext schemaContext, String dataspaceName);
}
