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

package org.onap.cps.ncmp.init;

import java.util.Map;
import lombok.NonNull;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

public interface ModelLoader extends ApplicationListener<ApplicationReadyEvent> {

    @Override
    void onApplicationEvent(@NonNull ApplicationReadyEvent applicationReadyEvent);

    /**
     * Create schema set.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schemaset name
     * @param yangResourceContentMap yang resource content map
     * @return true if schema set is created
     */
    boolean createSchemaSet(String dataspaceName, String schemaSetName, Map<String, String> yangResourceContentMap);

    /**
     * Create anchor.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schemaset name
     * @param anchorName anchor name
     * @return true if anchor is created
     */
    boolean createAnchor(String dataspaceName, String schemaSetName, String anchorName);
}
