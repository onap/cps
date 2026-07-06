/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 Deutsche Telekom AG
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompositeQuery {

    /**
     * {@code CompositeQuery} is a single, self-referential data model used to represent a recursive CPS query
     *  expression.
     *
     * <ul>
     *   <li>{@code cpsPath}: the CPS path expression to match for this query node</li>
     *   <li>{@code operator}: the logical operator used to combine this node with its nested conditions;
     *       supported values are {@code and} and {@code or}, and the default is {@code and}</li>
     *   <li>{@code conditions}: the nested composite query conditions that are evaluated recursively</li>
     * </ul>
     */
    private String cpsPath;
    @Builder.Default
    private String operator = "and";
    @Builder.Default
    private Collection<CompositeQuery> conditions = new ArrayList<>();
}
