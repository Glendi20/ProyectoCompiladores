package com.dataquery.service;

import com.dataquery.compiler.ast.*;
import com.dataquery.compiler.symboltable.Table;
import com.dataquery.dto.UserProfile;
import com.dataquery.model.Suggestion;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de optimización y buenas prácticas SQL.
 * Analiza el AST y genera sugerencias educativas según el tipo de sentencia y la BD seleccionada.
 */
@Service
public class OptimizerService {

    public List<Suggestion> analyze(StatementNode ast, String database,
                                     Table resolvedTable, UserProfile profile) {
        List<Suggestion> list = new ArrayList<>();

        if      (ast instanceof SelectNode)      analyzeSelect((SelectNode) ast, database, resolvedTable, list, profile);
        else if (ast instanceof InsertNode)      analyzeInsert((InsertNode) ast, database, list);
        else if (ast instanceof UpdateNode)      analyzeUpdate((UpdateNode) ast, database, list);
        else if (ast instanceof DeleteNode)      analyzeDelete((DeleteNode) ast, database, list);
        else if (ast instanceof CreateTableNode) analyzeCreate((CreateTableNode) ast, database, list);
        else if (ast instanceof AlterTableNode)  analyzeAlter((AlterTableNode) ast, list);
        else if (ast instanceof DropNode)        analyzeDrop((DropNode) ast, list);

        return list;
    }

    // ─── SELECT ─────────────────────────────────────────────────────────────

