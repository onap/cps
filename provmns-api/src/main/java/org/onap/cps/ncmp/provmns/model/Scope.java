package org.onap.cps.ncmp.provmns.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.onap.cps.ncmp.provmns.model.ScopeType;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Scope
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-03-03T10:16:44.871763977Z[Europe/Dublin]", comments = "Generator version: 7.12.0")
public class Scope {

  private @Nullable ScopeType scopeType;

  private @Nullable Integer scopeLevel;

  public Scope scopeType(ScopeType scopeType) {
    this.scopeType = scopeType;
    return this;
  }

  /**
   * Get scopeType
   * @return scopeType
   */
  @Valid 
  @Schema(name = "scopeType", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("scopeType")
  public ScopeType getScopeType() {
    return scopeType;
  }

  public void setScopeType(ScopeType scopeType) {
    this.scopeType = scopeType;
  }

  public Scope scopeLevel(Integer scopeLevel) {
    this.scopeLevel = scopeLevel;
    return this;
  }

  /**
   * Get scopeLevel
   * @return scopeLevel
   */
  
  @Schema(name = "scopeLevel", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("scopeLevel")
  public Integer getScopeLevel() {
    return scopeLevel;
  }

  public void setScopeLevel(Integer scopeLevel) {
    this.scopeLevel = scopeLevel;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Scope scope = (Scope) o;
    return Objects.equals(this.scopeType, scope.scopeType) &&
        Objects.equals(this.scopeLevel, scope.scopeLevel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scopeType, scopeLevel);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Scope {\n");
    sb.append("    scopeType: ").append(toIndentedString(scopeType)).append("\n");
    sb.append("    scopeLevel: ").append(toIndentedString(scopeLevel)).append("\n");
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

