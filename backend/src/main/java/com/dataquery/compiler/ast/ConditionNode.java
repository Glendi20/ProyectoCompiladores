package com.dataquery.compiler.ast;

import java.util.List;

/**
 * Nodo AST para condiciones WHERE/ON/HAVING.
 * Soporta: comparaciones, IN, LIKE, BETWEEN, IS NULL, IS NOT NULL, AND/OR encadenados.
 */
public class ConditionNode extends ASTNode {

    public enum SpecialOp { NONE, IN, NOT_IN, LIKE, ILIKE, BETWEEN, IS_NULL, IS_NOT_NULL, EXISTS, NOT_EXISTS, SUBQUERY }

    // Comparación simple: left op right
    public ExpressionNode left;
    public CompOperator   op;
    public ExpressionNode right;

    // Operadores especiales
    public SpecialOp          specialOp  = SpecialOp.NONE;
    public List<ExpressionNode> inValues = null;
    public ExpressionNode     betweenLow = null;
    public ExpressionNode     betweenHigh = null;

    // Encadenamiento AND/OR
    public String        logicalConnector = null;  // "AND" o "OR"
    public ConditionNode next             = null;

    public ConditionNode(ExpressionNode left, CompOperator op, ExpressionNode right) {
        this.left  = left;
        this.op    = op;
        this.right = right;
    }

    public ConditionNode() {}

    public static String operatorToString(CompOperator op) {
        switch (op) {
            case EQUAL:         return "=";
            case GREATER:       return ">";
            case LESS:          return "<";
            case GREATER_EQUAL: return ">=";
            case LESS_EQUAL:    return "<=";
            case NOT_EQUAL:     return "!=";
            default:            return "?";
        }
    }

    @Override
    public void buildString(StringBuilder sb, int indent) {
        String sp = " ".repeat(indent);
        sb.append(sp);

        switch (specialOp) {
            case IS_NULL:
                sb.append(left.value).append(" IS NULL\n");
                break;
            case IS_NOT_NULL:
                sb.append(left.value).append(" IS NOT NULL\n");
                break;
            case IN:
            case NOT_IN:
                sb.append(left.value).append(specialOp == SpecialOp.NOT_IN ? " NOT IN (" : " IN (");
                if (inValues != null) {
                    for (int i = 0; i < inValues.size(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(inValues.get(i).value);
                    }
                }
                sb.append(")\n");
                break;
            case LIKE:
            case ILIKE:
                sb.append(left.value).append(specialOp == SpecialOp.ILIKE ? " ILIKE '" : " LIKE '").append(right.value).append("'\n");
                break;
            case BETWEEN:
                sb.append(left.value).append(" BETWEEN ").append(betweenLow.value).append(" AND ").append(betweenHigh.value).append("\n");
                break;
            default:
                if (left != null && right != null)
                    sb.append(left.value).append(" ").append(operatorToString(op)).append(" ").append(formatValue(right)).append("\n");
        }
    }

    private String formatValue(ExpressionNode expr) {
        return expr.type == ExpressionNode.ExprType.STRING ? "'" + expr.value + "'" : expr.value;
    }
}