    private void analyzeSelect(SelectNode n, String db, Table table, List<Suggestion> s, UserProfile profile) {

        // Regla 1: SELECT * — mensaje personalizado según historial
        if (n.selectAll) {
            String example = table != null ? buildSpecificColumns(n, table) : "SELECT id, nombre FROM " + n.tableName;
            if (profile != null && !profile.isNew() && profile.getSelectStarCount() >= 5) {
                s.add(opt("¡Llevas " + profile.getSelectStarCount() + " veces usando SELECT *!",
                    profile.getNombre() + ", este es un patrón repetido en tu historial. " +
                    "SELECT * transfiere todas las columnas aunque no las necesites, consume más memoria " +
                    "y rompe el código si alguien agrega o elimina columnas. Ya es hora de romper este hábito.",
                    example));
            } else if (profile != null && !profile.isNew() && profile.getSelectStarCount() >= 2) {
                s.add(opt("Ya usaste SELECT * " + profile.getSelectStarCount() + " veces",
                    "Vemos que SELECT * aparece con frecuencia en tu historial. " +
                    "Especifica las columnas que realmente necesitas para mejorar el rendimiento.",
                    example));
            } else {
                s.add(opt("Evita SELECT *",
                    "Seleccionar todas las columnas con * transfiere datos innecesarios por la red y consume " +
                    "más memoria. Especifica solo las columnas que tu aplicación necesita.",
                    example));
            }
        }

        // Regla 2: Sin WHERE retorna toda la tabla
        if (n.whereCondition == null && n.joins.isEmpty()) {
            s.add(bp("Agrega una cláusula WHERE",
                "Sin WHERE la consulta escanea TODA la tabla. En tablas con millones de filas puede tardar minutos " +
                "y bloquear otros procesos.",
                "SELECT ... FROM " + n.tableName + " WHERE id = 1"));
        }

        // Regla 3: Sin LIMIT en consultas de mucho volumen
        if (n.limit == null && n.whereCondition == null) {
            s.add(bp("Pagina tus resultados con LIMIT",
                "Retornar miles de filas de golpe sobrecarga la red, la memoria y la UI. " +
                "Usa LIMIT + OFFSET para implementar paginación.",
                buildLimitExample(n, db)));
        }

        // Regla 4: DISTINCT puede ser señal de un JOIN mal hecho
        if (n.distinct) {
            s.add(bp("Revisa el uso de DISTINCT",
                "DISTINCT elimina duplicados DESPUÉS de ejecutar la consulta, lo cual es costoso. " +
                "Si aparecen duplicados, generalmente es señal de un JOIN incorrecto. " +
                "Corrige el JOIN en lugar de usar DISTINCT como parche.",
                "-- Revisa las condiciones ON de tus JOINs"));
        }

        // Regla 5: JOINs sin índice en columna ON
        if (!n.joins.isEmpty()) {
            s.add(opt("Asegura índices en columnas de JOIN",
                "Las columnas usadas en ON (como usuario_id, producto_id) deben tener índice. " +
                "Sin índice, cada JOIN hace un Full Table Scan que puede ser exponencialmente lento.",
                "CREATE INDEX idx_usuario_id ON pedidos(usuario_id);"));
        }

        // Regla 6: GROUP BY sin función de agregación
        if (!n.groupByColumns.isEmpty() && n.columns.stream().noneMatch(c ->
                c.toUpperCase().contains("COUNT") || c.toUpperCase().contains("SUM") ||
                c.toUpperCase().contains("AVG")   || c.toUpperCase().contains("MAX") ||
                c.toUpperCase().contains("MIN"))) {
            s.add(bp("GROUP BY necesita una función de agregación",
                "GROUP BY agrupa filas para calcular totales, promedios, etc. " +
                "Sin una función de agregación (COUNT, SUM, AVG, MAX, MIN) el resultado puede ser inesperado.",
                "SELECT departamento_id, COUNT(*) AS total FROM empleados GROUP BY departamento_id"));
        }

        // Regla 7: ORDER BY sin LIMIT es costoso
        if (!n.orderBy.isEmpty() && n.limit == null) {
            s.add(opt("ORDER BY sin LIMIT ordena toda la tabla",
                "Ordenar millones de filas sin limitarlas primero es muy costoso en memoria y CPU. " +
                "Combina siempre ORDER BY con LIMIT.",
                "SELECT ... ORDER BY fecha DESC LIMIT 20"));
        }

        // ── Reglas avanzadas ─────────────────────────────────────────────

        // Regla 8: Subquery en WHERE → sugerir reescribir como JOIN
        if (hasSubqueryInCondition(n.whereCondition)) {
            s.add(opt("Reescribe la subquery como JOIN",
                "Las subqueries correlacionadas se ejecutan UNA VEZ POR FILA de la tabla exterior. " +
                "Un JOIN con índice puede ser 10-100x más rápido porque el motor lo optimiza globalmente.",
                "-- En vez de:\nSELECT nombre FROM usuarios WHERE id IN (SELECT usuario_id FROM pedidos)\n\n" +
                "-- Usa:\nSELECT DISTINCT u.nombre\nFROM usuarios u\nINNER JOIN pedidos p ON u.id = p.usuario_id"));
        }

        // Regla 9: Window Functions (OVER) sin PARTITION BY
        boolean hasWindowFn = n.columns.stream().anyMatch(c -> c.toUpperCase().contains(" OVER") || c.toUpperCase().startsWith("RANK") || c.toUpperCase().startsWith("ROW_NUMBER") || c.toUpperCase().startsWith("DENSE_RANK"));
        if (hasWindowFn) {
            boolean missingPartition = n.columns.stream()
                .filter(c -> c.toUpperCase().contains(" OVER"))
                .anyMatch(c -> !c.toUpperCase().contains("PARTITION"));
            if (missingPartition) {
                s.add(bp("Window Function sin PARTITION BY escanea toda la tabla",
                    "RANK() OVER (ORDER BY salario) rankea TODAS las filas juntas. " +
                    "Si quieres rankear por grupos (por departamento, por categoría), agrega PARTITION BY. " +
                    "Sin él, la función procesa millones de filas innecesariamente.",
                    "-- Sin PARTITION BY (rankea todo):\nRANK() OVER (ORDER BY salario DESC)\n\n" +
                    "-- Con PARTITION BY (rankea por grupo):\nRANK() OVER (PARTITION BY departamento_id ORDER BY salario DESC)"));
            }
            s.add(opt("Agrega índice en la columna de PARTITION BY",
                "Las window functions con PARTITION BY hacen un sort interno por esa columna. " +
                "Un índice en esa columna puede eliminar el sort y mejorar el rendimiento dramáticamente.",
                "CREATE INDEX idx_depto ON empleados(departamento_id);\n" +
                "-- Luego tu window function usará el índice automáticamente"));
        }

        // Regla 10: CASE WHEN sin ELSE devuelve NULL silenciosamente
        boolean hasCaseWhen = n.columns.stream().anyMatch(c -> c.toUpperCase().contains("CASE ") || c.toUpperCase().startsWith("CASE"));
        if (hasCaseWhen) {
            boolean missingElse = n.columns.stream()
                .filter(c -> c.toUpperCase().contains("CASE ") || c.toUpperCase().startsWith("CASE"))
                .anyMatch(c -> !c.toUpperCase().contains(" ELSE "));
            if (missingElse) {
                s.add(bp("CASE WHEN sin ELSE devuelve NULL",
                    "Si ninguna condición del CASE se cumple y no hay ELSE, SQL devuelve NULL silenciosamente. " +
                    "Esto puede romper cálculos, sumas o comparaciones posteriores sin ningún error visible.",
                    "CASE\n  WHEN salario > 5000 THEN 'Senior'\n  WHEN salario > 3000 THEN 'Mid'\n  ELSE 'Junior'  -- Siempre agrega ELSE\nEND"));
            }
        }

        // Regla 11: UNION → sugerir UNION ALL si no necesita eliminar duplicados
        if (n.hasUnion) {
            s.add(opt("UNION ALL es más rápido que UNION",
                "UNION elimina duplicados haciendo un sort de TODAS las filas combinadas — muy costoso. " +
                "Si sabes que los resultados no tienen duplicados (o no te importan), " +
                "usa UNION ALL que simplemente concatena sin sort.",
                "-- UNION (lento — elimina duplicados):\nSELECT nombre FROM usuarios\nUNION\nSELECT nombre FROM empleados\n\n" +
                "-- UNION ALL (rápido — concatena directo):\nSELECT nombre FROM usuarios\nUNION ALL\nSELECT nombre FROM empleados"));
        }

        // Regla 12: CTE → tips avanzados de rendimiento
        if (n.hasCte) {
            s.add(bp("CTEs: cuidado con la re-ejecución múltiple",
                "En la mayoría de motores (MySQL, SQL Server), si referencias el mismo CTE más de una vez, " +
                "se ejecuta CADA VEZ. Para CTEs costosas referenciadas múltiples veces, usa una tabla temporal. " +
                "En PostgreSQL puedes usar MATERIALIZED para forzar que se ejecute una sola vez.",
                "-- PostgreSQL: forzar materialización\nWITH resumen AS MATERIALIZED (\n  SELECT ...\n)\nSELECT * FROM resumen r1 JOIN resumen r2 ON ...\n\n" +
                "-- SQL Server / MySQL: tabla temporal\nINSERT INTO #temp SELECT ...\nSELECT * FROM #temp r1 JOIN #temp r2 ON ..."));
        }

        // Sugerencia de dialecto
        s.add(buildSelectDialect(n, db));

        // Sugerencia personalizada Tipo 3 basada en historial
        if (profile != null && !profile.isNew()) {
            Suggestion personal = buildPersonalSuggestion(profile);
            if (personal != null) s.add(personal);
        }
    }

