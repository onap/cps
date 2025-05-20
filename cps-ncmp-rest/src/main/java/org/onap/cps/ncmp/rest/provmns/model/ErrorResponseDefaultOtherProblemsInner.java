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
 * ErrorResponseDefaultOtherProblemsInner.
 */

@JsonTypeName("ErrorResponseDefault_otherProblems_inner")
public class ErrorResponseDefaultOtherProblemsInner {

    private @Nullable String status;

    private String type;

    private @Nullable String reason;

    private @Nullable String title;


    private List<String> badAttributes = new ArrayList<>();


    private List<String> badObjects = new ArrayList<>();

    public ErrorResponseDefaultOtherProblemsInner() {
        super();
    }

    /**
     * Constructor with only required parameters.
     */
    public ErrorResponseDefaultOtherProblemsInner(final String type) {
        this.type = type;
    }

    public ErrorResponseDefaultOtherProblemsInner status(final String status) {
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

    public ErrorResponseDefaultOtherProblemsInner type(final String type) {
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

    public ErrorResponseDefaultOtherProblemsInner reason(final String reason) {
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

    public ErrorResponseDefaultOtherProblemsInner title(final String title) {
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

    public ErrorResponseDefaultOtherProblemsInner badAttributes(final List<String> badAttributes) {
        this.badAttributes = badAttributes;
        return this;
    }

    /**
     * Add bad attributes item.
     *
     * @param badAttributesItem bad attributes item
     * @return ErrorResponseDefaultOtherProblemsInner
     */
    public ErrorResponseDefaultOtherProblemsInner addBadAttributesItem(final String badAttributesItem) {
        if (this.badAttributes == null) {
            this.badAttributes = new ArrayList<>();
        }
        this.badAttributes.add(badAttributesItem);
        return this;
    }

    /**
     * Get badAttributes.
     *
     * @return badAttributes
     */

    @Schema(name = "badAttributes", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("badAttributes")
    public List<String> getBadAttributes() {
        return badAttributes;
    }

    public void setBadAttributes(final List<String> badAttributes) {
        this.badAttributes = badAttributes;
    }

    public ErrorResponseDefaultOtherProblemsInner badObjects(final List<String> badObjects) {
        this.badObjects = badObjects;
        return this;
    }

    /**
     * Add bad objects item.
     *
     * @param badObjectsItem bad objects item
     * @return ErrorResponseDefaultOtherProblemsInner
     */
    public ErrorResponseDefaultOtherProblemsInner addBadObjectsItem(final String badObjectsItem) {
        if (this.badObjects == null) {
            this.badObjects = new ArrayList<>();
        }
        this.badObjects.add(badObjectsItem);
        return this;
    }

    /**
     * Get badObjects.
     *
     * @return badObjects
     */

    @Schema(name = "badObjects", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("badObjects")
    public List<String> getBadObjects() {
        return badObjects;
    }

    public void setBadObjects(final List<String> badObjects) {
        this.badObjects = badObjects;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ErrorResponseDefaultOtherProblemsInner errorResponseDefaultOtherProblemsInner =
                (ErrorResponseDefaultOtherProblemsInner) o;
        return Objects.equals(this.status, errorResponseDefaultOtherProblemsInner.status) && Objects.equals(this.type,
                errorResponseDefaultOtherProblemsInner.type) && Objects.equals(this.reason,
                errorResponseDefaultOtherProblemsInner.reason) && Objects.equals(this.title,
                errorResponseDefaultOtherProblemsInner.title) && Objects.equals(this.badAttributes,
                errorResponseDefaultOtherProblemsInner.badAttributes) && Objects.equals(this.badObjects,
                errorResponseDefaultOtherProblemsInner.badObjects);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, type, reason, title, badAttributes, badObjects);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("class ErrorResponseDefaultOtherProblemsInner {\n");
        sb.append("    status: ").append(toIndentedString(status)).append("\n");
        sb.append("    type: ").append(toIndentedString(type)).append("\n");
        sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
        sb.append("    title: ").append(toIndentedString(title)).append("\n");
        sb.append("    badAttributes: ").append(toIndentedString(badAttributes)).append("\n");
        sb.append("    badObjects: ").append(toIndentedString(badObjects)).append("\n");
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

