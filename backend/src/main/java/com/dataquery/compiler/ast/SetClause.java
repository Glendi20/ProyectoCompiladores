package com.dataquery.compiler.ast;

/**
 * Representa una asignación en UPDATE: columna = valor
 * Ejemplo: SET nombre = 'Juan', edad = 25
 */
public class SetClause extends ASTNode {
    public String column;
    public ExpressionNode value;

    public SetClause(String column, ExpressionNode value) {
        this.column = column;
        this.value  = value;
    }

    @Override
    public void buildString(StringBuilder sb, int indent) {
        String sp = " ".repeat(indent);
        sb.append(sp).append(column).append(" = ");
        value.buildString(sb, 0);
    }
}