    // Detecta si una cadena de condiciones contiene una subquery
    private boolean hasSubqueryInCondition(ConditionNode cond) {
        while (cond != null) {
            if (cond.specialOp == ConditionNode.SpecialOp.IN || cond.specialOp == ConditionNode.SpecialOp.NOT_IN) {
                if (cond.inValues != null && cond.inValues.stream()
                        .anyMatch(v -> "(subquery)".equals(v.value))) return true;
            }
            if (cond.specialOp == ConditionNode.SpecialOp.EXISTS ||
                cond.specialOp == ConditionNode.SpecialOp.NOT_EXISTS) return true;
            if (cond.right != null && "(subquery)".equals(cond.right.value)) return true;
            cond = cond.next;
        }
        return false;
    }

    // ─── INSERT ─────────────────────────────────────────────────────────────

    private void analyzeInsert(InsertNode n, String db, List<Suggestion> s) {

        // Regla: siempre especifica columnas
        if (n.columns.isEmpty()) {
            s.add(opt("Especifica las columnas en INSERT",
                "INSERT sin columnas explícitas rompe si alguien agrega o reordena columnas en la tabla. " +
                "Siempre lista los nombres de columnas.",
                "INSERT INTO " + n.tableName + " (nombre, email) VALUES ('Juan', 'j@mail.com')"));
        }

        // Regla: múltiples filas en un solo INSERT
        if (n.valueRows.size() == 1) {
            s.add(bp("Usa INSERT múltiple para mejor rendimiento",
                "Insertar muchas filas una por una hace un round-trip a la BD por cada INSERT. " +
                "Agrupa los valores en un solo INSERT para reducir el tiempo a la décima parte.",
                "INSERT INTO " + n.tableName + " (col1) VALUES ('a'), ('b'), ('c')"));
        }

        // Regla: envuelve en transacción para múltiples inserts
        s.add(bp("Usa transacciones para operaciones múltiples",
            "Si insertas en varias tablas relacionadas, usa una transacción para garantizar " +
            "que todas las inserciones se completen o ninguna (atomicidad).",
            buildTransactionExample(db)));

        s.add(buildInsertDialect(n, db));
    }

