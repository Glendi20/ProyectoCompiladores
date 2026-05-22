package com.dataquery.service;

import com.dataquery.compiler.ast.*;
import com.dataquery.compiler.lexer.Lexer;
import com.dataquery.compiler.lexer.Token;
import com.dataquery.compiler.parser.Parser;
import com.dataquery.compiler.semantic.SemanticAnalyzer;
import com.dataquery.compiler.symboltable.SymbolTable;
import com.dataquery.compiler.symboltable.Table;
import com.dataquery.dto.UserProfile;
import com.dataquery.model.*;
import com.dataquery.repository.ErrorFrequencyRepository;
import com.dataquery.repository.QueryHistoryRepository;
import com.dataquery.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Orquesta las 4 fases del compilador SQL y persiste los resultados en SQL Server.
 * El usuario se obtiene del JWT (no del request body) para máxima seguridad.
 */
@Service
public class CompilerService {

    @Autowired private OptimizerService         optimizerService;
    @Autowired private UserProfileService        userProfileService;
    @Autowired private QueryHistoryRepository    historyRepo;
    @Autowired private ErrorFrequencyRepository  errorFreqRepo;

    public CompilerResponse compile(CompilerRequest request, User user) {
        CompilerResponse response = new CompilerResponse();
        String query    = request.getQuery() != null ? request.getQuery().trim() : "";
        String database = request.getDatabase() != null ? request.getDatabase() : "mysql";

        if (query.isEmpty()) {
            response.setValid(false);
            response.setErrors(List.of("La consulta está vacía"));
            response.setSuggestions(new ArrayList<>());
            response.setTokens(new ArrayList<>());
            return response;
        }

        // ── FASE 1: Análisis Léxico ──────────────────────────────────────
        Lexer lexer = new Lexer(query);
        List<Token> tokens = lexer.tokenize();
        response.setTokens(tokens.stream()
            .map(t -> new TokenDto(t.type.name(), t.value, t.line, t.column))
            .collect(Collectors.toList()));

        // ── FASE 2: Análisis Sintáctico ──────────────────────────────────
        StatementNode ast;
        Parser parser;
        try {
            parser = new Parser(tokens);
            ast = parser.parse();
            response.setStatementType(ast.getStatementType());
            response.setAst(toASTString(ast));
        } catch (RuntimeException e) {
            response.setValid(false);
            response.setErrors(List.of(e.getMessage()));
            response.setWarnings(new ArrayList<>());
            response.setSuggestions(new ArrayList<>());
            response.setDialectNote(dialectNote(database));
            saveHistory(user, query, "UNKNOWN", database, false, List.of(e.getMessage()), 0);
            return response;
        }

        // ── FASE 3: Análisis Semántico ───────────────────────────────────
        SymbolTable symbolTable = new SymbolTable();
        SemanticAnalyzer analyzer = new SemanticAnalyzer(symbolTable);
        analyzer.addCteNames(parser.getCteNames());
        boolean valid = analyzer.analyze(ast);

        response.setValid(valid);
        response.setErrors(analyzer.getErrors());
        response.setWarnings(analyzer.getWarnings());

        // ── FASE 4: Optimización Tipo 3 (personalizada) ──────────────────
        Table resolvedTable = resolveMainTable(ast, symbolTable);

        // Construir perfil del usuario para sugerencias personalizadas
        UserProfile profile = (user != null) ? userProfileService.buildProfile(user) : null;

        response.setSuggestions(
            optimizerService.analyze(ast, database, resolvedTable, profile));
        response.setDialectNote(dialectNote(database));

        // Query optimizada sugerida
        if (valid && ast instanceof SelectNode) {
            SelectNode sel = (SelectNode) ast;
            if (sel.selectAll && resolvedTable != null) {
                List<String> cols = new ArrayList<>();
                for (var col : resolvedTable.columns) cols.add(col.name);
                response.setOptimizedQuery(
                    "SELECT " + String.join(", ", cols) +
                    " FROM " + sel.tableName +
                    (sel.whereCondition != null ? " WHERE ..." : "") +
                    (sel.limit == null ? " LIMIT 20" : ""));
            }
        }

        // ── Persistir en SQL Server ───────────────────────────────────────
        int suggestionsCount = response.getSuggestions() != null ? response.getSuggestions().size() : 0;
        saveHistory(user, query, ast.getStatementType(), database, valid,
                    analyzer.getErrors(), suggestionsCount);

        // Actualizar frecuencia de errores (alimenta la IA)
        if (!valid) updateErrorFrequency(analyzer.getErrors(), ast.getStatementType(), database);

        return response;
    }

