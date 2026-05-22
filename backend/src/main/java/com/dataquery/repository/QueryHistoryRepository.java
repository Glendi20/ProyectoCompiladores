package com.dataquery.repository;

import com.dataquery.model.QueryHistory;
import com.dataquery.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QueryHistoryRepository extends JpaRepository<QueryHistory, Long> {

    // ── Estadísticas personales del estudiante ────────────────────────────

    List<QueryHistory> findTop10ByUserOrderByAnalyzedAtDesc(User user);
    long countByUser(User user);
    long countByUserAndValid(User user, boolean valid);
    long countByUserAndStatementType(User user, String statementType);
    long countByUserAndDatabaseType(User user, String databaseType);

    // Detecta cuántas veces el usuario usó SELECT * (mala práctica)
    @Query("SELECT COUNT(q) FROM QueryHistory q WHERE q.user = :user AND q.queryText LIKE 'SELECT *%'")
    long countSelectStarByUser(User user);

    // Tasa de éxito de la última semana para calcular tendencia
    @Query("""
        SELECT COUNT(q) FROM QueryHistory q
        WHERE q.user = :user
          AND q.valid = true
          AND q.analyzedAt >= CURRENT_TIMESTAMP - 7 DAY
        """)
    long countSuccessfulLastWeekByUser(User user);

    @Query("""
        SELECT COUNT(q) FROM QueryHistory q
        WHERE q.user = :user
          AND q.analyzedAt >= CURRENT_TIMESTAMP - 7 DAY
        """)
    long countTotalLastWeekByUser(User user);

    // ── Estadísticas globales (para el Profesor) ──────────────────────────

    List<QueryHistory> findTop20ByOrderByAnalyzedAtDesc();
    long countByValid(boolean valid);
    long countByStatementType(String statementType);
    long countByDatabaseType(String databaseType);

    // Distribución por tipo de sentencia
    @Query("SELECT q.statementType, COUNT(q) FROM QueryHistory q GROUP BY q.statementType")
    List<Object[]> countGroupByStatementType();

    // Distribución por base de datos
    @Query("SELECT q.databaseType, COUNT(q) FROM QueryHistory q GROUP BY q.databaseType")
    List<Object[]> countGroupByDatabaseType();

    // Progreso de todos los usuarios (para vista del Profesor)
    @Query("""
        SELECT q.user, COUNT(q), SUM(CASE WHEN q.valid = true THEN 1 ELSE 0 END)
        FROM QueryHistory q
        GROUP BY q.user
        ORDER BY COUNT(q) DESC
        """)
    List<Object[]> getUsersProgress();
}
