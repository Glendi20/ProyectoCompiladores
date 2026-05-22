package com.dataquery.compiler.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Nodo AST para SELECT extendido.
 * Soporta: DISTINCT, JOINs, WHERE con AND/OR, GROUP BY, HAVING, ORDER BY, LIMIT/OFFSET.
 */
public class SelectNode extends StatementNode {

    public List<String>        columns      = new ArrayList<>();
    public boolean             selectAll    = false;
    public boolean             distinct     = false;
    public String              tableName    = "";
    public String              tableAlias   = null;
    public List<JoinNode>      joins        = new ArrayList<>();
    public ConditionNode       whereCondition = null;   // soporta AND/OR via .next
    public List<String>        groupByColumns = new ArrayList<>();
    public ConditionNode       havingCondition = null;
    public List<OrderByClause> orderBy      = new ArrayList<>();
    public Integer             limit        = null;
    public Integer             offset       = null;
    public boolean             hasUnion     = false;
    public boolean             hasCte       = false;

    @Override public String getStatementType() { return "SELECT"; }

    public String toASTString() {
        StringBuilder sb = new StringBuilder();
        buildString(sb, 0);
        return sb.toString();
    }

    @Override
    public void buildString(StringBuilder sb, int indent) {
        String sp = " ".repeat(indent);
        sb.append(sp).append("SELECT").append(distinct ? " DISTINCT" : "").append("\n");

        sb.append(sp).append("  Columns: ");
        sb.append(selectAll ? "*" : String.join(", ", columns)).append("\n");

        sb.append(sp).append("  FROM: ").append(tableName);
        if (tableAlias != null) sb.append(" AS ").append(tableAlias);
        sb.append("\n");

        for (JoinNode join : joins) join.buildString(sb, indent + 2);

        if (whereCondition != null) {
            sb.append(sp).append("  WHERE:\n");
            ConditionNode c = whereCondition;
            while (c != null) {
                c.buildString(sb, indent + 4);
                if (c.logicalConnector != null) sb.append(" ".repeat(indent + 4)).append(c.logicalConnector).append("\n");
                c = c.next;
            }
        }
        if (!groupByColumns.isEmpty())
            sb.append(sp).append("  GROUP BY: ").append(String.join(", ", groupByColumns)).append("\n");
        if (havingCondition != null) {
            sb.append(sp).append("  HAVING:\n");
            havingCondition.buildString(sb, indent + 4);
        }
        if (!orderBy.isEmpty()) {
            sb.append(sp).append("  ORDER BY: ");
            List<String> parts = new ArrayList<>();
            for (OrderByClause o : orderBy) parts.add(o.toString());
            sb.append(String.join(", ", parts)).append("\n");
        }
        if (limit  != null) sb.append(sp).append("  LIMIT: ").append(limit).append("\n");
        if (offset != null) sb.append(sp).append("  OFFSET: ").append(offset).append("\n");
    }
}
