/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2020-2021 Nordix Foundation.
 * Modifications Copyright (C) 2021 Bell Canada.
 * Modifications Copyright (C) 2021 Pantheon.tech
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

package org.onap.cps.spi.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Setter(AccessLevel.PROTECTED)
@Getter
public class DataNode {

    DataNode() {    }

    private String dataspace;
    private String schemaSetName;
    private String anchorName;
    private ExtendedModuleReference extendedModuleReference;
    private String xpath;
    private Map<String, Object> leaves = Collections.emptyMap();
    private Collection<String> xpathsChildren;
    private Collection<DataNode> childDataNodes = Collections.emptySet();
}
