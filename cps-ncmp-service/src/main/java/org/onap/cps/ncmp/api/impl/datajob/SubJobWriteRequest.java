package org.onap.cps.ncmp.api.impl.datajob;

import lombok.Data;

import java.util.List;

@Data
public class SubJobWriteRequest {
    private String dataAcceptType;
    private String dataContentType;
    private String dataProducerId;
    private List<DMI3ggpWriteOperation> data;
}
