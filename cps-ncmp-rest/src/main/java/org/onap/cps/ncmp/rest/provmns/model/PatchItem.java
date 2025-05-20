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

package org.onap.cps.ncmp.rest.provmns.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import org.springframework.lang.Nullable;

/**
 * PatchItem.
 */

public class PatchItem {

    private PatchOperation op;

    private @Nullable String from;

    private String path;

    private @Nullable Object value = null;

    public PatchItem() {
        super();
    }

    /**
     * Constructor with only required parameters.
     */
    public PatchItem(final PatchOperation op, final String path) {
        this.op = op;
        this.path = path;
    }

    public PatchItem op(final PatchOperation op) {
        this.op = op;
        return this;
    }

    /**
     * Get op.
     *
     * @return op
     */
    @NotNull
    @Schema(name = "op", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("op")
    public PatchOperation getOp() {
        return op;
    }

    public void setOp(final PatchOperation op) {
        this.op = op;
    }

    public PatchItem from(final String from) {
        this.from = from;
        return this;
    }

    /**
     * Get from.
     *
     * @return from
     */

    @Schema(name = "from", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("from")
    public String getFrom() {
        return from;
    }

    public void setFrom(final String from) {
        this.from = from;
    }

    public PatchItem path(final String path) {
        this.path = path;
        return this;
    }

    /**
     * Get path.
     *
     * @return path
     */
    @NotNull
    @Schema(name = "path", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("path")
    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public PatchItem value(final Object value) {
        this.value = value;
        return this;
    }

    /**
     * Get value.
     *
     * @return value
     */

    @Schema(name = "value", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("value")
    public Object getValue() {
        return value;
    }

    public void setValue(final Object value) {
        this.value = value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final PatchItem patchItem = (PatchItem) o;
        return Objects.equals(this.op, patchItem.op) && Objects.equals(this.from, patchItem.from) && Objects.equals(
                this.path, patchItem.path) && Objects.equals(this.value, patchItem.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(op, from, path, value);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("class PatchItem {\n");
        sb.append("    op: ").append(toIndentedString(op)).append("\n");
        sb.append("    from: ").append(toIndentedString(from)).append("\n");
        sb.append("    path: ").append(toIndentedString(path)).append("\n");
        sb.append("    value: ").append(toIndentedString(value)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(final Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}