    // ─── UPDATE ─────────────────────────────────────────────────────────────

    private void analyzeUpdate(UpdateNode n, String db, List<Suggestion> s) {

        // CRÍTICO: UPDATE sin WHERE
        if (n.whereCondition == null) {
            s.add(new Suggestion("danger",
                "⚠ PELIGRO: UPDATE sin WHERE",
                "Esta sentencia modificará CADA FILA de la tabla '" + n.tableName + "'. " +
                "Una vez ejecutada, no hay CTRL+Z. Siempre agrega WHERE con la clave primaria.",
                "UPDATE " + n.tableName + " SET ... WHERE id = 1"));
        }

        // Regla: hacer backup antes de UPDATE masivo
        s.add(bp("Haz un SELECT antes de UPDATE",
            "Antes de ejecutar un UPDATE, corre el mismo filtro con SELECT para ver exactamente " +
            "qué registros serán modificados. Evita sorpresas.",
            "-- Primero verifica:\nSELECT * FROM " + n.tableName +
            (n.whereCondition != null ? " WHERE ..." : "") + "\n-- Luego ejecuta el UPDATE"));

        // Regla: transacción para UPDATE crítico
        s.add(bp("Envuelve UPDATEs críticos en transacción",
            "Una transacción te permite hacer ROLLBACK si algo sale mal, recuperando el estado original.",
            buildTransactionExample(db)));

        s.add(buildUpdateDialect(n, db));
    }

    // ─── DELETE ─────────────────────────────────────────────────────────────

