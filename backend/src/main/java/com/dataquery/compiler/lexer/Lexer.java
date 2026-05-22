package com.dataquery.compiler.lexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Analizador Léxico: convierte texto SQL en una lista de tokens.
 * Reconoce 70+ palabras clave, identificadores, literales y operadores.
 */
public class Lexer {

    private final String source;
    private int position;
    private int line;
    private int column;
    private char currentChar;

    // Mapa de palabras clave → tipo de token
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();
    static {
        // DQL
        KEYWORDS.put("SELECT",        TokenType.SELECT);
        KEYWORDS.put("FROM",          TokenType.FROM);
        KEYWORDS.put("WHERE",         TokenType.WHERE);
        KEYWORDS.put("DISTINCT",      TokenType.DISTINCT);
        KEYWORDS.put("AS",            TokenType.AS);
        KEYWORDS.put("ORDER",         TokenType.ORDER);
        KEYWORDS.put("BY",            TokenType.BY);
        KEYWORDS.put("ASC",           TokenType.ASC);
        KEYWORDS.put("DESC",          TokenType.DESC);
        KEYWORDS.put("GROUP",         TokenType.GROUP);
        KEYWORDS.put("HAVING",        TokenType.HAVING);
        KEYWORDS.put("LIMIT",         TokenType.LIMIT);
        KEYWORDS.put("OFFSET",        TokenType.OFFSET);
        // JOINs
        KEYWORDS.put("JOIN",          TokenType.JOIN);
        KEYWORDS.put("INNER",         TokenType.INNER);
        KEYWORDS.put("LEFT",          TokenType.LEFT);
        KEYWORDS.put("RIGHT",         TokenType.RIGHT);
        KEYWORDS.put("OUTER",         TokenType.OUTER);
        KEYWORDS.put("CROSS",         TokenType.CROSS);
        KEYWORDS.put("ON",            TokenType.ON);
        // DML
        KEYWORDS.put("INSERT",        TokenType.INSERT);
        KEYWORDS.put("INTO",          TokenType.INTO);
        KEYWORDS.put("VALUES",        TokenType.VALUES);
        KEYWORDS.put("UPDATE",        TokenType.UPDATE);
        KEYWORDS.put("SET",           TokenType.SET);
        KEYWORDS.put("DELETE",        TokenType.DELETE);
        // DDL
        KEYWORDS.put("CREATE",        TokenType.CREATE);
        KEYWORDS.put("TABLE",         TokenType.TABLE);
        KEYWORDS.put("DATABASE",      TokenType.DATABASE);
        KEYWORDS.put("INDEX",         TokenType.INDEX);
        KEYWORDS.put("VIEW",          TokenType.VIEW);
        KEYWORDS.put("ALTER",         TokenType.ALTER);
        KEYWORDS.put("ADD",           TokenType.ADD);
        KEYWORDS.put("COLUMN",        TokenType.COLUMN);
        KEYWORDS.put("MODIFY",        TokenType.MODIFY);
        KEYWORDS.put("RENAME",        TokenType.RENAME);
        KEYWORDS.put("TO",            TokenType.TO);
        KEYWORDS.put("DROP",          TokenType.DROP);
        KEYWORDS.put("TRUNCATE",      TokenType.TRUNCATE);
        KEYWORDS.put("IF",            TokenType.IF);
        KEYWORDS.put("EXISTS",        TokenType.EXISTS);
        // Constraints
        KEYWORDS.put("PRIMARY",       TokenType.PRIMARY);
        KEYWORDS.put("KEY",           TokenType.KEY);
        KEYWORDS.put("FOREIGN",       TokenType.FOREIGN);
        KEYWORDS.put("REFERENCES",    TokenType.REFERENCES);
        KEYWORDS.put("NOT",           TokenType.NOT);
        KEYWORDS.put("NULL",          TokenType.NULL);
        KEYWORDS.put("DEFAULT",       TokenType.DEFAULT);
        KEYWORDS.put("UNIQUE",        TokenType.UNIQUE);
        KEYWORDS.put("AUTO_INCREMENT",TokenType.AUTO_INCREMENT);
        KEYWORDS.put("CHECK",         TokenType.CHECK);
        KEYWORDS.put("CONSTRAINT",    TokenType.CONSTRAINT);
        // Tipos de datos SQL
        KEYWORDS.put("INT",           TokenType.INT_TYPE);
        KEYWORDS.put("INTEGER",       TokenType.INT_TYPE);
        KEYWORDS.put("VARCHAR",       TokenType.VARCHAR_TYPE);
        KEYWORDS.put("FLOAT",         TokenType.FLOAT_TYPE);
        KEYWORDS.put("DOUBLE",        TokenType.FLOAT_TYPE);
        KEYWORDS.put("BOOLEAN",       TokenType.BOOLEAN_TYPE);
        KEYWORDS.put("BOOL",          TokenType.BOOLEAN_TYPE);
        KEYWORDS.put("DATE",          TokenType.DATE_TYPE);
        KEYWORDS.put("TEXT",          TokenType.TEXT_TYPE);
        KEYWORDS.put("BIGINT",        TokenType.BIGINT_TYPE);
        KEYWORDS.put("DECIMAL",       TokenType.DECIMAL_TYPE);
        KEYWORDS.put("NUMERIC",       TokenType.DECIMAL_TYPE);
        KEYWORDS.put("DATETIME",      TokenType.DATETIME_TYPE);
        KEYWORDS.put("TIMESTAMP",     TokenType.TIMESTAMP_TYPE);
        KEYWORDS.put("CHAR",          TokenType.CHAR_TYPE);
        // Procedimientos
        KEYWORDS.put("PROCEDURE",     TokenType.PROCEDURE);
        KEYWORDS.put("FUNCTION",      TokenType.FUNCTION);
        KEYWORDS.put("BEGIN",         TokenType.BEGIN);
        KEYWORDS.put("END",           TokenType.END);
        KEYWORDS.put("RETURN",        TokenType.RETURN);
        KEYWORDS.put("CALL",          TokenType.CALL);
        KEYWORDS.put("EXEC",          TokenType.EXEC);
        KEYWORDS.put("EXECUTE",       TokenType.EXEC);
        KEYWORDS.put("DECLARE",       TokenType.DECLARE);
        // Transacciones
        KEYWORDS.put("TRANSACTION",   TokenType.TRANSACTION);
        KEYWORDS.put("START",         TokenType.START);
        KEYWORDS.put("COMMIT",        TokenType.COMMIT);
        KEYWORDS.put("ROLLBACK",      TokenType.ROLLBACK);
        KEYWORDS.put("SAVEPOINT",     TokenType.SAVEPOINT);
        // Backup
        KEYWORDS.put("BACKUP",        TokenType.BACKUP);
        KEYWORDS.put("RESTORE",       TokenType.RESTORE);
        KEYWORDS.put("DISK",          TokenType.DISK);
        // Operadores lógicos/especiales
        KEYWORDS.put("AND",           TokenType.AND);
        KEYWORDS.put("OR",            TokenType.OR);
        KEYWORDS.put("IN",            TokenType.IN);
        KEYWORDS.put("LIKE",          TokenType.LIKE);
        KEYWORDS.put("ILIKE",         TokenType.ILIKE);
        KEYWORDS.put("BETWEEN",       TokenType.BETWEEN);
        KEYWORDS.put("IS",            TokenType.IS);
        KEYWORDS.put("CASE",          TokenType.CASE);
        KEYWORDS.put("WHEN",          TokenType.WHEN);
        KEYWORDS.put("THEN",          TokenType.THEN);
        KEYWORDS.put("ELSE",          TokenType.ELSE);
        // Funciones
        KEYWORDS.put("COUNT",         TokenType.COUNT);
        KEYWORDS.put("SUM",           TokenType.SUM);
        KEYWORDS.put("AVG",           TokenType.AVG);
        KEYWORDS.put("MAX",           TokenType.MAX_FN);
        KEYWORDS.put("MIN",           TokenType.MIN_FN);
        KEYWORDS.put("COALESCE",      TokenType.COALESCE);
        KEYWORDS.put("IFNULL",        TokenType.IFNULL);
        KEYWORDS.put("NOW",           TokenType.NOW);
        KEYWORDS.put("UPPER",         TokenType.UPPER);
        KEYWORDS.put("LOWER",         TokenType.LOWER);
        KEYWORDS.put("LENGTH",        TokenType.LENGTH);
        KEYWORDS.put("CONCAT",        TokenType.CONCAT);
        // Admin
        KEYWORDS.put("GRANT",         TokenType.GRANT);
        KEYWORDS.put("REVOKE",        TokenType.REVOKE);
        KEYWORDS.put("SHOW",          TokenType.SHOW);
        KEYWORDS.put("DESCRIBE",      TokenType.DESCRIBE);
        KEYWORDS.put("USE",           TokenType.USE);
        // Literales booleanos
        KEYWORDS.put("TRUE",          TokenType.BOOLEAN_LITERAL);
        KEYWORDS.put("FALSE",         TokenType.BOOLEAN_LITERAL);
        // CTEs y operaciones de conjunto
        KEYWORDS.put("WITH",          TokenType.WITH);
        KEYWORDS.put("RECURSIVE",     TokenType.RECURSIVE);
        KEYWORDS.put("UNION",         TokenType.UNION);
        KEYWORDS.put("INTERSECT",     TokenType.INTERSECT);
        KEYWORDS.put("EXCEPT",        TokenType.EXCEPT);
        // TOP / paginación avanzada
        KEYWORDS.put("TOP",           TokenType.TOP);
        KEYWORDS.put("PERCENT",       TokenType.PERCENT);
        KEYWORDS.put("FETCH",         TokenType.FETCH);
        KEYWORDS.put("NEXT",          TokenType.NEXT);
        KEYWORDS.put("ONLY",          TokenType.ONLY);
        KEYWORDS.put("TIES",          TokenType.TIES);
        KEYWORDS.put("FIRST",         TokenType.FIRST);
        KEYWORDS.put("LAST",          TokenType.LAST);
        KEYWORDS.put("NULLS",         TokenType.NULLS);
        // JOINs adicionales
        KEYWORDS.put("FULL",          TokenType.FULL);
        // Window Functions
        KEYWORDS.put("OVER",          TokenType.OVER);
        KEYWORDS.put("PARTITION",     TokenType.PARTITION);
        KEYWORDS.put("RANK",          TokenType.RANK);
        KEYWORDS.put("DENSE_RANK",    TokenType.DENSE_RANK);
        KEYWORDS.put("ROW_NUMBER",    TokenType.ROW_NUMBER);
        KEYWORDS.put("NTILE",         TokenType.NTILE);
        KEYWORDS.put("LAG",           TokenType.LAG);
        KEYWORDS.put("LEAD",          TokenType.LEAD);
        KEYWORDS.put("FIRST_VALUE",   TokenType.FIRST_VALUE);
        KEYWORDS.put("LAST_VALUE",    TokenType.LAST_VALUE);
        KEYWORDS.put("PERCENT_RANK",  TokenType.PERCENT_RANK);
        KEYWORDS.put("CUME_DIST",     TokenType.CUME_DIST);
        KEYWORDS.put("ROWS",          TokenType.ROWS_KW);
        KEYWORDS.put("RANGE",         TokenType.RANGE_KW);
        KEYWORDS.put("UNBOUNDED",     TokenType.UNBOUNDED);
        KEYWORDS.put("PRECEDING",     TokenType.PRECEDING);
        KEYWORDS.put("FOLLOWING",     TokenType.FOLLOWING);
        KEYWORDS.put("CURRENT_ROW",   TokenType.CURRENT_ROW);
        // Tipos de datos adicionales
        KEYWORDS.put("NVARCHAR",      TokenType.NVARCHAR_TYPE);
        KEYWORDS.put("NCHAR",         TokenType.NVARCHAR_TYPE);
        KEYWORDS.put("TINYINT",       TokenType.TINYINT_TYPE);
        KEYWORDS.put("SMALLINT",      TokenType.SMALLINT_TYPE);
        KEYWORDS.put("MONEY",         TokenType.MONEY_TYPE);
        KEYWORDS.put("SMALLMONEY",    TokenType.MONEY_TYPE);
        KEYWORDS.put("REAL",          TokenType.REAL_TYPE);
        KEYWORDS.put("SERIAL",        TokenType.SERIAL_TYPE);
        KEYWORDS.put("BIGSERIAL",     TokenType.SERIAL_TYPE);
        KEYWORDS.put("IDENTITY",      TokenType.AUTO_INCREMENT);
        // Funciones de cadena adicionales
        KEYWORDS.put("REPLACE",       TokenType.REPLACE_FN);
        KEYWORDS.put("SUBSTRING",     TokenType.SUBSTRING_FN);
        KEYWORDS.put("SUBSTR",        TokenType.SUBSTRING_FN);
        KEYWORDS.put("TRIM",          TokenType.TRIM_FN);
        KEYWORDS.put("LTRIM",         TokenType.LTRIM_FN);
        KEYWORDS.put("RTRIM",         TokenType.RTRIM_FN);
        KEYWORDS.put("CHARINDEX",     TokenType.CHARINDEX_FN);
        KEYWORDS.put("STRING_AGG",    TokenType.STRING_AGG);
        // Funciones matemáticas
        KEYWORDS.put("ROUND",         TokenType.ROUND_FN);
        KEYWORDS.put("ABS",           TokenType.ABS_FN);
        KEYWORDS.put("FLOOR",         TokenType.FLOOR_FN);
        KEYWORDS.put("CEILING",       TokenType.CEILING_FN);
        KEYWORDS.put("CEIL",          TokenType.CEILING_FN);
        KEYWORDS.put("POWER",         TokenType.POWER_FN);
        KEYWORDS.put("SQRT",          TokenType.SQRT_FN);
        // Funciones de fecha
        KEYWORDS.put("GETDATE",       TokenType.GETDATE_FN);
        KEYWORDS.put("GETUTCDATE",    TokenType.GETDATE_FN);
        KEYWORDS.put("DATEADD",       TokenType.DATEADD_FN);
        KEYWORDS.put("DATEDIFF",      TokenType.DATEDIFF_FN);
        KEYWORDS.put("DATEPART",      TokenType.DATEPART_FN);
        KEYWORDS.put("YEAR",          TokenType.YEAR_FN);
        KEYWORDS.put("MONTH",         TokenType.MONTH_FN);
        KEYWORDS.put("DAY",           TokenType.DAY_FN);
        KEYWORDS.put("CURDATE",       TokenType.CURDATE_FN);
        KEYWORDS.put("SYSDATE",       TokenType.SYSDATE_FN);
        KEYWORDS.put("CURRENT_DATE",  TokenType.CURDATE_FN);
        KEYWORDS.put("CURRENT_TIMESTAMP", TokenType.NOW);
        // Conversión de tipos
        KEYWORDS.put("CAST",          TokenType.CAST);
        KEYWORDS.put("CONVERT",       TokenType.CONVERT);
        KEYWORDS.put("EXTRACT",       TokenType.EXTRACT);
        KEYWORDS.put("TRY_CAST",      TokenType.TRY_CAST);
        KEYWORDS.put("TRY_CONVERT",   TokenType.TRY_CONVERT);
        KEYWORDS.put("NULLIF",        TokenType.NULLIF);
        KEYWORDS.put("IIF",           TokenType.IIF);
        KEYWORDS.put("NVL",           TokenType.NVL);
    }

