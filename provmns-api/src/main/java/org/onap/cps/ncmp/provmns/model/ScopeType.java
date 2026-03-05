package org.onap.cps.ncmp.provmns.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Gets or Sets ScopeType
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-03-03T10:16:44.871763977Z[Europe/Dublin]", comments = "Generator version: 7.12.0")
public enum ScopeType {
  
  BASE_ONLY("BASE_ONLY"),
  
  BASE_NTH_LEVEL("BASE_NTH_LEVEL"),
  
  BASE_SUBTREE("BASE_SUBTREE"),
  
  BASE_ALL("BASE_ALL");

  private String value;

  ScopeType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ScopeType fromValue(String value) {
    for (ScopeType b : ScopeType.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

