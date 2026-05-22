package com.dataquery.compiler.parser;

import com.dataquery.compiler.ast.*;
import com.dataquery.compiler.lexer.Token;
import com.dataquery.compiler.lexer.TokenType;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser recursivo descendente — soporta SQL completo:
 * SELECT (subqueries, CTEs, window functions, UNION, CASE WHEN, aritmética),
 * INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, TRUNCATE, transacciones y más.
 */
public class Parser {

    private final List<Token> tokens;
    private int   position;
    private Token current;
    private final List<String> cteNames = new ArrayList<>();

    public Parser(List<Token> tokens) {
        this.tokens   = tokens;
        this.position = 0;
        this.current  = tokens.isEmpty() ? null : tokens.get(0);
    }

    // ─── Helpers básicos ────────────────────────────────────────────────────

    private void advance() {
        position++;
        if (position < tokens.size()) current = tokens.get(position);
    }

    private boolean check(TokenType t) { return current != null && current.type == t; }

    private boolean checkAny(TokenType... types) {
        for (TokenType t : types) if (check(t)) return true;
        return false;
    }

    private Token consume(TokenType t) {
        if (!check(t)) error("Se esperaba '" + t.name() + "' pero se encontró '" +
                             (current != null ? current.value : "EOF") + "'");
        Token tok = current; advance(); return tok;
    }

    private Token consumeIdentifier() {
        if (current == null) error("Se esperaba un identificador");
        Token tok = current; advance(); return tok;
    }

    private boolean optional(TokenType t) {
        if (check(t)) { advance(); return true; } return false;
    }

    private void error(String msg) {
        String pos = current != null ? " (línea " + current.line + ", col " + current.column + ")" : "";
        throw new RuntimeException("Error de sintaxis" + pos + ": " + msg);
    }

    private void skipToSemicolonOrEOF() {
        while (!check(TokenType.SEMICOLON) && !check(TokenType.END_OF_FILE)) advance();
        optional(TokenType.SEMICOLON);
    }

    // ─── Método estrella: consumir tokens como string ────────────────────────

    private String consumeTokensAsString(boolean stopAtComma) {
        StringBuilder sb = new StringBuilder();
        int parenDepth = 0;
        int caseDepth  = 0;

        while (current != null && !check(TokenType.END_OF_FILE)) {
            TokenType t = current.type;

            if (t == TokenType.CASE) caseDepth++;
            if (t == TokenType.END && caseDepth > 0) {
                caseDepth--;
                appendToken(sb, "END");
                advance();
                continue;
            }

            if (parenDepth == 0 && caseDepth == 0) {
                if (stopAtComma && t == TokenType.COMMA) break;
                if (t == TokenType.SEMICOLON)            break;
                if (isMainClause(t))                     break;
            }

            if (t == TokenType.RIGHT_PAREN) {
                if (parenDepth == 0) break;
                parenDepth--;
                sb.append(")");
                advance();
                continue;
            }

            if (t == TokenType.LEFT_PAREN) parenDepth++;

            appendToken(sb, current.value);
            advance();
        }
        return sb.toString().trim();
    }

    private void appendToken(StringBuilder sb, String val) {
        if (sb.length() == 0) { sb.append(val); return; }
        char last = sb.charAt(sb.length() - 1);
        if (val.equals(")") || val.equals(",") || val.equals(".")) { sb.append(val); return; }
        if (last == '(' || last == '.') { sb.append(val); return; }
        sb.append(" ").append(val);
    }

    private boolean isMainClause(TokenType t) {
        switch (t) {
            case FROM: case WHERE: case GROUP: case HAVING: case ORDER:
            case LIMIT: case OFFSET: case UNION: case INTERSECT: case EXCEPT:
            case JOIN: case INNER: case LEFT: case RIGHT: case FULL:
            case CROSS: case OUTER: case ON: case FETCH:
                return true;
            default: return false;
        }
    }

    // ─── Saltar bloque entre paréntesis ──────────────────────────────────────

    private void skipEnclosedBlock() {
        if (!check(TokenType.LEFT_PAREN)) return;
        advance();
        int depth = 1;
        while (!check(TokenType.END_OF_FILE)) {
            if (check(TokenType.LEFT_PAREN))  depth++;
            else if (check(TokenType.RIGHT_PAREN)) {
                depth--;
                if (depth == 0) { advance(); return; }
            }
            advance();
        }
    }

    // ─── Punto de entrada ────────────────────────────────────────────────────

    public StatementNode parse() {
        boolean withFound = false;
        if (check(TokenType.WITH)) { parseWithClauses(); withFound = true; }

        StatementNode node = parseStatement();
        if (withFound && node instanceof SelectNode) ((SelectNode) node).hasCte = true;

        while (checkAny(TokenType.UNION, TokenType.INTERSECT, TokenType.EXCEPT)) {
            if (node instanceof SelectNode) ((SelectNode) node).hasUnion = true;
            advance();
            if (check(TokenType.ALL) || (current != null && "ALL".equalsIgnoreCase(current.value))) advance();
            optional(TokenType.DISTINCT);
            if (check(TokenType.SELECT)) parseSelect();
        }

        optional(TokenType.SEMICOLON);
        return node;
    }

