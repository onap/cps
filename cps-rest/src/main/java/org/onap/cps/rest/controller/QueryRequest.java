package org.onap.cps.rest.controller;

import java.util.List;

public class QueryRequest {
    private String xpath;
    private List<String> select;
    private String condition;

    public String getXpath() {
        return xpath;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    public List<String> getSelect() {
        return select;
    }

    public void setSelect(List<String> select) {
        this.select = select;
    }

    public String getCondition() {
        return condition;
    }

    public void setWhere(String where) {
        this.condition = where;
    }
}