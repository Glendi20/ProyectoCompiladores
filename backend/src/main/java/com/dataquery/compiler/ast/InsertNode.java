package com.dataquery.compiler.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Nodo AST para INSERT INTO.
 * Ejemplo: INSERT INTO usuarios (nombre, edad) VALUES ('Juan', 25)
 * También soporta múltiples filas: VALUES ('Juan', 25), ('Ana', 30)
 */
public class InsertNode extends StatementNode {

    public String              tableName;
    public List<String>        columns   = new ArrayList<>();  // puede estar vacío (sin especificar columnas)
    public List<List<ExpressionNode>> valueRows = new ArrayList<>(); // múltiples filas

    @Override public String getStatementType() { return "INSERT"; }

    public String toASTString() {
        StringBuilder sb = new StringBuilder();
        buildString(sb, 0);
        return sb.toString();
    }

    @Override
    public void buildString(StringBuilder sb, int indent) {
        String sp = " ".repeat(indent);
        sb.append(sp).append("INSERT INTO: ").append(tableName).append("\n");
        if (!columns.isEmpty())
            sb.append(sp).append("  Columns: ").append(String.join(", ", columns)).append("\n");
        sb.append(sp).append("  Values (").append(valueRows.size()).append(" row(s)):\n");
        for (int r = 0; r < valueRows.size(); r++) {
            sb.append(sp).append("    Row ").append(r + 1).append(": (");
            List<ExpressionNode> row = valueRows.get(r);
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(row.get(i).value);
            }
            sb.append(")\n");
        }
    }
}
