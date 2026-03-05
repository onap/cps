package org.onap.cps.ncmp.provmns.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.onap.cps.ncmp.provmns.model.ErrorResponseGetOtherProblemsInner;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Default schema for the response message body in case the Get is not successful.
 */

@Schema(name = "ErrorResponseGet", description = "Default schema for the response message body in case the Get is not successful.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-03-03T10:16:44.871763977Z[Europe/Dublin]", comments = "Generator version: 7.12.0")
public class ErrorResponseGet {

  private @Nullable String status;

  private String type;

  private @Nullable String reason;

  private @Nullable String title;

  @Valid
  private List<String> badQueryParams = new ArrayList<>();

  @Valid
  private List<@Valid ErrorResponseGetOtherProblemsInner> otherProblems = new ArrayList<>();

  public ErrorResponseGet() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ErrorResponseGet(String type) {
    this.type = type;
  }

  public ErrorResponseGet status(String status) {
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

  public ErrorResponseGet type(String type) {
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

  public ErrorResponseGet reason(String reason) {
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

  public ErrorResponseGet title(String title) {
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

  public ErrorResponseGet badQueryParams(List<String> badQueryParams) {
    this.badQueryParams = badQueryParams;
    return this;
  }

  public ErrorResponseGet addBadQueryParamsItem(String badQueryParamsItem) {
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

  public ErrorResponseGet otherProblems(List<@Valid ErrorResponseGetOtherProblemsInner> otherProblems) {
    this.otherProblems = otherProblems;
    return this;
  }

  public ErrorResponseGet addOtherProblemsItem(ErrorResponseGetOtherProblemsInner otherProblemsItem) {
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
  public List<@Valid ErrorResponseGetOtherProblemsInner> getOtherProblems() {
    return otherProblems;
  }

  public void setOtherProblems(List<@Valid ErrorResponseGetOtherProblemsInner> otherProblems) {
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
    ErrorResponseGet errorResponseGet = (ErrorResponseGet) o;
    return Objects.equals(this.status, errorResponseGet.status) &&
        Objects.equals(this.type, errorResponseGet.type) &&
        Objects.equals(this.reason, errorResponseGet.reason) &&
        Objects.equals(this.title, errorResponseGet.title) &&
        Objects.equals(this.badQueryParams, errorResponseGet.badQueryParams) &&
        Objects.equals(this.otherProblems, errorResponseGet.otherProblems);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, type, reason, title, badQueryParams, otherProblems);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ErrorResponseGet {\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    badQueryParams: ").append(toIndentedString(badQueryParams)).append("\n");
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