    private void analyzeDelete(DeleteNode n, String db, List<Suggestion> s) {

        // CRÍTICO: DELETE sin WHERE
        if (n.whereCondition == null) {
            s.add(new Suggestion("danger",
                "⚠ PELIGRO EXTREMO: DELETE sin WHERE",
                "Esta sentencia BORRARÁ TODOS LOS REGISTROS de '" + n.tableName + "' de forma permanente. " +
                "Usa TRUNCATE TABLE si quieres vaciar la tabla, y solo con backup previo.",
                "-- Para vaciar rápido: TRUNCATE TABLE " + n.tableName + "\n" +
                "-- Para borrar específico: DELETE FROM " + n.tableName + " WHERE id = 1"));
        }

        s.add(bp("Haz backup antes de DELETE",
            "Antes de borrar datos, exporta o respalda la tabla. " +
            "Una vez ejecutado el DELETE sin transacción, los datos son irrecuperables.",
            "-- SQL Server / MySQL:\nSELECT * INTO backup_" + n.tableName + " FROM " + n.tableName));

        s.add(bp("Usa DELETE dentro de una transacción",
            "Envuelve el DELETE en una transacción para poder hacer ROLLBACK si hay algún error.",
            buildTransactionExample(db)));
    }

    // ─── CREATE TABLE ────────────────────────────────────────────────────────

    private void analyzeCreate(CreateTableNode n, String db, List<Suggestion> s) {
        s.add(bp("Nombra tus tablas en plural y minúsculas",
            "La convención estándar es usar nombres en plural, minúsculas y con guiones bajos. " +
            "Ej: usuarios, pedido_items, categorias. Evita mayúsculas y espacios.",
            "CREATE TABLE pedido_items (...)"));

        s.add(opt("Siempre define una PRIMARY KEY con AUTO_INCREMENT",
            "Toda tabla debe tener una columna id como clave primaria. " +
            "AUTO_INCREMENT (MySQL) o SERIAL (PostgreSQL) la genera automáticamente.",
            buildPKExample(db, n.tableName)));

        s.add(bp("Usa IF NOT EXISTS para evitar errores",
            "CREATE TABLE IF NOT EXISTS previene un error si la tabla ya existe. " +
            "Útil en scripts de inicialización que se ejecutan múltiples veces.",
            "CREATE TABLE IF NOT EXISTS " + n.tableName + " (...)"));

        s.add(buildCreateDialect(db));
    }

    // ─── ALTER TABLE ─────────────────────────────────────────────────────────

    private void analyzeAlter(AlterTableNode n, List<Suggestion> s) {
        s.add(bp("Haz backup antes de ALTER TABLE",
            "Modificar la estructura de una tabla en producción es riesgoso. " +
            "Siempre ten un backup y prueba primero en un ambiente de desarrollo.",
            "-- Crea una copia de seguridad antes:\nCREATE TABLE " + n.tableName + "_backup AS SELECT * FROM " + n.tableName));

        s.add(bp("ALTER TABLE puede bloquear la tabla",
            "En tablas grandes, ALTER TABLE puede bloquear todas las operaciones de lectura/escritura " +
            "durante minutos u horas. Planifica la ejecución en horarios de bajo tráfico.",
            "-- Usa herramientas como gh-ost o pt-online-schema-change para tablas grandes"));
    }

    // ─── DROP ────────────────────────────────────────────────────────────────

    private void analyzeDrop(DropNode n, List<Suggestion> s) {
        if (n.dropType == DropNode.DropType.DATABASE) {
            s.add(new Suggestion("danger",
                "⚠ PELIGRO MÁXIMO: DROP DATABASE",
                "Esto elimina TODA la base de datos con todas sus tablas y datos. " +
                "Es IRREVERSIBLE. Asegúrate de tener un backup completo y de estar en el ambiente correcto.",
                "-- Verifica que estás en el ambiente correcto antes de ejecutar"));
        } else {
            s.add(bp("Usa IF EXISTS para evitar errores",
                "DROP TABLE IF EXISTS previene un error si la tabla no existe.",
                "DROP TABLE IF EXISTS " + n.name));
            s.add(bp("Considera TRUNCATE en vez de DROP + CREATE",
                "Si quieres vaciar una tabla pero conservar su estructura, TRUNCATE es más rápido y seguro.",
                "TRUNCATE TABLE " + n.name));
        }
    }

    // ─── Helpers de construcción ─────────────────────────────────────────────

