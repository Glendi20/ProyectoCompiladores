package com.dataquery.compiler.ast;

/**
 * Nodo AST para DELETE FROM.
 * Ejemplo: DELETE FROM usuarios WHERE id = 5
 * ADVERTENCIA: DELETE sin WHERE elimina TODOS los registros de la tabla.
 */
public class DeleteNode extends StatementNode {

    public String        tableName;
    public ConditionNode whereCondition = null;

    @Override public String getStatementType() { return "DELETE"; }

    public String toASTString() {
        StringBuilder sb = new StringBuilder();
        buildString(sb, 0);
        return sb.toString();
    }

    @Override
    public void buildString(StringBuilder sb, int indent) {
        String sp = " ".repeat(indent);
        sb.append(sp).append("DELETE FROM: ").append(tableName).append("\n");
        if (whereCondition != null) {
            sb.append(sp).append("  WHERE:\n");
            whereCondition.buildString(sb, indent + 4);
        } else {
            sb.append(sp).append("  WHERE: ⚠ NO ESPECIFICADO — ELIMINA TODOS LOS REGISTROS\n");
        }
    }
}