    // ── Persistencia ─────────────────────────────────────────────────────

    private void saveHistory(User user, String query, String stmtType,
                             String dbType, boolean valid,
                             List<String> errors, int suggestionsCount) {
        if (user == null) return;
        try {
            String errorJson = errors.isEmpty() ? null : String.join(" | ", errors);
            QueryHistory history = new QueryHistory(
                user, query, stmtType, dbType, valid,
                errors.size(), 0, suggestionsCount, errorJson);
            historyRepo.save(history);
        } catch (Exception ignored) {
            // No fallar la compilación si hay error al guardar historial
        }
    }

    private void updateErrorFrequency(List<String> errors, String stmtType, String dbType) {
        for (String error : errors) {
            // Normalizar el error para agrupar variaciones del mismo problema
            String pattern = normalizeError(error);
            try {
                Optional<ErrorFrequency> existing =
                    errorFreqRepo.findByErrorPatternAndStatementType(pattern, stmtType);
                if (existing.isPresent()) {
                    existing.get().incrementCount();
                    errorFreqRepo.save(existing.get());
                } else {
                    errorFreqRepo.save(new ErrorFrequency(pattern, stmtType, dbType));
                }
            } catch (Exception ignored) {}
        }
    }

    private String normalizeError(String error) {
        // Normalizar para agrupar: quitar nombres de tablas/columnas específicos
        if (error.contains("no existe en el schema")) return "Tabla no existe en el schema";
        if (error.contains("no existe en la tabla"))  return "Columna no existe en la tabla";
        if (error.contains("UPDATE sin WHERE"))        return "UPDATE sin WHERE — peligro crítico";
        if (error.contains("DELETE sin WHERE"))        return "DELETE sin WHERE — peligro crítico";
        if (error.contains("Tipos incompatibles"))     return "Tipos incompatibles en condición";
        if (error.contains("Error de sintaxis"))       return "Error de sintaxis";
        return error.length() > 100 ? error.substring(0, 100) : error;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String toASTString(StatementNode ast) {
        if (ast instanceof SelectNode)      return ((SelectNode) ast).toASTString();
        if (ast instanceof InsertNode)      return ((InsertNode) ast).toASTString();
        if (ast instanceof UpdateNode)      return ((UpdateNode) ast).toASTString();
        if (ast instanceof DeleteNode)      return ((DeleteNode) ast).toASTString();
        if (ast instanceof CreateTableNode) return ((CreateTableNode) ast).toASTString();
        if (ast instanceof AlterTableNode)  return ((AlterTableNode) ast).toASTString();
        if (ast instanceof DropNode)        return ((DropNode) ast).toASTString();
        StringBuilder sb = new StringBuilder();
        ast.buildString(sb, 0);
        return sb.toString();
    }

    private Table resolveMainTable(StatementNode ast, SymbolTable st) {
        String name = null;
        if (ast instanceof SelectNode)      name = ((SelectNode) ast).tableName;
        else if (ast instanceof InsertNode) name = ((InsertNode) ast).tableName;
        else if (ast instanceof UpdateNode) name = ((UpdateNode) ast).tableName;
        else if (ast instanceof DeleteNode) name = ((DeleteNode) ast).tableName;
        return name != null ? st.findTable(name) : null;
    }

    private String dialectNote(String db) {
        switch (db.toLowerCase()) {
            case "mysql":      return "MySQL 8.0 — Motor InnoDB, ACID, índices B-Tree.";
            case "postgresql": return "PostgreSQL 16 — JSON nativo, arrays, extensiones.";
            case "sqlserver":  return "SQL Server 2022 — T-SQL, procedimientos, Azure.";
            case "mongodb":    return "MongoDB 7.0 — NoSQL orientada a documentos BSON.";
            default:           return "";
        }
    }
}