    // ─── WITH / CTE ──────────────────────────────────────────────────────────

    private void parseWithClauses() {
        consume(TokenType.WITH);
        optional(TokenType.RECURSIVE);
        do {
            String cteName = consumeIdentifier().value;
            cteNames.add(cteName.toLowerCase());
            // Lista de columnas opcional: nombre(col1, col2)
            if (check(TokenType.LEFT_PAREN) && !nextIsSelect()) skipEnclosedBlock();
            consume(TokenType.AS);
            skipEnclosedBlock();
        } while (optional(TokenType.COMMA));
    }

    private boolean nextIsSelect() {
        int pos = position + 1;
        while (pos < tokens.size()) {
            TokenType t = tokens.get(pos).type;
            if (t == TokenType.SELECT)     return true;
            if (t == TokenType.IDENTIFIER || t == TokenType.COMMA) { pos++; continue; }
            break;
        }
        return false;
    }

    // ─── Dispatcher ──────────────────────────────────────────────────────────

    private StatementNode parseStatement() {
        if (current == null || check(TokenType.END_OF_FILE)) error("La consulta está vacía");
        switch (current.type) {
            case SELECT:   return parseSelect();
            case INSERT:   return parseInsert();
            case UPDATE:   return parseUpdate();
            case DELETE:   return parseDelete();
            case CREATE:   return parseCreate();
            case ALTER:    return parseAlter();
            case DROP:     return parseDrop();
            case TRUNCATE: return parseTruncate();
            case BACKUP:   return parseBackup();
            case START: case TRANSACTION: case BEGIN: case COMMIT: case ROLLBACK:
                return parseTransaction();
            case CALL: case EXEC:
                return parseCallProcedure();
            default:
                error("Sentencia no reconocida: '" + current.value + "'");
                return null;
        }
    }

    // ─── SELECT ──────────────────────────────────────────────────────────────

    private SelectNode parseSelect() {
        SelectNode node = new SelectNode();
        consume(TokenType.SELECT);

        if (optional(TokenType.DISTINCT)) node.distinct = true;

        // SQL Server: TOP N [PERCENT] [WITH TIES]
        if (check(TokenType.TOP)) {
            advance();
            if (check(TokenType.NUMBER)) node.limit = Integer.parseInt(consume(TokenType.NUMBER).value);
            optional(TokenType.PERCENT);
            if (current != null && "WITH".equalsIgnoreCase(current.value)) { advance(); optional(TokenType.TIES); }
        }

        parseSelectColumns(node);
        consume(TokenType.FROM);

        // Tabla principal o subquery derivada: (SELECT ...) AS alias
        if (check(TokenType.LEFT_PAREN)) {
            skipEnclosedBlock();
            node.tableName = "(subquery)";
        } else {
            node.tableName = consumeIdentifier().value;
        }

        // Alias de tabla
        if (optional(TokenType.AS)) {
            node.tableAlias = consumeIdentifier().value;
        } else if (current != null && current.type == TokenType.IDENTIFIER && !isReservedWord(current.type)) {
            node.tableAlias = consumeIdentifier().value;
        }

        // JOINs múltiples
        while (checkAny(TokenType.JOIN, TokenType.INNER, TokenType.LEFT, TokenType.RIGHT,
                        TokenType.CROSS, TokenType.OUTER, TokenType.FULL)) {
            node.joins.add(parseJoin());
        }

        if (optional(TokenType.WHERE))   node.whereCondition   = parseConditionChain();

        if (check(TokenType.GROUP)) {
            advance(); consume(TokenType.BY);
            node.groupByColumns.add(consumeTokensAsString(true));
            while (optional(TokenType.COMMA)) node.groupByColumns.add(consumeTokensAsString(true));
        }

        if (optional(TokenType.HAVING))  node.havingCondition  = parseConditionChain();

        if (check(TokenType.ORDER)) {
            advance(); consume(TokenType.BY);
            node.orderBy.add(parseOrderByClause());
            while (optional(TokenType.COMMA)) node.orderBy.add(parseOrderByClause());
        }

        if (optional(TokenType.LIMIT)) {
            if (check(TokenType.NUMBER)) node.limit = Integer.parseInt(consume(TokenType.NUMBER).value);
        }
        if (optional(TokenType.OFFSET)) {
            if (check(TokenType.NUMBER)) node.offset = Integer.parseInt(consume(TokenType.NUMBER).value);
        }

        // FETCH FIRST / NEXT N ROWS ONLY (SQL estándar ISO)
        if (check(TokenType.FETCH)) {
            advance();
            optional(TokenType.NEXT); optional(TokenType.FIRST);
            if (check(TokenType.NUMBER)) node.limit = Integer.parseInt(consume(TokenType.NUMBER).value);
            if (current != null && "ROWS".equalsIgnoreCase(current.value)) advance();
            if (current != null && "ROW".equalsIgnoreCase(current.value))  advance();
            optional(TokenType.ONLY);
        }

        return node;
    }

