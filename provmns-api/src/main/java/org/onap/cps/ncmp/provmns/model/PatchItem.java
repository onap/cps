package org.onap.cps.ncmp.provmns.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.onap.cps.ncmp.provmns.model.PatchOperation;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * PatchItem
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-03-03T10:16:44.871763977Z[Europe/Dublin]", comments = "Generator version: 7.12.0")
public class PatchItem {

  private PatchOperation op;

  private @Nullable String from;

  private String path;

  private @Nullable Object value = null;

  public PatchItem() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PatchItem(PatchOperation op, String path) {
    this.op = op;
    this.path = path;
  }

  public PatchItem op(PatchOperation op) {
    this.op = op;
    return this;
  }

  /**
   * Get op
   * @return op
   */
  @NotNull @Valid 
  @Schema(name = "op", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("op")
  public PatchOperation getOp() {
    return op;
  }

  public void setOp(PatchOperation op) {
    this.op = op;
  }

  public PatchItem from(String from) {
    this.from = from;
    return this;
  }

  /**
   * Get from
   * @return from
   */
  
  @Schema(name = "from", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("from")
  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public PatchItem path(String path) {
    this.path = path;
    return this;
  }

  /**
   * Get path
   * @return path
   */
  @NotNull 
  @Schema(name = "path", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("path")
  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public PatchItem value(Object value) {
    this.value = value;
    return this;
  }

  /**
   * Get value
   * @return value
   */
  
  @Schema(name = "value", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("value")
  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PatchItem patchItem = (PatchItem) o;
    return Objects.equals(this.op, patchItem.op) &&
        Objects.equals(this.from, patchItem.from) &&
        Objects.equals(this.path, patchItem.path) &&
        Objects.equals(this.value, patchItem.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(op, from, path, value);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
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
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