    public Lexer(String source) {
        this.source      = source;
        this.position    = 0;
        this.line        = 1;
        this.column      = 1;
        this.currentChar = source.isEmpty() ? '\0' : source.charAt(0);
    }

    private void advance() {
        if (currentChar == '\n') { line++; column = 1; } else { column++; }
        position++;
        currentChar = position >= source.length() ? '\0' : source.charAt(position);
    }

    private char peek() {
        int next = position + 1;
        return next >= source.length() ? '\0' : source.charAt(next);
    }

    private void skipWhitespaceAndComments() {
        while (currentChar != '\0' && Character.isWhitespace(currentChar)) advance();
        // Comentario de línea: -- ...
        if (currentChar == '-' && peek() == '-') {
            while (currentChar != '\0' && currentChar != '\n') advance();
            skipWhitespaceAndComments();
        }
        // Comentario de bloque: /* ... */
        if (currentChar == '/' && peek() == '*') {
            advance(); advance();
            while (currentChar != '\0' && !(currentChar == '*' && peek() == '/')) advance();
            if (currentChar != '\0') { advance(); advance(); }
            skipWhitespaceAndComments();
        }
    }

    private Token readIdentifierOrKeyword() {
        int sl = line, sc = column;
        StringBuilder sb = new StringBuilder();
        while (currentChar != '\0' && (Character.isLetterOrDigit(currentChar) || currentChar == '_')) {
            sb.append(currentChar); advance();
        }
        String word = sb.toString();
        TokenType type = KEYWORDS.getOrDefault(word.toUpperCase(), TokenType.IDENTIFIER);
        return new Token(type, word, sl, sc);
    }