    private void parseSelectColumns(SelectNode node) {
        if (check(TokenType.ASTERISK)) { node.selectAll = true; advance(); return; }
        node.columns.add(consumeTokensAsString(true));
        while (optional(TokenType.COMMA)) {
            if (check(TokenType.ASTERISK)) { node.selectAll = true; advance(); break; }
            node.columns.add(consumeTokensAsString(true));
        }
    }

    // ─── JOIN ─────────────────────────────────────────────────────────────────

    private JoinNode parseJoin() {
        JoinNode.JoinType joinType = JoinNode.JoinType.INNER;

        if (check(TokenType.FULL))  {
            advance(); optional(TokenType.OUTER);
            joinType = JoinNode.JoinType.FULL;  consume(TokenType.JOIN);
        } else if (check(TokenType.INNER))  {
            advance(); joinType = JoinNode.JoinType.INNER; optional(TokenType.JOIN);
        } else if (check(TokenType.LEFT))   {
            advance(); optional(TokenType.OUTER); joinType = JoinNode.JoinType.LEFT;  consume(TokenType.JOIN);
        } else if (check(TokenType.RIGHT))  {
            advance(); optional(TokenType.OUTER); joinType = JoinNode.JoinType.RIGHT; consume(TokenType.JOIN);
        } else if (check(TokenType.CROSS))  {
            advance(); joinType = JoinNode.JoinType.CROSS; consume(TokenType.JOIN);
        } else if (check(TokenType.OUTER))  {
            advance(); joinType = JoinNode.JoinType.FULL;  consume(TokenType.JOIN);
        } else {
            advance(); // JOIN sólo → INNER
        }

        String table;
        if (check(TokenType.LEFT_PAREN)) { skipEnclosedBlock(); table = "(subquery)"; }
        else { table = consumeIdentifier().value; }

        String alias = null;
        if (optional(TokenType.AS)) { alias = consumeIdentifier().value; }
        else if (current != null && current.type == TokenType.IDENTIFIER) { alias = consumeIdentifier().value; }

        ConditionNode onCond = null;
        if (optional(TokenType.ON)) onCond = parseConditionChain();

        return new JoinNode(joinType, table, alias, onCond);
    }

    private OrderByClause parseOrderByClause() {
        // Captura la expresión completa. Si termina en DESC, marca descendente.
        StringBuilder expr = new StringBuilder();
        boolean asc = true;
        int parenDepth = 0;

        while (current != null && !check(TokenType.END_OF_FILE)) {
            TokenType t = current.type;
            if (parenDepth == 0 && (t == TokenType.COMMA || t == TokenType.SEMICOLON)) break;
            if (parenDepth == 0 && isMainClause(t) && t != TokenType.ORDER) break;
            if (parenDepth == 0 && (t == TokenType.UNION || t == TokenType.INTERSECT || t == TokenType.EXCEPT)) break;
            if (parenDepth == 0 && check(TokenType.LIMIT)) break;
            if (parenDepth == 0 && check(TokenType.FETCH)) break;

            if (t == TokenType.LEFT_PAREN)  parenDepth++;
            if (t == TokenType.RIGHT_PAREN) { if (parenDepth == 0) break; parenDepth--; }

            if (t == TokenType.DESC && parenDepth == 0) { asc = false; advance(); break; }
            if (t == TokenType.ASC  && parenDepth == 0) { asc = true;  advance(); break; }

            if (expr.length() > 0) expr.append(" ");
            expr.append(current.value);
            advance();
        }

        // NULLS FIRST / NULLS LAST
        if (check(TokenType.NULLS)) {
            advance();
            if (checkAny(TokenType.FIRST, TokenType.LAST, TokenType.IDENTIFIER)) advance();
        }

        return new OrderByClause(expr.toString().trim(), asc);
    }

    // ─── INSERT ──────────────────────────────────────────────────────────────