    private Suggestion opt(String title, String msg, String example) {
        return new Suggestion("optimization", title, msg, example);
    }
    private Suggestion bp(String title, String msg, String example) {
        return new Suggestion("best-practice", title, msg, example);
    }

    private String buildSpecificColumns(SelectNode n, Table table) {
        List<String> cols = new ArrayList<>();
        for (var col : table.columns) cols.add(col.name);
        String where = n.whereCondition != null ? " WHERE ..." : "";
        return "SELECT " + String.join(", ", cols) + " FROM " + n.tableName + where;
    }

    private String buildLimitExample(SelectNode n, String db) {
        String cols = n.selectAll ? "*" : String.join(", ", n.columns);
        if ("sqlserver".equalsIgnoreCase(db))
            return "SELECT TOP 20 " + cols + " FROM " + n.tableName;
        return "SELECT " + cols + " FROM " + n.tableName + " LIMIT 20 OFFSET 0";
    }

    private String buildTransactionExample(String db) {
        if ("sqlserver".equalsIgnoreCase(db))
            return "BEGIN TRANSACTION;\n  -- tus sentencias aquí\nCOMMIT;  -- o ROLLBACK si hay error";
        return "START TRANSACTION;\n  -- tus sentencias aquí\nCOMMIT;  -- o ROLLBACK si hay error";
    }

    private String buildPKExample(String db, String tableName) {
        if ("postgresql".equalsIgnoreCase(db))
            return "CREATE TABLE " + tableName + " (\n  id SERIAL PRIMARY KEY,\n  ...);";
        if ("sqlserver".equalsIgnoreCase(db))
            return "CREATE TABLE " + tableName + " (\n  id INT IDENTITY(1,1) PRIMARY KEY,\n  ...);";
        return "CREATE TABLE " + tableName + " (\n  id INT AUTO_INCREMENT PRIMARY KEY,\n  ...);";
    }

    // ─── Sugerencia personalizada Tipo 3 ────────────────────────────────────

    private Suggestion buildPersonalSuggestion(UserProfile p) {
        String nombre = p.getNombre() != null ? p.getNombre().split(" ")[0] : "Usuario";

        // Tendencia en declive — alertar
        if ("DECLINING".equals(p.getTrend())) {
            return new Suggestion("personal",
                nombre + ", tu tasa de éxito bajó esta semana",
                "Tu rendimiento general es " + p.getSuccessRate() + "% pero esta semana bajó a " +
                p.getWeeklySuccessRate() + "%. " +
                (p.getTopError() != null
                    ? "Tu error más reciente fue: \"" + p.getTopError() + "\". Repásalo con calma."
                    : "Revisa los errores recientes en tu Dashboard para identificar el patrón."),
                "-- Abre el Dashboard → Actividad reciente para ver tus últimas queries");
        }

        // Tendencia mejorando — motivar
        if ("IMPROVING".equals(p.getTrend())) {
            return new Suggestion("personal",
                "¡Vas mejorando, " + nombre + "!",
                "Esta semana alcanzaste " + p.getWeeklySuccessRate() + "% de éxito, " +
                "por encima de tu promedio histórico de " + p.getSuccessRate() + "%. " +
                "Sigue practicando — la consistencia es clave para dominar SQL.",
                "-- Sigue así y revisa el Dashboard para ver tu progreso");
        }

        // Tasa de éxito baja — reforzar fundamentos
        if (p.getSuccessRate() > 0 && p.getSuccessRate() < 50) {
            return new Suggestion("personal",
                nombre + ", refuerza los fundamentos SQL",
                "Tu tasa de éxito actual es " + p.getSuccessRate() + "%. " +
                "No te preocupes — es normal al comenzar. " +
                (p.getTopError() != null
                    ? "Concéntrate en este error frecuente: \"" + p.getTopError() + "\"."
                    : "Practica con los ejemplos del editor y revisa la sintaxis básica."),
                "SELECT id, nombre FROM tabla WHERE condicion");
        }

        // Usuario experimentado con buena tasa — desafío avanzado
        if (p.getTotalQueries() >= 20 && p.getSuccessRate() >= 80) {
            return new Suggestion("personal",
                nombre + ", ya dominas lo básico — prueba algo avanzado",
                "Con " + p.getTotalQueries() + " queries y " + p.getSuccessRate() + "% de éxito, " +
                "estás listo para JOINs múltiples, subconsultas y funciones de ventana (OVER/PARTITION BY).",
                "SELECT u.nombre, COUNT(p.id) AS pedidos,\n" +
                "       RANK() OVER (ORDER BY COUNT(p.id) DESC) AS ranking\n" +
                "FROM usuarios u\n" +
                "LEFT JOIN pedidos p ON u.id = p.usuario_id\n" +
                "GROUP BY u.id, u.nombre");
        }

        return null;
    }

