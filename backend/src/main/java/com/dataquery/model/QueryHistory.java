package com.dataquery.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad JPA que guarda cada query analizada por cada usuario.
 * Alimenta el Dashboard (Tipo 3) y el análisis de la IA.
 */
@Entity
@Table(name = "query_history",
       indexes = {
           @Index(name = "idx_qh_user",       columnList = "user_id"),
           @Index(name = "idx_qh_analyzed_at", columnList = "analyzed_at"),
           @Index(name = "idx_qh_stmt_type",  columnList = "statement_type")
       })
public class QueryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "query_text", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String queryText;

    @Column(name = "statement_type", length = 30)
    private String statementType;   // SELECT, INSERT, UPDATE, DELETE, etc.

    @Column(name = "database_type", length = 20)
    private String databaseType;    // mysql, postgresql, sqlserver, mongodb

    @Column(name = "is_valid", nullable = false)
    private boolean valid;

    @Column(name = "errors_count")
    private int errorsCount;

    @Column(name = "warnings_count")
    private int warningsCount;

    @Column(name = "suggestions_count")
    private int suggestionsCount;

    // JSON simplificado con los mensajes de error
    @Column(name = "error_messages", columnDefinition = "NVARCHAR(MAX)")
    private String errorMessages;

    @CreationTimestamp
    @Column(name = "analyzed_at", updatable = false)
    private LocalDateTime analyzedAt;

    // ── Constructor ───────────────────────────────────────────────────────

    public QueryHistory() {}

    public QueryHistory(User user, String queryText, String statementType,
                        String databaseType, boolean valid,
                        int errorsCount, int warningsCount,
                        int suggestionsCount, String errorMessages) {
        this.user             = user;
        this.queryText        = queryText;
        this.statementType    = statementType;
        this.databaseType     = databaseType;
        this.valid            = valid;
        this.errorsCount      = errorsCount;
        this.warningsCount    = warningsCount;
        this.suggestionsCount = suggestionsCount;
        this.errorMessages    = errorMessages;
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public Long          getId()               { return id; }
    public User          getUser()             { return user; }
    public String        getQueryText()        { return queryText; }
    public String        getStatementType()    { return statementType; }
    public String        getDatabaseType()     { return databaseType; }
    public boolean       isValid()             { return valid; }
    public int           getErrorsCount()      { return errorsCount; }
    public int           getWarningsCount()    { return warningsCount; }
    public int           getSuggestionsCount() { return suggestionsCount; }
    public String        getErrorMessages()    { return errorMessages; }
    public LocalDateTime getAnalyzedAt()       { return analyzedAt; }
}