    private InsertNode parseInsert() {
        InsertNode node = new InsertNode();
        consume(TokenType.INSERT);
        consume(TokenType.INTO);
        node.tableName = consumeIdentifier().value;

        if (optional(TokenType.LEFT_PAREN)) {
            node.columns.add(consumeIdentifier().value);
            while (optional(TokenType.COMMA)) node.columns.add(consumeIdentifier().value);
            consume(TokenType.RIGHT_PAREN);
        }

        // INSERT … SELECT
        if (check(TokenType.SELECT)) { parseSelect(); return node; }

        consume(TokenType.VALUES);
        do {
            consume(TokenType.LEFT_PAREN);
            List<ExpressionNode> row = new ArrayList<>();
            row.add(parseExpression());
            while (optional(TokenType.COMMA)) row.add(parseExpression());
            consume(TokenType.RIGHT_PAREN);
            node.valueRows.add(row);
        } while (optional(TokenType.COMMA));
        return node;
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    private UpdateNode parseUpdate() {
        UpdateNode node = new UpdateNode();
        consume(TokenType.UPDATE);
        node.tableName = consumeIdentifier().value;
        optional(TokenType.AS);
        if (current != null && current.type == TokenType.IDENTIFIER
                && !current.value.equalsIgnoreCase("SET")) consumeIdentifier();
        consume(TokenType.SET);
        node.setClauses.add(parseSetClause());
        while (optional(TokenType.COMMA)) node.setClauses.add(parseSetClause());
        if (optional(TokenType.WHERE)) node.whereCondition = parseConditionChain();
        return node;
    }

    private SetClause parseSetClause() {
        String col = consumeIdentifier().value;
        if (optional(TokenType.DOT)) col += "." + consumeIdentifier().value;
        consume(TokenType.EQUAL);
        ExpressionNode val = parseArithmeticExpression();
        return new SetClause(col, val);
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    private DeleteNode parseDelete() {
        DeleteNode node = new DeleteNode();
        consume(TokenType.DELETE);
        consume(TokenType.FROM);
        node.tableName = consumeIdentifier().value;
        if (optional(TokenType.WHERE)) node.whereCondition = parseConditionChain();
        return node;
    }

    // ─── CREATE ──────────────────────────────────────────────────────────────

    private StatementNode parseCreate() {
        consume(TokenType.CREATE);
        if (check(TokenType.TABLE))    return parseCreateTable();
        if (check(TokenType.DATABASE)) return parseCreateDatabase();
        if (check(TokenType.INDEX))    return parseCreateIndex();
        if (check(TokenType.VIEW))     return parseCreateView();
        if (checkAny(TokenType.PROCEDURE, TokenType.FUNCTION)) return parseCreateProcedure();
        if (check(TokenType.UNIQUE))   { advance(); return parseCreateIndex(); }
        error("Se esperaba TABLE, DATABASE, INDEX, VIEW o PROCEDURE/FUNCTION después de CREATE");
        return null;
    }

    private CreateTableNode parseCreateTable() {
        CreateTableNode node = new CreateTableNode();
        consume(TokenType.TABLE);
        if (check(TokenType.IF)) { advance(); optional(TokenType.NOT); consume(TokenType.EXISTS); node.ifNotExists = true; }
        node.tableName = consumeIdentifier().value;
        consume(TokenType.LEFT_PAREN);
        node.columns.add(parseColumnDefinition());
        while (optional(TokenType.COMMA)) {
            if (checkAny(TokenType.PRIMARY, TokenType.FOREIGN, TokenType.UNIQUE,
                         TokenType.CHECK, TokenType.CONSTRAINT, TokenType.INDEX)) {
                parseTableConstraint();
            } else if (check(TokenType.RIGHT_PAREN)) {
                break;
            } else {
                node.columns.add(parseColumnDefinition());
            }
        }
        consume(TokenType.RIGHT_PAREN);
        while (!checkAny(TokenType.SEMICOLON, TokenType.END_OF_FILE)) advance();
        return node;
    }

    private ColumnDefinitionNode parseColumnDefinition() {
        String name = consumeIdentifier().value;
        String type = parseDataType();
        ColumnDefinitionNode col = new ColumnDefinitionNode(name, type);

        while (!checkAny(TokenType.COMMA, TokenType.RIGHT_PAREN, TokenType.END_OF_FILE)) {
            if      (check(TokenType.NOT))           { advance(); consume(TokenType.NULL); col.notNull = true; }
            else if (check(TokenType.NULL))          { advance(); }
            else if (check(TokenType.PRIMARY))       { advance(); consume(TokenType.KEY); col.primaryKey = true; col.notNull = true; }
            else if (check(TokenType.UNIQUE))        { advance(); col.unique = true; }
            else if (check(TokenType.AUTO_INCREMENT)){ advance(); col.autoIncrement = true; }
            else if (check(TokenType.DEFAULT)) {
                advance();
                col.defaultValue = consumeTokensAsString(true).replaceAll("\\s*,$","").trim();
                break;
            }
            else if (check(TokenType.REFERENCES)) {
                advance();
                col.references = consumeIdentifier().value;
                if (check(TokenType.LEFT_PAREN)) {
                    advance(); col.referencesCol = consumeIdentifier().value; consume(TokenType.RIGHT_PAREN);
                }
            }
            else if (check(TokenType.CHECK))      { advance(); skipEnclosedBlock(); }
            else if (check(TokenType.CONSTRAINT)) { advance(); consumeIdentifier(); }
            else break;
        }
        return col;
    }

    private String parseDataType() {
        String type = current.value.toUpperCase(); advance();
        if (optional(TokenType.LEFT_PAREN)) {
            type += "(" + consume(TokenType.NUMBER).value;
            if (optional(TokenType.COMMA)) type += "," + consume(TokenType.NUMBER).value;
            consume(TokenType.RIGHT_PAREN);
            type += ")";
        }
        if (current != null && "UNSIGNED".equalsIgnoreCase(current.value)) { type += " UNSIGNED"; advance(); }
        return type;
    }

    private void parseTableConstraint() {
        while (!checkAny(TokenType.COMMA, TokenType.RIGHT_PAREN, TokenType.END_OF_FILE)) advance();
    }

    private StatementNode parseCreateDatabase() {
        consume(TokenType.DATABASE);
        if (check(TokenType.IF)) { advance(); optional(TokenType.NOT); consume(TokenType.EXISTS); }
        String name = consumeIdentifier().value;
        DropNode node = new DropNode(); node.dropType = DropNode.DropType.DATABASE; node.name = "CREATE DATABASE " + name;
        return node;
    }

    private StatementNode parseCreateIndex() {
        optional(TokenType.UNIQUE);
        if (check(TokenType.INDEX)) advance();
        if (check(TokenType.IF)) { advance(); optional(TokenType.NOT); consume(TokenType.EXISTS); }
        String idxName = check(TokenType.IDENTIFIER) ? consumeIdentifier().value : "idx";
        consume(TokenType.ON);
        String tableName = consumeIdentifier().value;
        if (check(TokenType.LEFT_PAREN)) skipEnclosedBlock();
        DropNode node = new DropNode(); node.dropType = DropNode.DropType.INDEX; node.name = idxName + " ON " + tableName;
        return node;
    }

    private StatementNode parseCreateView() {
        consume(TokenType.VIEW);
        if (check(TokenType.IF)) { advance(); optional(TokenType.NOT); consume(TokenType.EXISTS); }
        String viewName = consumeIdentifier().value;
        consume(TokenType.AS);
        if (check(TokenType.SELECT)) parseSelect();
        DropNode node = new DropNode(); node.dropType = DropNode.DropType.TABLE; node.name = "VIEW " + viewName;
        return node;
    }

    private StatementNode parseCreateProcedure() {
        boolean isProc = check(TokenType.PROCEDURE); advance();
        String name = check(TokenType.IDENTIFIER) ? consumeIdentifier().value : "proc";
        while (!checkAny(TokenType.END_OF_FILE, TokenType.SEMICOLON)) advance();
        DropNode node = new DropNode(); node.dropType = DropNode.DropType.TABLE;
        node.name = (isProc ? "PROCEDURE " : "FUNCTION ") + name; return node;
    }

    // ─── ALTER ───────────────────────────────────────────────────────────────

    private AlterTableNode parseAlter() {
        AlterTableNode node = new AlterTableNode();
        consume(TokenType.ALTER); consume(TokenType.TABLE);
        node.tableName = consumeIdentifier().value;

        if (check(TokenType.ADD)) {
            advance(); optional(TokenType.COLUMN);
            if (check(TokenType.CONSTRAINT)) { advance(); skipToSemicolonOrEOF(); return node; }
            if (checkAny(TokenType.PRIMARY, TokenType.FOREIGN, TokenType.UNIQUE, TokenType.CHECK)) {
                skipToSemicolonOrEOF(); return node;
            }
            node.alterType = AlterTableNode.AlterType.ADD;
            node.column = parseColumnDefinition();
        } else if (check(TokenType.DROP)) {
            advance(); optional(TokenType.COLUMN);
            node.alterType = AlterTableNode.AlterType.DROP;
            node.columnName = consumeIdentifier().value;
        } else if (check(TokenType.MODIFY)) {
            advance(); optional(TokenType.COLUMN);
            node.alterType = AlterTableNode.AlterType.MODIFY;
            node.column = parseColumnDefinition();
        } else if (check(TokenType.RENAME)) {
            advance(); optional(TokenType.TO);
            node.alterType = AlterTableNode.AlterType.RENAME;
            node.newName = consumeIdentifier().value;
        } else if (current != null && "CHANGE".equalsIgnoreCase(current.value)) {
            advance(); optional(TokenType.COLUMN);
            consumeIdentifier();
            node.alterType = AlterTableNode.AlterType.MODIFY;
            node.column = parseColumnDefinition();
        } else {
            skipToSemicolonOrEOF();
        }
        return node;
    }

    // ─── DROP ─────────────────────────────────────────────────────────────────

    private DropNode parseDrop() {
        DropNode node = new DropNode();
        consume(TokenType.DROP);
        if      (check(TokenType.TABLE))     { advance(); node.dropType = DropNode.DropType.TABLE; }
        else if (check(TokenType.DATABASE))  { advance(); node.dropType = DropNode.DropType.DATABASE; }
        else if (check(TokenType.INDEX))     { advance(); node.dropType = DropNode.DropType.INDEX; }
        else if (check(TokenType.VIEW))      { advance(); node.dropType = DropNode.DropType.TABLE; }
        else if (checkAny(TokenType.PROCEDURE, TokenType.FUNCTION)) { advance(); node.dropType = DropNode.DropType.TABLE; }
        else { error("Se esperaba TABLE, DATABASE, INDEX, VIEW o PROCEDURE después de DROP"); }
        if (check(TokenType.IF)) { advance(); optional(TokenType.NOT); consume(TokenType.EXISTS); node.ifExists = true; }
        node.name = consumeIdentifier().value;
        return node;
    }

    private DropNode parseTruncate() {
        consume(TokenType.TRUNCATE); optional(TokenType.TABLE);
        String name = consumeIdentifier().value;
        DropNode node = new DropNode(); node.dropType = DropNode.DropType.TABLE; node.name = "TRUNCATE " + name; return node;
    }

    private DropNode parseBackup() {
        consume(TokenType.BACKUP); optional(TokenType.DATABASE);
        String name = check(TokenType.IDENTIFIER) ? consumeIdentifier().value : "";
        DropNode node = new DropNode(); node.dropType = DropNode.DropType.DATABASE; node.name = "BACKUP " + name;
        while (!checkAny(TokenType.SEMICOLON, TokenType.END_OF_FILE)) advance();
        return node;
    }

    private DropNode parseTransaction() {
        String keyword = current.value; advance(); optional(TokenType.TRANSACTION);
        DropNode node = new DropNode(); node.dropType = DropNode.DropType.DATABASE; node.name = keyword + " TRANSACTION";
        return node;
    }

    private DropNode parseCallProcedure() {
        String keyword = current.value; advance();
        String procName = check(TokenType.IDENTIFIER) ? consumeIdentifier().value : "";
        if (check(TokenType.LEFT_PAREN)) skipEnclosedBlock();
        DropNode node = new DropNode(); node.dropType = DropNode.DropType.TABLE; node.name = keyword + " " + procName;
        return node;
    }

    // ─── CONDICIONES ─────────────────────────────────────────────────────────

    private ConditionNode parseConditionChain() {
        ConditionNode first = parseSingleCondition();
        ConditionNode cur   = first;
        while (checkAny(TokenType.AND, TokenType.OR)) {
            cur.logicalConnector = this.current.value.toUpperCase();
            advance();
            ConditionNode next = parseSingleCondition();
            cur.next = next; cur = next;
        }
        return first;
    }

    private ConditionNode parseSingleCondition() {
        ConditionNode node = new ConditionNode();

        // Grupo: (cond1 AND/OR cond2)
        if (check(TokenType.LEFT_PAREN)) {
            int look = position + 1;
            if (look < tokens.size() && tokens.get(look).type == TokenType.SELECT) {
                skipEnclosedBlock();
                node.left = new ExpressionNode(ExpressionNode.ExprType.IDENTIFIER, "(subquery)");
                node.specialOp = ConditionNode.SpecialOp.SUBQUERY;
                return node;
            }
            advance();
            ConditionNode inner = parseConditionChain();
            if (check(TokenType.RIGHT_PAREN)) advance();
            return inner;
        }

        // EXISTS / NOT EXISTS
        if (check(TokenType.EXISTS)) {
            advance();
            if (check(TokenType.LEFT_PAREN)) skipEnclosedBlock();
            node.specialOp = ConditionNode.SpecialOp.EXISTS;
            node.left = new ExpressionNode(ExpressionNode.ExprType.IDENTIFIER, "EXISTS");
            return node;
        }

        boolean negated = false;
        if (check(TokenType.NOT)) {
            advance(); negated = true;
            if (check(TokenType.EXISTS)) {
                advance();
                if (check(TokenType.LEFT_PAREN)) skipEnclosedBlock();
                node.specialOp = ConditionNode.SpecialOp.NOT_EXISTS;
                node.left = new ExpressionNode(ExpressionNode.ExprType.IDENTIFIER, "NOT EXISTS");
                return node;
            }
        }

        ExpressionNode left = parseArithmeticExpression();
        node.left = left;

        // IS NULL / IS NOT NULL
        if (check(TokenType.IS)) {
            advance();
            boolean isNot = optional(TokenType.NOT);
            consume(TokenType.NULL);
            node.specialOp = isNot ? ConditionNode.SpecialOp.IS_NOT_NULL : ConditionNode.SpecialOp.IS_NULL;
            return node;
        }

        // IN / NOT IN
        if (check(TokenType.IN) || negated && check(TokenType.IN)) {
            boolean isNotIn = negated;
            if (!isNotIn) optional(TokenType.IN); else advance();
            node.specialOp = isNotIn ? ConditionNode.SpecialOp.NOT_IN : ConditionNode.SpecialOp.IN;
            node.inValues  = parseInList();
            return node;
        }
        if (check(TokenType.IN)) {
            node.specialOp = negated ? ConditionNode.SpecialOp.NOT_IN : ConditionNode.SpecialOp.IN;
            advance();
            node.inValues = parseInList();
            return node;
        }

        // LIKE / ILIKE
        if (checkAny(TokenType.LIKE, TokenType.ILIKE)) {
            node.specialOp = check(TokenType.ILIKE) ? ConditionNode.SpecialOp.ILIKE : ConditionNode.SpecialOp.LIKE;
            advance(); node.right = parseExpression(); return node;
        }

        // BETWEEN x AND y
        if (optional(TokenType.BETWEEN)) {
            node.specialOp   = ConditionNode.SpecialOp.BETWEEN;
            node.betweenLow  = parseArithmeticExpression();
            consume(TokenType.AND);
            node.betweenHigh = parseArithmeticExpression();
            return node;
        }

        // ANY / ALL / SOME (subquery comparison)
        if (checkAny(TokenType.ANY, TokenType.ALL, TokenType.SOME)) {
            advance();
            if (check(TokenType.LEFT_PAREN)) skipEnclosedBlock();
            node.specialOp = ConditionNode.SpecialOp.SUBQUERY;
            return node;
        }

        // Comparación: =, >, <, >=, <=, !=, <>
        if (checkAny(TokenType.EQUAL, TokenType.GREATER, TokenType.LESS,
                     TokenType.GREATER_EQUAL, TokenType.LESS_EQUAL, TokenType.NOT_EQUAL)) {
            node.op = tokenToCompOp(this.current.type);
            advance();
            if (check(TokenType.LEFT_PAREN)) {
                int look = position + 1;
                if (look < tokens.size() && tokens.get(look).type == TokenType.SELECT) {
                    skipEnclosedBlock();
                    node.right = new ExpressionNode(ExpressionNode.ExprType.IDENTIFIER, "(subquery)");
                } else {
                    node.right = parseArithmeticExpression();
                }
            } else if (checkAny(TokenType.ANY, TokenType.ALL, TokenType.SOME)) {
                advance(); if (check(TokenType.LEFT_PAREN)) skipEnclosedBlock();
                node.right = new ExpressionNode(ExpressionNode.ExprType.IDENTIFIER, "(subquery)");
            } else {
                node.right = parseArithmeticExpression();
            }
            return node;
        }

        node.specialOp = ConditionNode.SpecialOp.NONE;
        return node;
    }

    private List<ExpressionNode> parseInList() {
        List<ExpressionNode> values = new ArrayList<>();
        consume(TokenType.LEFT_PAREN);
        if (check(TokenType.SELECT)) {
            int depth = 1;
            while (!check(TokenType.END_OF_FILE) && depth > 0) {
                if (check(TokenType.LEFT_PAREN))       depth++;
                else if (check(TokenType.RIGHT_PAREN)) { depth--; if (depth == 0) { advance(); break; } }
                advance();
            }
            values.add(new ExpressionNode(ExpressionNode.ExprType.IDENTIFIER, "(subquery)"));
            return values;
        }
        values.add(parseExpression());
        while (optional(TokenType.COMMA)) values.add(parseExpression());
        consume(TokenType.RIGHT_PAREN);
        return values;
    }

    private CompOperator tokenToCompOp(TokenType t) {
        switch (t) {
            case EQUAL:         return CompOperator.EQUAL;
            case GREATER:       return CompOperator.GREATER;
            case LESS:          return CompOperator.LESS;
            case GREATER_EQUAL: return CompOperator.GREATER_EQUAL;
            case LESS_EQUAL:    return CompOperator.LESS_EQUAL;
            case NOT_EQUAL:     return CompOperator.NOT_EQUAL;
            default: error("Operador inválido: " + t); return CompOperator.EQUAL;
        }
    }

    // ─── EXPRESIONES ─────────────────────────────────────────────────────────

    private ExpressionNode parseArithmeticExpression() {
        ExpressionNode left = parseExpression();
        while (checkAny(TokenType.PLUS, TokenType.MINUS, TokenType.ASTERISK, TokenType.DIVIDE, TokenType.MODULO)) {
            if (check(TokenType.ASTERISK)) {
                int next = position + 1;
                if (next < tokens.size() && isMainClause(tokens.get(next).type)) break;
                if (next < tokens.size() && tokens.get(next).type == TokenType.COMMA) break;
                if (next < tokens.size() && tokens.get(next).type == TokenType.FROM)  break;
            }
            String op = current.value; advance();
            ExpressionNode right = parseExpression();
            String leftStr  = left.type == ExpressionNode.ExprType.FUNCTION_CALL
                ? left.functionName + "(" + left.functionArg + ")" : left.value;
            String rightStr = right.type == ExpressionNode.ExprType.FUNCTION_CALL
                ? right.functionName + "(" + right.functionArg + ")" : right.value;
            left = new ExpressionNode(ExpressionNode.ExprType.IDENTIFIER, leftStr + " " + op + " " + rightStr);
        }
        return left;
    }

    private ExpressionNode parseExpression() {
        // Subquery escalar: (SELECT ...)
        if (check(TokenType.LEFT_PAREN)) {
            int look = position + 1;
            if (look < tokens.size() && tokens.get(look).type == TokenType.SELECT) {
                skipEnclosedBlock();
                return new ExpressionNode(ExpressionNode.ExprType.IDENTIFIER, "(subquery)");
            }
            advance();
            ExpressionNode inner = parseArithmeticExpression();
            if (check(TokenType.RIGHT_PAREN)) advance();
            return inner;
        }

        // CASE WHEN ... THEN ... ELSE ... END
        if (check(TokenType.CASE)) {
            String caseExpr = consumeTokensAsString(true);
            return new ExpressionNode(ExpressionNode.ExprType.IDENTIFIER, caseExpr);
        }

        // CAST / CONVERT / TRY_CAST
        if (checkAny(TokenType.CAST, TokenType.CONVERT, TokenType.TRY_CAST, TokenType.TRY_CONVERT)) {
            String fn = current.value; advance();
            if (check(TokenType.LEFT_PAREN)) skipEnclosedBlock();
            return new ExpressionNode(ExpressionNode.ExprType.IDENTIFIER, fn + "(...)");
        }

        // EXTRACT(YEAR FROM fecha)
        if (check(TokenType.EXTRACT)) {
            advance();
            if (check(TokenType.LEFT_PAREN)) skipEnclosedBlock();
            return new ExpressionNode(ExpressionNode.ExprType.IDENTIFIER, "EXTRACT(...)");
        }

        // Cualquier función con paréntesis
        if (isFunctionToken(current != null ? current.type : TokenType.INVALID)) {
            String fn = current.value; advance();
            if (!check(TokenType.LEFT_PAREN)) return ExpressionNode.functionCall(fn, "");
            advance(); // (
            StringBuilder args = new StringBuilder();
            int depth = 1;
            while (!check(TokenType.END_OF_FILE)) {
                if (check(TokenType.LEFT_PAREN))  { depth++; args.append("("); advance(); continue; }
                if (check(TokenType.RIGHT_PAREN)) {
                    depth--; if (depth == 0) { advance(); break; }
                    args.append(")"); advance(); continue;
                }
                if (args.length() > 0 && !current.value.equals(",")) args.append(" ");
                args.append(current.value);
                if (current.value.equals(",")) args.append(" ");
                advance();
            }
            return ExpressionNode.functionCall(fn, args.toString().trim());
        }

        if (optional(TokenType.NULL))
            return new ExpressionNode(ExpressionNode.ExprType.IDENTIFIER, "NULL");

        if (check(TokenType.BOOLEAN_LITERAL)) {
            String v = current.value; advance();
            return new ExpressionNode(ExpressionNode.ExprType.BOOLEAN, v);
        }

        if (check(TokenType.STRING)) {
            String v = current.value; advance();
            return new ExpressionNode(ExpressionNode.ExprType.STRING, v);
        }

        if (check(TokenType.MINUS)) {
            advance();
            String v = check(TokenType.NUMBER) ? "-" + consume(TokenType.NUMBER).value : "-0";
            return new ExpressionNode(ExpressionNode.ExprType.NUMBER, v);
        }

        if (check(TokenType.NUMBER)) {
            String v = current.value; advance();
            return new ExpressionNode(ExpressionNode.ExprType.NUMBER, v);
        }

        if (check(TokenType.PLUS)) { advance(); return parseExpression(); }

        if (current != null && !isSentenceTerminator(current.type)) {
            String v = current.value; advance();
            if (optional(TokenType.DOT) && current != null) { v += "." + current.value; advance(); }
            return new ExpressionNode(ExpressionNode.ExprType.IDENTIFIER, v);
        }

        error("Se esperaba una expresión");
        return null;
    }

    private boolean isFunctionToken(TokenType t) {
        switch (t) {
            case COUNT: case SUM: case AVG: case MAX_FN: case MIN_FN:
            case UPPER: case LOWER: case LENGTH: case COALESCE: case IFNULL:
            case NOW: case CONCAT: case REPLACE_FN: case SUBSTRING_FN:
            case TRIM_FN: case LTRIM_FN: case RTRIM_FN: case CHARINDEX_FN:
            case ROUND_FN: case ABS_FN: case FLOOR_FN: case CEILING_FN:
            case POWER_FN: case SQRT_FN: case GETDATE_FN: case DATEADD_FN:
            case DATEDIFF_FN: case DATEPART_FN: case YEAR_FN: case MONTH_FN:
            case DAY_FN: case CURDATE_FN: case SYSDATE_FN:
            case RANK: case DENSE_RANK: case ROW_NUMBER: case NTILE:
            case LAG: case LEAD: case FIRST_VALUE: case LAST_VALUE:
            case PERCENT_RANK: case CUME_DIST: case STRING_AGG:
            case NULLIF: case IIF: case NVL: case MOD_FN: case STUFF_FN:
                return true;
            default: return false;
        }
    }

    private boolean isSentenceTerminator(TokenType t) {
        switch (t) {
            case END_OF_FILE: case COMMA: case SEMICOLON: case RIGHT_PAREN:
            case EQUAL: case GREATER: case LESS: case GREATER_EQUAL:
            case LESS_EQUAL: case NOT_EQUAL: case AND: case OR:
            case WHERE: case FROM: case ORDER: case GROUP: case HAVING:
            case LIMIT: case OFFSET: case JOIN: case INNER: case LEFT:
            case RIGHT: case ON: case UNION: case INTERSECT: case EXCEPT:
            case FULL: case CROSS: case OUTER: case FETCH:
                return true;
            default: return false;
        }
    }

    private boolean isReservedWord(TokenType t) {
        switch (t) {
            case SELECT: case FROM: case WHERE: case JOIN: case INNER:
            case LEFT: case RIGHT: case FULL: case CROSS: case ON:
            case GROUP: case HAVING: case ORDER: case LIMIT: case OFFSET:
            case UNION: case INTERSECT: case EXCEPT: case AND: case OR:
            case NOT: case IN: case LIKE: case BETWEEN: case IS:
            case CASE: case WHEN: case THEN: case ELSE: case END:
            case INSERT: case UPDATE: case DELETE: case SET: case VALUES:
            case CREATE: case ALTER: case DROP: case TRUNCATE:
            case SEMICOLON: case END_OF_FILE: case COMMA:
                return true;
            default: return false;
        }
    }

    public List<String> getCteNames() { return cteNames; }
}
