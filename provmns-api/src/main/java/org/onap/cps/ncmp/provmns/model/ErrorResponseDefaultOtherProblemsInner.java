package org.onap.cps.ncmp.provmns.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ErrorResponseDefaultOtherProblemsInner
 */

@JsonTypeName("ErrorResponseDefault_otherProblems_inner")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-03-03T10:16:44.871763977Z[Europe/Dublin]", comments = "Generator version: 7.12.0")
public class ErrorResponseDefaultOtherProblemsInner {

  private @Nullable String status;

  private String type;

  private @Nullable String reason;

  private @Nullable String title;

  @Valid
  private List<String> badAttributes = new ArrayList<>();

  @Valid
  private List<String> badObjects = new ArrayList<>();

  public ErrorResponseDefaultOtherProblemsInner() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ErrorResponseDefaultOtherProblemsInner(String type) {
    this.type = type;
  }

  public ErrorResponseDefaultOtherProblemsInner status(String status) {
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

  public ErrorResponseDefaultOtherProblemsInner type(String type) {
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

  public ErrorResponseDefaultOtherProblemsInner reason(String reason) {
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

  public ErrorResponseDefaultOtherProblemsInner title(String title) {
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

  public ErrorResponseDefaultOtherProblemsInner badAttributes(List<String> badAttributes) {
    this.badAttributes = badAttributes;
    return this;
  }

  public ErrorResponseDefaultOtherProblemsInner addBadAttributesItem(String badAttributesItem) {
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

  public ErrorResponseDefaultOtherProblemsInner badObjects(List<String> badObjects) {
    this.badObjects = badObjects;
    return this;
  }

  public ErrorResponseDefaultOtherProblemsInner addBadObjectsItem(String badObjectsItem) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ErrorResponseDefaultOtherProblemsInner errorResponseDefaultOtherProblemsInner = (ErrorResponseDefaultOtherProblemsInner) o;
    return Objects.equals(this.status, errorResponseDefaultOtherProblemsInner.status) &&
        Objects.equals(this.type, errorResponseDefaultOtherProblemsInner.type) &&
        Objects.equals(this.reason, errorResponseDefaultOtherProblemsInner.reason) &&
        Objects.equals(this.title, errorResponseDefaultOtherProblemsInner.title) &&
        Objects.equals(this.badAttributes, errorResponseDefaultOtherProblemsInner.badAttributes) &&
        Objects.equals(this.badObjects, errorResponseDefaultOtherProblemsInner.badObjects);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, type, reason, title, badAttributes, badObjects);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
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
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

