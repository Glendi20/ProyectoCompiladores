package com.dataquery.compiler.semantic;

import com.dataquery.compiler.ast.*;
import com.dataquery.compiler.symboltable.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Analizador Semántico: valida que la consulta tenga sentido lógico.
 * Verifica tablas, columnas, tipos de datos y detecta operaciones peligrosas.
 */
public class SemanticAnalyzer {

    private final SymbolTable  symbolTable;
    private final List<String> errors    = new ArrayList<>();
    private final List<String> warnings  = new ArrayList<>();
    private final List<String> cteNames  = new ArrayList<>();

    public SemanticAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public void addCteNames(List<String> names) {
        if (names != null) cteNames.addAll(names);
    }

    // ─── Punto de entrada ───────────────────────────────────────────────────

    public boolean analyze(StatementNode ast) {
        if (ast == null) { errors.add("Sentencia vacía"); return false; }
        errors.clear();
        warnings.clear();

        if      (ast instanceof SelectNode)      analyzeSelect((SelectNode) ast);
        else if (ast instanceof InsertNode)      analyzeInsert((InsertNode) ast);
        else if (ast instanceof UpdateNode)      analyzeUpdate((UpdateNode) ast);
        else if (ast instanceof DeleteNode)      analyzeDelete((DeleteNode) ast);
        else if (ast instanceof CreateTableNode) analyzeCreate((CreateTableNode) ast);
        else if (ast instanceof AlterTableNode)  analyzeAlter((AlterTableNode) ast);
        else if (ast instanceof DropNode)        analyzeDrop((DropNode) ast);

        return errors.isEmpty();
    }

    // ─── SELECT ─────────────────────────────────────────────────────────────

    private void analyzeSelect(SelectNode node) {
        Table table = requireTable(node.tableName);

        if (!node.selectAll) {
            for (String col : node.columns) {
                String clean = stripAlias(col);
                if (!clean.contains(".") && !clean.contains("("))
                    requireColumn(clean, table, node.tableName);
            }
        }

        // Validar JOINs
        for (JoinNode join : node.joins) {
            Table joinTable = requireTable(join.tableName);
            if (join.onCondition != null) validateConditionTypes(join.onCondition, table, joinTable);
        }

        // Validar WHERE
        if (node.whereCondition != null) validateConditionChain(node.whereCondition, table);

        // Validar GROUP BY
        for (String g : node.groupByColumns)
            requireColumn(g, table, node.tableName);

        // Validar HAVING
        if (node.havingCondition != null) validateConditionChain(node.havingCondition, table);

        // Validar ORDER BY — ignorar aliases definidos en el SELECT
        java.util.Set<String> selectAliases = new java.util.HashSet<>();
        for (String col : node.columns) {
            int asIdx = col.toUpperCase().lastIndexOf(" AS ");
            if (asIdx >= 0) selectAliases.add(col.substring(asIdx + 4).trim().toLowerCase());
        }
        for (OrderByClause o : node.orderBy) {
            String col = o.column.contains(".") ? o.column.split("\\.")[1] : o.column;
            if (!selectAliases.contains(col.toLowerCase()))
                requireColumn(o.column, table, node.tableName);
        }
    }

    // ─── INSERT ─────────────────────────────────────────────────────────────

    private void analyzeInsert(InsertNode node) {
        Table table = requireTable(node.tableName);
        if (table == null) return;

        // Validar que las columnas especificadas existen
        for (String col : node.columns)
            requireColumn(col, table, node.tableName);

        // Validar que el número de valores coincide con el número de columnas
        if (!node.columns.isEmpty()) {
            for (int r = 0; r < node.valueRows.size(); r++) {
                List<ExpressionNode> row = node.valueRows.get(r);
                if (row.size() != node.columns.size()) {
                    errors.add("Fila " + (r + 1) + ": se esperaban " + node.columns.size() +
                               " valores pero se encontraron " + row.size());
                }
            }
        }

        // Advertencia: insertar sin especificar columnas es mala práctica
        if (node.columns.isEmpty()) {
            warnings.add("Buena práctica: especifica los nombres de columnas en INSERT INTO para evitar errores si la estructura de la tabla cambia");
        }
    }

    // ─── UPDATE ─────────────────────────────────────────────────────────────

