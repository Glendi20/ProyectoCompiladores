package com.dataquery.compiler.ast;

/**
 * Representa un JOIN en una consulta SELECT.
 * Ejemplo: INNER JOIN pedidos ON usuarios.id = pedidos.usuario_id
 */
public class JoinNode extends ASTNode {

    public enum JoinType { INNER, LEFT, RIGHT, CROSS, FULL }

    public JoinType joinType;
    public String tableName;
    public String alias;
    public ConditionNode onCondition;

    public JoinNode(JoinType joinType, String tableName, String alias, ConditionNode onCondition) {
        this.joinType = joinType;
        this.tableName = tableName;
        this.alias = alias;
        this.onCondition = onCondition;
    }

    public String joinTypeToString() {
        switch (joinType) {
            case INNER: return "INNER JOIN";
            case LEFT:  return "LEFT JOIN";
            case RIGHT: return "RIGHT JOIN";
            case CROSS: return "CROSS JOIN";
            case FULL:  return "FULL OUTER JOIN";
            default:    return "JOIN";
        }
    }

    @Override
    public void buildString(StringBuilder sb, int indent) {
        String sp = " ".repeat(indent);
        sb.append(sp).append(joinTypeToString()).append(": ").append(tableName);
        if (alias != null) sb.append(" AS ").append(alias);
        sb.append("\n");
        if (onCondition != null) {
            sb.append(sp).append("  ON:\n");
            onCondition.buildString(sb, indent + 4);
        }
    }
}
