/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe
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

package org.onap.cps.ncmp.provmns.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/**
 * Concrete implementation of Resource. Generated from the 3GPP TS28532 ProvMnS spec but maintained
 * here as a source file because Resource (which references this class) is hand-written and both
 * must compile together.
 */
@Getter
@Setter
public class ResourceOneOf implements Resource {

    private String id;
    private String objectClass;
    private String objectInstance;
    private Object attributes;
    private Map<String, List> additionalProperties = new HashMap<>();

    public ResourceOneOf() { }

    public ResourceOneOf(final String id) {
        this.id = id;
    }

    @JsonAnySetter
    public ResourceOneOf putAdditionalProperty(final String key, final List value) {
        this.additionalProperties.put(key, value);
        return this;
    }

    @JsonAnyGetter
    public Map<String, List> getAdditionalProperties() {
        return additionalProperties;
    }

    public List getAdditionalProperty(final String key) {
        return additionalProperties.get(key);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceOneOf)) return false;
        final ResourceOneOf that = (ResourceOneOf) o;
        return Objects.equals(id, that.id)
            && Objects.equals(objectClass, that.objectClass)
            && Objects.equals(objectInstance, that.objectInstance)
            && Objects.equals(attributes, that.attributes)
            && Objects.equals(additionalProperties, that.additionalProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, objectClass, objectInstance, attributes, additionalProperties);
    }

    @Override
    public String toString() {
        return "ResourceOneOf{"
            + "id=" + id
            + ", objectClass=" + objectClass
            + ", objectInstance=" + objectInstance
            + ", attributes=" + attributes
            + ", additionalProperties=" + additionalProperties
            + "}";
    }

}
