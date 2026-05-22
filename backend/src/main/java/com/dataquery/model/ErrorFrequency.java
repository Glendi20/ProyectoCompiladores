package com.dataquery.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entidad JPA que acumula la frecuencia de errores en el sistema.
 * Cada vez que un usuario comete un error, se incrementa el contador.
 * Esta tabla alimenta la IA Tipo 3 — sugerencias basadas en datos reales.
 */
@Entity
@Table(name = "error_frequency",
       indexes = {
           @Index(name = "idx_ef_count", columnList = "occurrence_count DESC"),
           @Index(name = "idx_ef_stmt",  columnList = "statement_type")
       })
public class ErrorFrequency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Texto del error (normalizado para agrupar variaciones del mismo error)
    @Column(name = "error_pattern", length = 500, nullable = false)
    private String errorPattern;

    // Cuántas veces ocurrió este error en total
    @Column(name = "occurrence_count", nullable = false)
    private int occurrenceCount = 1;

    @Column(name = "statement_type", length = 30)
    private String statementType;   // en qué tipo de query ocurrió más

    @Column(name = "database_type", length = 20)
    private String databaseType;    // en qué BD ocurrió más

    @Column(name = "first_seen")
    private LocalDateTime firstSeen;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @PrePersist
    protected void onCreate() {
        firstSeen = LocalDateTime.now();
        lastSeen  = LocalDateTime.now();
    }

    public ErrorFrequency() {}

    public ErrorFrequency(String errorPattern, String statementType, String databaseType) {
        this.errorPattern  = errorPattern;
        this.statementType = statementType;
        this.databaseType  = databaseType;
    }

    public void incrementCount() {
        this.occurrenceCount++;
        this.lastSeen = LocalDateTime.now();
    }

    // ── Getters y Setters ─────────────────────────────────────────────────

    public Long          getId()               { return id; }
    public String        getErrorPattern()     { return errorPattern; }
    public void          setErrorPattern(String v) { this.errorPattern = v; }
    public int           getOccurrenceCount()  { return occurrenceCount; }
    public void          setOccurrenceCount(int v) { this.occurrenceCount = v; }
    public String        getStatementType()    { return statementType; }
    public void          setStatementType(String v){ this.statementType = v; }
    public String        getDatabaseType()     { return databaseType; }
    public LocalDateTime getFirstSeen()        { return firstSeen; }
    public LocalDateTime getLastSeen()         { return lastSeen; }
    public void          setLastSeen(LocalDateTime v) { this.lastSeen = v; }
}
