package com.dataquery.compiler.ast;

/**
 * Nodo AST para DROP TABLE, DROP INDEX, DROP DATABASE.
 * Ejemplos:
 *   DROP TABLE usuarios
 *   DROP TABLE IF EXISTS temporal
 *   DROP INDEX idx_nombre
 *   DROP DATABASE ventas
 */
public class DropNode extends StatementNode {

    public enum DropType { TABLE, INDEX, DATABASE }

    public DropType dropType;
    public String   name;
    public boolean  ifExists = false;

    @Override public String getStatementType() { return "DROP " + dropType.name(); }

    public String toASTString() {
        StringBuilder sb = new StringBuilder();
        buildString(sb, 0);
        return sb.toString();
    }

    @Override
    public void buildString(StringBuilder sb, int indent) {
        String sp = " ".repeat(indent);
        sb.append(sp).append("DROP ").append(dropType.name());
        if (ifExists) sb.append(" IF EXISTS");
        sb.append(": ").append(name).append("\n");
    }
}
