package com.dataquery.compiler.lexer;

public enum TokenType {

    // ─── DQL (consultas) ──────────────────────────────────────────────────
    SELECT, FROM, WHERE, DISTINCT, AS,
    ORDER, BY, ASC, DESC,
    GROUP, HAVING,
    LIMIT, OFFSET,
    TOP, PERCENT, FETCH, NEXT, ONLY, TIES,

    // ─── JOINs ────────────────────────────────────────────────────────────
    JOIN, INNER, LEFT, RIGHT, OUTER, CROSS, FULL, ON,

    // ─── DML (manipulación de datos) ──────────────────────────────────────
    INSERT, INTO, VALUES,
    UPDATE, SET,
    DELETE,

    // ─── DDL (definición de datos) ────────────────────────────────────────
    CREATE, TABLE, DATABASE, INDEX, VIEW,
    ALTER, ADD, COLUMN, MODIFY, RENAME, TO,
    DROP,
    IF, EXISTS,
    TRUNCATE,

    // ─── Constraints / Restricciones ──────────────────────────────────────
    PRIMARY, KEY, FOREIGN, REFERENCES,
    NOT, NULL,
    DEFAULT, UNIQUE, AUTO_INCREMENT, CHECK, CONSTRAINT,

    // ─── Tipos de datos SQL ───────────────────────────────────────────────
    INT_TYPE, VARCHAR_TYPE, FLOAT_TYPE, BOOLEAN_TYPE,
    DATE_TYPE, TEXT_TYPE, BIGINT_TYPE, DECIMAL_TYPE,
    DATETIME_TYPE, TIMESTAMP_TYPE, CHAR_TYPE,
    NVARCHAR_TYPE, TINYINT_TYPE, SMALLINT_TYPE, MONEY_TYPE, REAL_TYPE, SERIAL_TYPE,

    // ─── Procedimientos / Funciones ───────────────────────────────────────
    PROCEDURE, FUNCTION, BEGIN, END, RETURN, CALL, EXEC,
    DECLARE, CURSOR, OPEN, FETCH_KW, CLOSE,
    IN_PARAM, OUT_PARAM, INOUT_PARAM,

    // ─── Transacciones ────────────────────────────────────────────────────
    TRANSACTION, START, COMMIT, ROLLBACK, SAVEPOINT,

    // ─── Backup / Restore ─────────────────────────────────────────────────
    BACKUP, RESTORE, DISK,

    // ─── Operadores lógicos y especiales ──────────────────────────────────
    AND, OR, IN, NOT_IN, LIKE, ILIKE, BETWEEN, IS,
    ANY, ALL, SOME, EXISTS_OP,
    CASE, WHEN, THEN, ELSE,

    // ─── CTEs y operaciones de conjunto ───────────────────────────────────
    WITH, RECURSIVE, UNION, INTERSECT, EXCEPT,

    // ─── Funciones de ventana (Window Functions) ──────────────────────────
    OVER, PARTITION,
    RANK, DENSE_RANK, ROW_NUMBER, NTILE, LAG, LEAD,
    FIRST_VALUE, LAST_VALUE, PERCENT_RANK, CUME_DIST,
    ROWS_KW, RANGE_KW, UNBOUNDED, PRECEDING, FOLLOWING, CURRENT_ROW,
    NULLS, FIRST, LAST,

    // ─── Funciones de agregación ──────────────────────────────────────────
    COUNT, SUM, AVG, MAX_FN, MIN_FN,
    COALESCE, IFNULL, NOW, UPPER, LOWER, LENGTH, CONCAT,

    // ─── Funciones de cadena ──────────────────────────────────────────────
    REPLACE_FN, SUBSTRING_FN, TRIM_FN, LTRIM_FN, RTRIM_FN,
    CHARINDEX_FN, STUFF_FN, FORMAT_FN, STRING_AGG,

    // ─── Funciones matemáticas ────────────────────────────────────────────
    ROUND_FN, ABS_FN, FLOOR_FN, CEILING_FN, POWER_FN, SQRT_FN, MOD_FN,

    // ─── Funciones de fecha ───────────────────────────────────────────────
    GETDATE_FN, DATEADD_FN, DATEDIFF_FN, DATEPART_FN,
    YEAR_FN, MONTH_FN, DAY_FN, CURDATE_FN, SYSDATE_FN,

    // ─── Conversión de tipos ──────────────────────────────────────────────
    CAST, CONVERT, EXTRACT, TRY_CAST, TRY_CONVERT, PARSE,

    // ─── Funciones de nulabilidad ─────────────────────────────────────────
    NULLIF, IIF, NVL, DECODE,

    // ─── Permisos / Administración ────────────────────────────────────────
    GRANT, REVOKE, SHOW, DESCRIBE, USE,

    // ─── Identificadores y literales ──────────────────────────────────────
    IDENTIFIER, NUMBER, STRING, BOOLEAN_LITERAL,

    // ─── Operadores de comparación ────────────────────────────────────────
    EQUAL, GREATER, LESS, GREATER_EQUAL, LESS_EQUAL, NOT_EQUAL,

    // ─── Operadores aritméticos ───────────────────────────────────────────
    PLUS, MINUS, DIVIDE, MODULO,

    // ─── Símbolos especiales ──────────────────────────────────────────────
    ASTERISK, COMMA, SEMICOLON, DOT,
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACKET, RIGHT_BRACKET,
    DOUBLE_COLON,

    // ─── Control ──────────────────────────────────────────────────────────
    END_OF_FILE, INVALID
}
