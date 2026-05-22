package com.dataquery.compiler.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Nodo AST para UPDATE.
 * Ejemplo: UPDATE usuarios SET nombre = 'Juan', edad = 26 WHERE id = 1
 * ADVERTENCIA: UPDATE sin WHERE afecta TODOS los registros.
 */
public class UpdateNode extends StatementNode {

    public String           tableName;
    public List<SetClause>  setClauses    = new ArrayList<>();
    public ConditionNode    whereCondition = null;

    @Override public String getStatementType() { return "UPDATE"; }

    public String toASTString() {
        StringBuilder sb = new StringBuilder();
        buildString(sb, 0);
        return sb.toString();
    }

    @Override
    public void buildString(StringBuilder sb, int indent) {
        String sp = " ".repeat(indent);
        sb.append(sp).append("UPDATE: ").append(tableName).append("\n");
        sb.append(sp).append("  SET:\n");
        for (SetClause sc : setClauses) sc.buildString(sb, indent + 4);
        if (whereCondition != null) {
            sb.append(sp).append("  WHERE:\n");
            whereCondition.buildString(sb, indent + 4);
        } else {
            sb.append(sp).append("  WHERE: ⚠ NO ESPECIFICADO — AFECTA TODOS LOS REGISTROS\n");
        }
    }
}
