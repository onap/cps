package org.onap.cps.ncmp.provmns.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import org.springframework.lang.Nullable;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
/**
 * ResourceOneOf
 */

@JsonTypeName("Resource_oneOf")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-03-03T10:16:44.871763977Z[Europe/Dublin]", comments = "Generator version: 7.12.0")
public class ResourceOneOf implements Resource {

  private String id;

  private @Nullable String objectClass;

  private @Nullable String objectInstance;

  private @Nullable Object attributes;

  public ResourceOneOf() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ResourceOneOf(String id) {
    this.id = id;
  }

  public ResourceOneOf id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull 
  @Schema(name = "id", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public ResourceOneOf objectClass(String objectClass) {
    this.objectClass = objectClass;
    return this;
  }

  /**
   * Get objectClass
   * @return objectClass
   */
  
  @Schema(name = "objectClass", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("objectClass")
  public String getObjectClass() {
    return objectClass;
  }

  public void setObjectClass(String objectClass) {
    this.objectClass = objectClass;
  }

  public ResourceOneOf objectInstance(String objectInstance) {
    this.objectInstance = objectInstance;
    return this;
  }

  /**
   * Get objectInstance
   * @return objectInstance
   */
  
  @Schema(name = "objectInstance", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("objectInstance")
  public String getObjectInstance() {
    return objectInstance;
  }

  public void setObjectInstance(String objectInstance) {
    this.objectInstance = objectInstance;
  }

  public ResourceOneOf attributes(Object attributes) {
    this.attributes = attributes;
    return this;
  }

  /**
   * Get attributes
   * @return attributes
   */
  
  @Schema(name = "attributes", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("attributes")
  public Object getAttributes() {
    return attributes;
  }

  public void setAttributes(Object attributes) {
    this.attributes = attributes;
  }
    /**
    * A container for additional, undeclared properties.
    * This is a holder for any undeclared properties as specified with
    * the 'additionalProperties' keyword in the OAS document.
    */
    private Map<String, List> additionalProperties;

    /**
    * Set the additional (undeclared) property with the specified name and value.
    * If the property does not already exist, create it otherwise replace it.
    */
    @JsonAnySetter
    public ResourceOneOf putAdditionalProperty(String key, List value) {
        if (this.additionalProperties == null) {
            this.additionalProperties = new HashMap<String, List>();
        }
        this.additionalProperties.put(key, value);
        return this;
    }

    /**
    * Return the additional (undeclared) property.
    */
    @JsonAnyGetter
    public Map<String, List> getAdditionalProperties() {
        return additionalProperties;
    }

    /**
    * Return the additional (undeclared) property with the specified name.
    */
    public List getAdditionalProperty(String key) {
        if (this.additionalProperties == null) {
            return null;
        }
        return this.additionalProperties.get(key);
    }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResourceOneOf resourceOneOf = (ResourceOneOf) o;
    return Objects.equals(this.id, resourceOneOf.id) &&
        Objects.equals(this.objectClass, resourceOneOf.objectClass) &&
        Objects.equals(this.objectInstance, resourceOneOf.objectInstance) &&
        Objects.equals(this.attributes, resourceOneOf.attributes) &&
    Objects.equals(this.additionalProperties, resourceOneOf.additionalProperties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, objectClass, objectInstance, attributes, additionalProperties);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ResourceOneOf {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    objectClass: ").append(toIndentedString(objectClass)).append("\n");
    sb.append("    objectInstance: ").append(toIndentedString(objectInstance)).append("\n");
    sb.append("    attributes: ").append(toIndentedString(attributes)).append("\n");
    
    sb.append("    additionalProperties: ").append(toIndentedString(additionalProperties)).append("\n");
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