    // ─── Sugerencias de dialecto ─────────────────────────────────────────────

    private Suggestion buildSelectDialect(SelectNode n, String db) {
        switch (db.toLowerCase()) {
            case "mysql":
                return new Suggestion("dialect", "Consejo MySQL",
                    "Usa EXPLAIN para ver el plan de ejecución y detectar Full Table Scans. " +
                    "Los backticks (`) permiten usar palabras reservadas como nombres.",
                    "EXPLAIN SELECT * FROM `" + n.tableName + "`;");
            case "postgresql":
                return new Suggestion("dialect", "Consejo PostgreSQL",
                    "Usa EXPLAIN ANALYZE para obtener el plan de ejecución real con tiempos. " +
                    "ILIKE permite búsquedas sin distinción de mayúsculas.",
                    "EXPLAIN ANALYZE SELECT * FROM " + n.tableName + " WHERE nombre ILIKE '%texto%';");
            case "sqlserver":
                return new Suggestion("dialect", "Consejo SQL Server",
                    "Usa TOP en lugar de LIMIT. WITH (NOLOCK) permite lectura sin bloqueo. " +
                    "SET STATISTICS IO ON muestra el uso de disco.",
                    "SELECT TOP 20 * FROM [" + n.tableName + "] WITH (NOLOCK);");
            case "mongodb":
                return new Suggestion("dialect", "Equivalente MongoDB",
                    "MongoDB usa find() o aggregate(). No existe SQL nativo — las consultas son en JSON.",
                    buildMongoQuery(n));
            default:
                return new Suggestion("dialect", "Selecciona una BD", "Elige MySQL, PostgreSQL, SQL Server o MongoDB.", "");
        }
    }

    private Suggestion buildInsertDialect(InsertNode n, String db) {
        if ("mongodb".equalsIgnoreCase(db))
            return new Suggestion("dialect", "Equivalente MongoDB",
                "En MongoDB se usa insertOne() o insertMany() con documentos JSON.",
                "db." + n.tableName + ".insertMany([\n  { nombre: 'Juan', edad: 25 },\n  { nombre: 'Ana', edad: 30 }\n])");
        if ("postgresql".equalsIgnoreCase(db))
            return new Suggestion("dialect", "Consejo PostgreSQL",
                "PostgreSQL soporta RETURNING para obtener los valores insertados, muy útil para obtener el ID generado.",
                "INSERT INTO " + n.tableName + " (nombre) VALUES ('Juan') RETURNING id, nombre;");
        return new Suggestion("dialect", "Consejo " + db,
            "Verifica el AUTO_INCREMENT / IDENTITY generado después del INSERT.",
            "-- MySQL: SELECT LAST_INSERT_ID();\n-- SQL Server: SELECT SCOPE_IDENTITY();");
    }

