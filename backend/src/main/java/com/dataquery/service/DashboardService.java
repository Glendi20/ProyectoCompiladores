package com.dataquery.service;

import com.dataquery.model.ErrorFrequency;
import com.dataquery.model.QueryHistory;
import com.dataquery.model.User;
import com.dataquery.repository.ErrorFrequencyRepository;
import com.dataquery.repository.QueryHistoryRepository;
import com.dataquery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Calcula estadísticas para el Dashboard.
 * Estudiante → estadísticas personales.
 * Profesor   → estadísticas globales de todos los usuarios.
 */
@Service
public class DashboardService {

    @Autowired private QueryHistoryRepository historyRepo;
    @Autowired private ErrorFrequencyRepository errorRepo;
    @Autowired private UserRepository userRepo;

    // ── Vista del Estudiante ──────────────────────────────────────────────

    public Map<String, Object> getStudentStats(User user) {
        Map<String, Object> stats = new LinkedHashMap<>();

        long total   = historyRepo.countByUser(user);
        long success = historyRepo.countByUserAndValid(user, true);
        long failed  = total - success;

        stats.put("totalQueries",   total);
        stats.put("successful",     success);
        stats.put("failed",         failed);
        stats.put("successRate",    total > 0 ? Math.round(success * 1000.0 / total) / 10.0 : 0);
        stats.put("selectStarCount", historyRepo.countSelectStarByUser(user));

        // Distribución por tipo de sentencia
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String type : List.of("SELECT","INSERT","UPDATE","DELETE","CREATE TABLE","ALTER TABLE","DROP TABLE")) {
            long c = historyRepo.countByUserAndStatementType(user, type);
            if (c > 0) byType.put(type, c);
        }
        stats.put("byStatementType", byType);

        // Distribución por base de datos
        Map<String, Long> byDb = new LinkedHashMap<>();
        for (String db : List.of("mysql","sqlserver","postgresql","mongodb")) {
            long c = historyRepo.countByUserAndDatabaseType(user, db);
            if (c > 0) byDb.put(db, c);
        }
        stats.put("byDatabase", byDb);

        // Últimas 10 queries del usuario
        stats.put("recentQueries", buildRecentList(
            historyRepo.findTop10ByUserOrderByAnalyzedAtDesc(user)));

        return stats;
    }

    // ── Vista del Profesor (global) ───────────────────────────────────────

    public Map<String, Object> getGlobalStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long total   = historyRepo.count();
        long success = historyRepo.countByValid(true);
        long failed  = total - success;
        long users   = userRepo.count();

        stats.put("totalQueries",  total);
        stats.put("totalUsers",    users);
        stats.put("successful",    success);
        stats.put("failed",        failed);
        stats.put("successRate",   total > 0 ? Math.round(success * 1000.0 / total) / 10.0 : 0);

        // Top 10 errores más comunes en el sistema
        stats.put("topErrors", buildErrorList(errorRepo.findTop10ByOrderByOccurrenceCountDesc()));

        // Distribución global por tipo
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String type : List.of("SELECT","INSERT","UPDATE","DELETE","CREATE TABLE","ALTER TABLE","DROP TABLE")) {
            long c = historyRepo.countByStatementType(type);
            if (c > 0) byType.put(type, c);
        }
        stats.put("byStatementType", byType);

        // Distribución global por BD
        Map<String, Long> byDb = new LinkedHashMap<>();
        for (String db : List.of("mysql","sqlserver","postgresql","mongodb")) {
            long c = historyRepo.countByDatabaseType(db);
            if (c > 0) byDb.put(db, c);
        }
        stats.put("byDatabase", byDb);

        // Últimas 20 queries globales
        stats.put("recentQueries", buildRecentList(
            historyRepo.findTop20ByOrderByAnalyzedAtDesc()));

        // Progreso por usuario
        stats.put("usersProgress", buildUsersProgress());

        return stats;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private List<Map<String, Object>> buildRecentList(List<QueryHistory> queries) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (QueryHistory q : queries) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id",            q.getId());
            item.put("statementType", q.getStatementType());
            item.put("databaseType",  q.getDatabaseType());
            item.put("valid",         q.isValid());
            item.put("errorsCount",   q.getErrorsCount());
            item.put("analyzedAt",    q.getAnalyzedAt() != null ? q.getAnalyzedAt().toString() : "");
            // Truncar query para el feed (máximo 60 caracteres)
            String qText = q.getQueryText();
            item.put("queryPreview",  qText.length() > 60 ? qText.substring(0, 60) + "..." : qText);
            if (q.getUser() != null)
                item.put("userName", q.getUser().getFullName());
            list.add(item);
        }
        return list;
    }

    private List<Map<String, Object>> buildErrorList(List<ErrorFrequency> errors) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ErrorFrequency e : errors) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("errorPattern",   e.getErrorPattern());
            item.put("occurrenceCount",e.getOccurrenceCount());
            item.put("statementType",  e.getStatementType());
            item.put("lastSeen",       e.getLastSeen() != null ? e.getLastSeen().toString() : "");
            list.add(item);
        }
        return list;
    }

    private List<Map<String, Object>> buildUsersProgress() {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            List<Object[]> rows = historyRepo.getUsersProgress();
            for (Object[] row : rows) {
                User u     = (User) row[0];
                long total = ((Number) row[1]).longValue();
                long ok    = ((Number) row[2]).longValue();
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("userId",      u.getId());
                item.put("nombre",      u.getFullName());
                item.put("email",       u.getEmail());
                item.put("role",        u.getRole().name());
                item.put("totalQueries",total);
                item.put("successful",  ok);
                item.put("successRate", total > 0 ? Math.round(ok * 1000.0 / total) / 10.0 : 0);
                list.add(item);
            }
        } catch (Exception ignored) {}
        return list;
    }
}
