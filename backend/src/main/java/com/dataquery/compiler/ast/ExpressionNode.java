package com.dataquery.compiler.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Nodo AST para expresiones: identificadores, números, strings, funciones de agregación.
 * Ejemplos: edad, 18, 'Guatemala', COUNT(*), SUM(precio)
 */
public class ExpressionNode extends ASTNode {

    public enum ExprType { IDENTIFIER, NUMBER, STRING, FUNCTION_CALL, BOOLEAN }

    public final ExprType type;
    public final String   value;

    // Para funciones de agregación: COUNT(*), SUM(col), AVG(col), etc.
    public String functionName = null;
    public String functionArg  = null;  // columna o * como argumento

    public ExpressionNode(ExprType type, String value) {
        this.type  = type;
        this.value = value;
    }

    public static ExpressionNode functionCall(String functionName, String arg) {
        ExpressionNode node = new ExpressionNode(ExprType.FUNCTION_CALL, functionName + "(" + arg + ")");
        node.functionName = functionName;
        node.functionArg  = arg;
        return node;
    }

    @Override
    public void buildString(StringBuilder sb, int indent) {
        String sp = " ".repeat(indent);
        switch (type) {
            case IDENTIFIER:    sb.append(sp).append("Identifier: ").append(value).append("\n"); break;
            case NUMBER:        sb.append(sp).append("Number: ").append(value).append("\n"); break;
            case STRING:        sb.append(sp).append("String: '").append(value).append("'\n"); break;
            case FUNCTION_CALL: sb.append(sp).append("Function: ").append(value).append("\n"); break;
            case BOOLEAN:       sb.append(sp).append("Boolean: ").append(value).append("\n"); break;
        }
    }
}
