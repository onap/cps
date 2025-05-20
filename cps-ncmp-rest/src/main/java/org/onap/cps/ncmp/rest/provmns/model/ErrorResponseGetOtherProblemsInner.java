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
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.Nullable;

/**
 * ErrorResponseGetOtherProblemsInner.
 */

@JsonTypeName("ErrorResponseGet_otherProblems_inner")
public class ErrorResponseGetOtherProblemsInner {

    private @Nullable String status;

    private String type;

    private @Nullable String reason;

    private @Nullable String title;


    private List<String> badQueryParams = new ArrayList<>();

    public ErrorResponseGetOtherProblemsInner() {
        super();
    }

    /**
     * Constructor with only required parameters.
     */
    public ErrorResponseGetOtherProblemsInner(final String type) {
        this.type = type;
    }

    public ErrorResponseGetOtherProblemsInner status(final String status) {
        this.status = status;
        return this;
    }

    /**
     * Get status.
     *
     * @return status
     */

    @Schema(name = "status", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public ErrorResponseGetOtherProblemsInner type(final String type) {
        this.type = type;
        return this;
    }

    /**
     * Get type.
     *
     * @return type
     */
    @NotNull
    @Schema(name = "type", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("type")
    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public ErrorResponseGetOtherProblemsInner reason(final String reason) {
        this.reason = reason;
        return this;
    }

    /**
     * Get reason.
     *
     * @return reason
     */

    @Schema(name = "reason", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("reason")
    public String getReason() {
        return reason;
    }

    public void setReason(final String reason) {
        this.reason = reason;
    }

    public ErrorResponseGetOtherProblemsInner title(final String title) {
        this.title = title;
        return this;
    }

    /**
     * Get title.
     *
     * @return title
     */

    @Schema(name = "title", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public ErrorResponseGetOtherProblemsInner badQueryParams(final List<String> badQueryParams) {
        this.badQueryParams = badQueryParams;
        return this;
    }

    /**
     * Add bad query params item.
     *
     * @param badQueryParamsItem bad query params item
     * @return ErrorResponseGetOtherProblemsInner
     */
    public ErrorResponseGetOtherProblemsInner addBadQueryParamsItem(final String badQueryParamsItem) {
        if (this.badQueryParams == null) {
            this.badQueryParams = new ArrayList<>();
        }
        this.badQueryParams.add(badQueryParamsItem);
        return this;
    }

    /**
     * Get badQueryParams.
     *
     * @return badQueryParams
     */

    @Schema(name = "badQueryParams", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("badQueryParams")
    public List<String> getBadQueryParams() {
        return badQueryParams;
    }

    public void setBadQueryParams(final List<String> badQueryParams) {
        this.badQueryParams = badQueryParams;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ErrorResponseGetOtherProblemsInner errorResponseGetOtherProblemsInner =
                (ErrorResponseGetOtherProblemsInner) o;
        return Objects.equals(this.status, errorResponseGetOtherProblemsInner.status) && Objects.equals(this.type,
                errorResponseGetOtherProblemsInner.type) && Objects.equals(this.reason,
                errorResponseGetOtherProblemsInner.reason) && Objects.equals(this.title,
                errorResponseGetOtherProblemsInner.title) && Objects.equals(this.badQueryParams,
                errorResponseGetOtherProblemsInner.badQueryParams);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, type, reason, title, badQueryParams);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("class ErrorResponseGetOtherProblemsInner {\n");
        sb.append("    status: ").append(toIndentedString(status)).append("\n");
        sb.append("    type: ").append(toIndentedString(type)).append("\n");
        sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
        sb.append("    title: ").append(toIndentedString(title)).append("\n");
        sb.append("    badQueryParams: ").append(toIndentedString(badQueryParams)).append("\n");
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

