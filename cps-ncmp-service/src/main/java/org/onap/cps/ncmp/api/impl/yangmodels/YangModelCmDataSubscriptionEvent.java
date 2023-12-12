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


package org.onap.cps.ncmp.api.impl.yangmodels;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Subscription event model to persist data into DB.
 * Yang model subscription event
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class YangModelCmDataSubscriptionEvent {

    @EqualsAndHashCode.Include
    @JsonProperty("name")
    private String name;

    private List<CmHandle> cmHandles;

    @AllArgsConstructor
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CmHandle {

        @JsonProperty()
        private final String id;

        private List<Filter> filters;
    }

    @AllArgsConstructor
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Filter {

        @JsonProperty()
        private final String id;

        @JsonProperty()
        private final List<String> subscribers;
    }
}


