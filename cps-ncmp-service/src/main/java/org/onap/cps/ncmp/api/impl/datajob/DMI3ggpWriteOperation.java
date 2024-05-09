package org.onap.cps.ncmp.api.impl.datajob;

import lombok.Data;

import java.util.Map;

@Data
public class DMI3ggpWriteOperation {
    private String path;
    private String op;
    private String moduleSetTag;
    private Object value;
    private Integer operationId;
    private Map<String, String> privateProperties;
}
