package org.onap.cps.query.parser;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuerySelectWhere {

    private List<String> selectFields;
    private List<WhereCondition> whereConditions;
    private List<String> whereBooleanOperators;

    public boolean hasWhereConditions() {
        return whereConditions != null && !whereConditions.isEmpty();
    }

    public record WhereCondition(String name, String operator, Object value) { }

    public static QuerySelectWhere createFrom(final String select, final String where) {
        return QuerySelectWhereUtil.getQuerySelectWhere(select, where);
    }
}