    private Suggestion buildUpdateDialect(UpdateNode n, String db) {
        if ("postgresql".equalsIgnoreCase(db))
            return new Suggestion("dialect", "Consejo PostgreSQL",
                "PostgreSQL soporta RETURNING en UPDATE para obtener los valores antes y después de la modificación.",
                "UPDATE " + n.tableName + " SET ... WHERE id = 1 RETURNING *;");
        if ("sqlserver".equalsIgnoreCase(db))
            return new Suggestion("dialect", "Consejo SQL Server",
                "SQL Server soporta OUTPUT para obtener los valores modificados.",
                "UPDATE " + n.tableName + " SET ... OUTPUT INSERTED.*, DELETED.* WHERE id = 1;");
        return new Suggestion("dialect", "Consejo MySQL",
            "Usa ROW_COUNT() después del UPDATE para saber cuántas filas fueron afectadas.",
            "UPDATE " + n.tableName + " SET ...; SELECT ROW_COUNT();");
    }

    private Suggestion buildCreateDialect(String db) {
        if ("postgresql".equalsIgnoreCase(db))
            return new Suggestion("dialect", "Consejo PostgreSQL",
                "En PostgreSQL usa SERIAL o BIGSERIAL para auto-incremento, y TEXT en vez de VARCHAR sin límite.",
                "CREATE TABLE ejemplo (\n  id SERIAL PRIMARY KEY,\n  nombre TEXT NOT NULL\n);");
        if ("sqlserver".equalsIgnoreCase(db))
            return new Suggestion("dialect", "Consejo SQL Server",
                "En SQL Server usa IDENTITY(1,1) para auto-incremento y NVARCHAR para soporte Unicode completo.",
                "CREATE TABLE ejemplo (\n  id INT IDENTITY(1,1) PRIMARY KEY,\n  nombre NVARCHAR(100) NOT NULL\n);");
        if ("mongodb".equalsIgnoreCase(db))
            return new Suggestion("dialect", "MongoDB no usa CREATE TABLE",
                "MongoDB es schema-less — los documentos se crean al insertar. " +
                "Usa createCollection() solo si necesitas opciones especiales.",
                "db.createCollection('ejemplo', { validator: { $jsonSchema: { ... } } })");
        return new Suggestion("dialect", "Consejo MySQL",
            "Usa ENGINE=InnoDB para soporte de transacciones y foreign keys. Especifica el charset.",
            "CREATE TABLE ejemplo (\n  id INT AUTO_INCREMENT PRIMARY KEY\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
    }

    // ─── Conversión MongoDB ───────────────────────────────────────────────────

    private String buildMongoQuery(SelectNode n) {
        StringBuilder sb = new StringBuilder("db.").append(n.tableName).append(".find(\n");
        if (n.whereCondition != null && n.whereCondition.left != null && n.whereCondition.right != null) {
            sb.append("  { ").append(n.whereCondition.left.value)
              .append(": { ").append(toMongoOp(n.whereCondition.op))
              .append(": ").append(fmtMongo(n.whereCondition.right)).append(" } }");
        } else {
            sb.append("  {}");
        }
        if (!n.selectAll && !n.columns.isEmpty()) {
            sb.append(",\n  { ");
            for (int i = 0; i < n.columns.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(n.columns.get(i)).append(": 1");
            }
            sb.append(" }");
        }
        sb.append("\n)");
        if (n.limit != null) sb.append(".limit(").append(n.limit).append(")");
        if (n.orderBy.size() == 1)
            sb.append(".sort({ ").append(n.orderBy.get(0).column)
              .append(": ").append(n.orderBy.get(0).ascending ? "1" : "-1").append(" })");
        return sb.toString();
    }

    private String toMongoOp(CompOperator op) {
        if (op == null) return "$eq";
        switch (op) {
            case EQUAL:         return "$eq";
            case GREATER:       return "$gt";
            case LESS:          return "$lt";
            case GREATER_EQUAL: return "$gte";
            case LESS_EQUAL:    return "$lte";
            case NOT_EQUAL:     return "$ne";
            default:            return "$eq";
        }
    }

    private String fmtMongo(ExpressionNode e) {
        return e.type == ExpressionNode.ExprType.STRING ? "\"" + e.value + "\"" : e.value;
    }
}