    private Token readNumber() {
        int sl = line, sc = column;
        StringBuilder sb = new StringBuilder();
        while (currentChar != '\0' && (Character.isDigit(currentChar) || currentChar == '.')) {
            sb.append(currentChar); advance();
        }
        return new Token(TokenType.NUMBER, sb.toString(), sl, sc);
    }

    private Token readString(char quote) {
        int sl = line, sc = column;
        advance(); // saltar comilla de apertura
        StringBuilder sb = new StringBuilder();
        while (currentChar != '\0' && currentChar != quote) {
            if (currentChar == '\\') { advance(); } // escape
            sb.append(currentChar); advance();
        }
        if (currentChar == quote) advance(); // saltar comilla de cierre
        return new Token(TokenType.STRING, sb.toString(), sl, sc);
    }

    public Token getNextToken() {
        skipWhitespaceAndComments();
        if (currentChar == '\0') return new Token(TokenType.END_OF_FILE, "", line, column);

        int sl = line, sc = column;

        if (Character.isLetter(currentChar) || currentChar == '_') return readIdentifierOrKeyword();
        if (Character.isDigit(currentChar))                         return readNumber();
        if (currentChar == '\'' || currentChar == '"')              return readString(currentChar);

        // Operadores de dos caracteres
        if (currentChar == '>') { advance(); if (currentChar == '=') { advance(); return new Token(TokenType.GREATER_EQUAL, ">=", sl, sc); } return new Token(TokenType.GREATER, ">", sl, sc); }
        if (currentChar == '<') { advance(); if (currentChar == '=') { advance(); return new Token(TokenType.LESS_EQUAL, "<=", sl, sc); } if (currentChar == '>') { advance(); return new Token(TokenType.NOT_EQUAL, "<>", sl, sc); } return new Token(TokenType.LESS, "<", sl, sc); }
        if (currentChar == '!') { advance(); if (currentChar == '=') { advance(); return new Token(TokenType.NOT_EQUAL, "!=", sl, sc); } return new Token(TokenType.INVALID, "!", sl, sc); }

        char ch = currentChar; advance();
        switch (ch) {
            case '=': return new Token(TokenType.EQUAL,         "=",  sl, sc);
            case '*': return new Token(TokenType.ASTERISK,      "*",  sl, sc);
            case ',': return new Token(TokenType.COMMA,         ",",  sl, sc);
            case ';': return new Token(TokenType.SEMICOLON,     ";",  sl, sc);
            case '(': return new Token(TokenType.LEFT_PAREN,    "(",  sl, sc);
            case ')': return new Token(TokenType.RIGHT_PAREN,   ")",  sl, sc);
            case '.': return new Token(TokenType.DOT,           ".",  sl, sc);
            case '+': return new Token(TokenType.PLUS,          "+",  sl, sc);
            case '-': return new Token(TokenType.MINUS,         "-",  sl, sc);
            case '/': return new Token(TokenType.DIVIDE,        "/",  sl, sc);
            case '%': return new Token(TokenType.MODULO,        "%",  sl, sc);
            case '[': return new Token(TokenType.LEFT_BRACKET,  "[",  sl, sc);
            case ']': return new Token(TokenType.RIGHT_BRACKET, "]",  sl, sc);
            default:  return new Token(TokenType.INVALID, String.valueOf(ch), sl, sc);
        }
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        Token token;
        do { token = getNextToken(); tokens.add(token); } while (token.type != TokenType.END_OF_FILE);
        return tokens;
    }
}
