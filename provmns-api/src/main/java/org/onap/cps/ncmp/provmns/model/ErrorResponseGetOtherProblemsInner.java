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
 * ErrorResponseGetOtherProblemsInner
 */

@JsonTypeName("ErrorResponseGet_otherProblems_inner")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-03-03T10:16:44.871763977Z[Europe/Dublin]", comments = "Generator version: 7.12.0")
public class ErrorResponseGetOtherProblemsInner {

  private @Nullable String status;

  private String type;

  private @Nullable String reason;

  private @Nullable String title;

  @Valid
  private List<String> badQueryParams = new ArrayList<>();

  public ErrorResponseGetOtherProblemsInner() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ErrorResponseGetOtherProblemsInner(String type) {
    this.type = type;
  }

  public ErrorResponseGetOtherProblemsInner status(String status) {
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

  public ErrorResponseGetOtherProblemsInner type(String type) {
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

  public ErrorResponseGetOtherProblemsInner reason(String reason) {
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

  public ErrorResponseGetOtherProblemsInner title(String title) {
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

  public ErrorResponseGetOtherProblemsInner badQueryParams(List<String> badQueryParams) {
    this.badQueryParams = badQueryParams;
    return this;
  }

  public ErrorResponseGetOtherProblemsInner addBadQueryParamsItem(String badQueryParamsItem) {
    if (this.badQueryParams == null) {
      this.badQueryParams = new ArrayList<>();
    }
    this.badQueryParams.add(badQueryParamsItem);
    return this;
  }

  /**
   * Get badQueryParams
   * @return badQueryParams
   */
  
  @Schema(name = "badQueryParams", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("badQueryParams")
  public List<String> getBadQueryParams() {
    return badQueryParams;
  }

  public void setBadQueryParams(List<String> badQueryParams) {
    this.badQueryParams = badQueryParams;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ErrorResponseGetOtherProblemsInner errorResponseGetOtherProblemsInner = (ErrorResponseGetOtherProblemsInner) o;
    return Objects.equals(this.status, errorResponseGetOtherProblemsInner.status) &&
        Objects.equals(this.type, errorResponseGetOtherProblemsInner.type) &&
        Objects.equals(this.reason, errorResponseGetOtherProblemsInner.reason) &&
        Objects.equals(this.title, errorResponseGetOtherProblemsInner.title) &&
        Objects.equals(this.badQueryParams, errorResponseGetOtherProblemsInner.badQueryParams);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, type, reason, title, badQueryParams);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
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
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

