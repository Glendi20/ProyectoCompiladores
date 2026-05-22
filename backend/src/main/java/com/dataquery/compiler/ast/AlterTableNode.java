package com.dataquery.compiler.ast;

/**
 * Nodo AST para ALTER TABLE.
 * Ejemplos:
 *   ALTER TABLE usuarios ADD COLUMN telefono VARCHAR(20)
 *   ALTER TABLE usuarios DROP COLUMN ciudad
 *   ALTER TABLE usuarios MODIFY COLUMN edad BIGINT
 *   ALTER TABLE usuarios RENAME TO clientes
 */
public class AlterTableNode extends StatementNode {

    public enum AlterType { ADD, DROP, MODIFY, RENAME }

    public String               tableName;
    public AlterType            alterType;
    public ColumnDefinitionNode column     = null;  // para ADD y MODIFY
    public String               columnName = null;  // para DROP
    public String               newName    = null;  // para RENAME

    @Override public String getStatementType() { return "ALTER TABLE"; }

    public String toASTString() {
        StringBuilder sb = new StringBuilder();
        buildString(sb, 0);
        return sb.toString();
    }

    @Override
    public void buildString(StringBuilder sb, int indent) {
        String sp = " ".repeat(indent);
        sb.append(sp).append("ALTER TABLE: ").append(tableName).append("\n");
        switch (alterType) {
            case ADD:
                sb.append(sp).append("  ADD COLUMN:\n");
                if (column != null) column.buildString(sb, indent + 4);
                break;
            case DROP:
                sb.append(sp).append("  DROP COLUMN: ").append(columnName).append("\n");
                break;
            case MODIFY:
                sb.append(sp).append("  MODIFY COLUMN:\n");
                if (column != null) column.buildString(sb, indent + 4);
                break;
            case RENAME:
                sb.append(sp).append("  RENAME TO: ").append(newName).append("\n");
                break;
        }
    }
}