    private void analyzeUpdate(UpdateNode node) {
        Table table = requireTable(node.tableName);

        // PELIGRO CRÍTICO: UPDATE sin WHERE
        if (node.whereCondition == null) {
            errors.add("PELIGRO CRÍTICO: UPDATE sin WHERE afectará TODOS los registros de '" +
                       node.tableName + "'. Agrega una cláusula WHERE para limitar el alcance");
        }

        // Validar columnas en SET
        for (SetClause sc : node.setClauses)
            requireColumn(sc.column, table, node.tableName);

        // Validar WHERE
        if (node.whereCondition != null) validateConditionChain(node.whereCondition, table);
    }

    // ─── DELETE ─────────────────────────────────────────────────────────────

    private void analyzeDelete(DeleteNode node) {
        Table table = requireTable(node.tableName);

        // PELIGRO CRÍTICO: DELETE sin WHERE
        if (node.whereCondition == null) {
            errors.add("PELIGRO CRÍTICO: DELETE sin WHERE eliminará TODOS los registros de '" +
                       node.tableName + "'. Usa TRUNCATE si quieres vaciar la tabla completa, o agrega WHERE");
        }

        if (node.whereCondition != null) validateConditionChain(node.whereCondition, table);
    }

    // ─── CREATE TABLE ────────────────────────────────────────────────────────

    private void analyzeCreate(CreateTableNode node) {
        // Advertir si la tabla ya existe en el schema
        if (symbolTable.findTable(node.tableName) != null && !node.ifNotExists) {
            warnings.add("La tabla '" + node.tableName + "' ya existe en el schema. Considera usar IF NOT EXISTS");
        }

        // Verificar que haya al menos una columna
        if (node.columns.isEmpty()) {
            errors.add("CREATE TABLE debe definir al menos una columna");
            return;
        }

        // Verificar que haya PRIMARY KEY
        boolean hasPK = node.columns.stream().anyMatch(c -> c.primaryKey);
        if (!hasPK) {
            warnings.add("Buena práctica: define una columna PRIMARY KEY en la tabla '" + node.tableName + "'");
        }

        // Detectar nombres de columna duplicados
        List<String> colNames = new ArrayList<>();
        for (ColumnDefinitionNode col : node.columns) {
            if (colNames.contains(col.name.toLowerCase())) {
                errors.add("Columna duplicada: '" + col.name + "' aparece más de una vez en CREATE TABLE");
            }
            colNames.add(col.name.toLowerCase());
        }
    }

    // ─── ALTER TABLE ─────────────────────────────────────────────────────────

    private void analyzeAlter(AlterTableNode node) {
        Table table = requireTable(node.tableName);
        if (table == null) return;

        switch (node.alterType) {
            case DROP:
                if (table.findColumn(node.columnName) == null)
                    errors.add("La columna '" + node.columnName + "' no existe en '" + node.tableName + "'");
                warnings.add("Eliminar columnas es una operación irreversible. Asegúrate de tener un backup");
                break;
            case RENAME:
                warnings.add("Renombrar una tabla puede romper queries, vistas o procedimientos existentes");
                break;
            case MODIFY:
                if (node.column != null && table.findColumn(node.column.name) == null)
                    errors.add("La columna '" + node.column.name + "' no existe en '" + node.tableName + "'");
                break;
            default:
                break;
        }
    }

    // ─── DROP ────────────────────────────────────────────────────────────────

    private void analyzeDrop(DropNode node) {
        String name = node.name;
        if (name != null && name.startsWith("BACKUP")) return;
        if (name != null && (name.startsWith("PROCEDURE") || name.startsWith("FUNCTION"))) return;

        if (node.dropType == DropNode.DropType.TABLE) {
            if (symbolTable.findTable(name) == null && !node.ifExists) {
                warnings.add("La tabla '" + name + "' no existe en el schema conocido");
            }
            warnings.add("DROP TABLE es irreversible. Considera hacer BACKUP antes de eliminar datos");
        }
        if (node.dropType == DropNode.DropType.DATABASE) {
            errors.add("DROP DATABASE es una operación EXTREMADAMENTE PELIGROSA. Elimina toda la base de datos");
        }
    }

    // ─── Validaciones de condiciones ─────────────────────────────────────────

    private void validateConditionChain(ConditionNode node, Table table) {
        ConditionNode c = node;
        while (c != null) {
            validateConditionTypes(c, table, null);
            c = c.next;
        }
    }

