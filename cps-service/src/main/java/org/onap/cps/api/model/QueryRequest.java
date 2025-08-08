package org.onap.cps.api.model;
import lombok.Setter;

import java.util.List;
public class QueryRequest {
    @Setter
    private String xpath;
    @Setter
    private List<String> select;
    private String condition;
    public String getXpath() {
        return xpath;
    }

    public List<String> getSelect() {
        return select;
    }

    public String getCondition() {
        return condition;
    }
    public void setWhere(String where) {
        this.condition = where;
    }
}
