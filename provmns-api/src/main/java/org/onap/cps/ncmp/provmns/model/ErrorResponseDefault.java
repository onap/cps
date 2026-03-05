package org.onap.cps.ncmp.provmns.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.onap.cps.ncmp.provmns.model.ErrorResponseDefaultOtherProblemsInner;
import org.springframework.lang.Nullable;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ErrorResponseDefault
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-03-03T10:16:44.871763977Z[Europe/Dublin]", comments = "Generator version: 7.12.0")
public class ErrorResponseDefault implements ClassNameIdPatchDefaultResponse {

  private @Nullable String status;

  private String type;

  private @Nullable String reason;

  private @Nullable String title;

  @Valid
  private List<String> badAttributes = new ArrayList<>();

  @Valid
  private List<String> badObjects = new ArrayList<>();

  @Valid
  private List<@Valid ErrorResponseDefaultOtherProblemsInner> otherProblems = new ArrayList<>();

  public ErrorResponseDefault() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ErrorResponseDefault(String type) {
    this.type = type;
  }

  public ErrorResponseDefault status(String status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  
  @Schema(name = "status", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("status")
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public ErrorResponseDefault type(String type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  @NotNull 
  @Schema(name = "type", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public ErrorResponseDefault reason(String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * Get reason
   * @return reason
   */
  
  @Schema(name = "reason", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("reason")
  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public ErrorResponseDefault title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Get title
   * @return title
   */
  
  @Schema(name = "title", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public ErrorResponseDefault badAttributes(List<String> badAttributes) {
    this.badAttributes = badAttributes;
    return this;
  }

  public ErrorResponseDefault addBadAttributesItem(String badAttributesItem) {
    if (this.badAttributes == null) {
      this.badAttributes = new ArrayList<>();
    }
    this.badAttributes.add(badAttributesItem);
    return this;
  }

  /**
   * Get badAttributes
   * @return badAttributes
   */
  
  @Schema(name = "badAttributes", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("badAttributes")
  public List<String> getBadAttributes() {
    return badAttributes;
  }

  public void setBadAttributes(List<String> badAttributes) {
    this.badAttributes = badAttributes;
  }

  public ErrorResponseDefault badObjects(List<String> badObjects) {
    this.badObjects = badObjects;
    return this;
  }

  public ErrorResponseDefault addBadObjectsItem(String badObjectsItem) {
    if (this.badObjects == null) {
      this.badObjects = new ArrayList<>();
    }
    this.badObjects.add(badObjectsItem);
    return this;
  }

  /**
   * Get badObjects
   * @return badObjects
   */
  
  @Schema(name = "badObjects", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("badObjects")
  public List<String> getBadObjects() {
    return badObjects;
  }

  public void setBadObjects(List<String> badObjects) {
    this.badObjects = badObjects;
  }

  public ErrorResponseDefault otherProblems(List<@Valid ErrorResponseDefaultOtherProblemsInner> otherProblems) {
    this.otherProblems = otherProblems;
    return this;
  }

  public ErrorResponseDefault addOtherProblemsItem(ErrorResponseDefaultOtherProblemsInner otherProblemsItem) {
    if (this.otherProblems == null) {
      this.otherProblems = new ArrayList<>();
    }
    this.otherProblems.add(otherProblemsItem);
    return this;
  }

  /**
   * Get otherProblems
   * @return otherProblems
   */
  @Valid 
  @Schema(name = "otherProblems", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("otherProblems")
  public List<@Valid ErrorResponseDefaultOtherProblemsInner> getOtherProblems() {
    return otherProblems;
  }

  public void setOtherProblems(List<@Valid ErrorResponseDefaultOtherProblemsInner> otherProblems) {
    this.otherProblems = otherProblems;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ErrorResponseDefault errorResponseDefault = (ErrorResponseDefault) o;
    return Objects.equals(this.status, errorResponseDefault.status) &&
        Objects.equals(this.type, errorResponseDefault.type) &&
        Objects.equals(this.reason, errorResponseDefault.reason) &&
        Objects.equals(this.title, errorResponseDefault.title) &&
        Objects.equals(this.badAttributes, errorResponseDefault.badAttributes) &&
        Objects.equals(this.badObjects, errorResponseDefault.badObjects) &&
        Objects.equals(this.otherProblems, errorResponseDefault.otherProblems);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, type, reason, title, badAttributes, badObjects, otherProblems);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ErrorResponseDefault {\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    badAttributes: ").append(toIndentedString(badAttributes)).append("\n");
    sb.append("    badObjects: ").append(toIndentedString(badObjects)).append("\n");
    sb.append("    otherProblems: ").append(toIndentedString(otherProblems)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

