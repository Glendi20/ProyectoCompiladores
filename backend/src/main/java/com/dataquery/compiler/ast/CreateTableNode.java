package com.dataquery.compiler.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Nodo AST para CREATE TABLE.
 * Ejemplo:
 *   CREATE TABLE clientes (
 *     id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 *     nombre VARCHAR(100) NOT NULL,
 *     email VARCHAR(150) UNIQUE
 *   )
 */
public class CreateTableNode extends StatementNode {

    public String                    tableName;
    public boolean                   ifNotExists = false;
    public List<ColumnDefinitionNode> columns    = new ArrayList<>();

    @Override public String getStatementType() { return "CREATE TABLE"; }

    public String toASTString() {
        StringBuilder sb = new StringBuilder();
        buildString(sb, 0);
        return sb.toString();
    }

    @Override
    public void buildString(StringBuilder sb, int indent) {
        String sp = " ".repeat(indent);
        sb.append(sp).append("CREATE TABLE");
        if (ifNotExists) sb.append(" IF NOT EXISTS");
        sb.append(": ").append(tableName).append("\n");
        sb.append(sp).append("  Columns (").append(columns.size()).append("):\n");
        for (ColumnDefinitionNode col : columns) col.buildString(sb, indent + 4);
    }
}
