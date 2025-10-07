package org.onap.cps.ncmp.rest.provmns.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConfigurationManagementDeleteInput {
    private String operation;
    private String targetIdentifier;
}
