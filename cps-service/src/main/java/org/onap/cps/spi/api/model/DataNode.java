/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2020-2022 Nordix Foundation.
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

package org.onap.cps.spi.api.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter(AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode
public class DataNode implements Serializable {

    private static final long serialVersionUID = 1482619410918597467L;

    DataNode() {}

    private String dataspace;
    private String schemaSetName;
    private String anchorName;
    private ModuleReference moduleReference;
    private String xpath;
    private String moduleNamePrefix;
    private Map<String, Serializable> leaves = Collections.emptyMap();
    private Collection<String> xpathsChildren;
    private Collection<DataNode> childDataNodes = Collections.emptySet();
}
