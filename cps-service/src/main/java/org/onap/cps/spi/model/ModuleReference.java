/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2020 Nordix Foundation.
 * Modifications Copyright 2020-2021 Pantheon.tech
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

import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleReference implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String namespace;
    private String revision;

    @Override
    public String toString() {
        return "ModuleReference{"
            + "name='" + name + '\''
            + ", namespace='" + namespace + '\''
            + ", revision='" + revision + '\''
            + '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ModuleReference that = (ModuleReference) o;
        return Objects.equals(name, that.name)
            && Objects.equals(namespace, that.namespace)
            && Objects.equals(revision, that.revision);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, namespace, revision);
    }
}