    private void validateConditionTypes(ConditionNode c, Table primary, Table secondary) {
        if (c == null || c.left == null) return;

        if (c.specialOp == ConditionNode.SpecialOp.IS_NULL ||
            c.specialOp == ConditionNode.SpecialOp.IS_NOT_NULL) {
            requireColumnInAnyTable(c.left.value, primary, secondary);
            return;
        }

        if (c.specialOp == ConditionNode.SpecialOp.IN ||
            c.specialOp == ConditionNode.SpecialOp.NOT_IN) {
            requireColumnInAnyTable(c.left.value, primary, secondary);
            return;
        }

        if (c.specialOp == ConditionNode.SpecialOp.BETWEEN) {
            requireColumnInAnyTable(c.left.value, primary, secondary);
            return;
        }

        if (c.left == null || c.right == null || c.op == null) return;

        DataType leftType  = resolveType(c.left,  primary, secondary);
        DataType rightType = resolveType(c.right, primary, secondary);

        if (leftType != null && rightType != null && !areCompatible(leftType, rightType)) {
            errors.add("Tipos incompatibles en condición: " +
                       leftType.dataTypeToString() + " " +
                       ConditionNode.operatorToString(c.op) + " " +
                       rightType.dataTypeToString());
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Table requireTable(String name) {
        if (name == null || name.isBlank()) return null;
        // Ignorar nombres especiales y derivados
        if (name.startsWith("(") || name.equalsIgnoreCase("(subquery)")) return null;
        if (name.startsWith("BACKUP") || name.startsWith("TRUNCATE") ||
            name.startsWith("PROCEDURE") || name.startsWith("FUNCTION") ||
            name.startsWith("VIEW")   || name.startsWith("CREATE") ||
            name.startsWith("CALL")   || name.startsWith("EXEC") ||
            name.contains("TRANSACTION")) return null;
        // Ignorar CTEs definidos con WITH
        if (cteNames.contains(name.toLowerCase())) return null;

        Table t = symbolTable.findTable(name);
        if (t == null)
            warnings.add("La tabla '" + name + "' no está en el schema de ejemplo. " +
                         "Si es una CTE o tabla real de tu BD, la consulta puede ser válida. " +
                         "Tablas del schema: " + symbolTable.getAvailableTables());
        return t;
    }

    private void requireColumn(String colName, Table table, String tableName) {
        if (table == null || colName == null) return;
        String clean = stripAlias(colName);
        if (clean.contains(".")) return;  // tabla.columna
        if (clean.contains("(")) return;  // función: COUNT(x), SUM(x)
        if (clean.contains(" ")) return;  // expresión compleja: CASE WHEN, window fn, aritmética
        if (clean.startsWith("*"))  return;
        if (table.findColumn(clean) == null)
            errors.add("La columna '" + clean + "' no existe en la tabla '" + tableName + "'");
    }

    private void requireColumnInAnyTable(String colName, Table t1, Table t2) {
        if (colName == null) return;
        if (colName.contains(".") || colName.contains("(") || colName.contains(" ")) return;
        if (colName.startsWith("*")) return;
        boolean found = (t1 != null && t1.findColumn(colName) != null)
                     || (t2 != null && t2.findColumn(colName) != null);
        if (!found && t1 != null)
            errors.add("La columna '" + colName + "' no existe en las tablas del JOIN");
    }

    private DataType resolveType(ExpressionNode expr, Table t1, Table t2) {
        if (expr == null) return null;
        if (expr.type == ExpressionNode.ExprType.NUMBER)  return DataType.INT;
        if (expr.type == ExpressionNode.ExprType.STRING)  return DataType.VARCHAR;
        if (expr.type == ExpressionNode.ExprType.BOOLEAN) return DataType.INT;
        if (expr.type == ExpressionNode.ExprType.FUNCTION_CALL) return DataType.INT;

        String name = expr.value;
        if (name.contains(".")) name = name.substring(name.lastIndexOf('.') + 1);

        Column col = t1 != null ? t1.findColumn(name) : null;
        if (col == null && t2 != null) col = t2.findColumn(name);
        return col != null ? col.type : null;
    }

    private boolean areCompatible(DataType a, DataType b) {
        if (a == b) return true;
        return (a == DataType.INT || a == DataType.FLOAT) && (b == DataType.INT || b == DataType.FLOAT);
    }

    private String stripAlias(String col) {
        int idx = col.toUpperCase().indexOf(" AS ");
        return idx >= 0 ? col.substring(0, idx).trim() : col.trim();
    }

    // ─── Accesores ───────────────────────────────────────────────────────────

    public List<String> getErrors()   { return errors; }
    public List<String> getWarnings() { return warnings; }
}
