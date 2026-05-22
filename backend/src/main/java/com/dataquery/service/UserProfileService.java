package com.dataquery.service;

import com.dataquery.dto.UserProfile;
import com.dataquery.model.User;
import com.dataquery.repository.QueryHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Calcula el perfil personalizado de un usuario basado en su historial.
 * Este perfil alimenta la IA Tipo 3 para generar sugerencias adaptadas.
 */
@Service
public class UserProfileService {

    @Autowired
    private QueryHistoryRepository historyRepo;

    public UserProfile buildProfile(User user) {
        UserProfile profile = new UserProfile();
        profile.setNombre(user.getFullName());

        long total = historyRepo.countByUser(user);
        profile.setTotalQueries(total);

        if (total == 0) {
            profile.setTrend("NEW");
            profile.setSuccessRate(0);
            profile.setSelectStarCount(0);
            return profile;
        }

        // Tasa de éxito global
        long exitosas = historyRepo.countByUserAndValid(user, true);
        double rate = (exitosas * 100.0) / total;
        profile.setSuccessRate(Math.round(rate * 10.0) / 10.0);

        // Cuántas veces usó SELECT *
        profile.setSelectStarCount(historyRepo.countSelectStarByUser(user));

        // Base de datos favorita
        profile.setFavoriteDatabase(getFavoriteDb(user));

        // Tipo de sentencia más usada
        profile.setMostUsedStatement(getMostUsedStatement(user));

        // Tendencia (comparar última semana con total)
        profile.setTrend(calculateTrend(user, rate));
        profile.setWeeklySuccessRate(getWeeklySuccessRate(user));

        // Error más común (del historial de queries con error)
        profile.setTopError(getTopError(user));

        return profile;
    }

    private String getFavoriteDb(User user) {
        String[] dbs = {"mysql", "sqlserver", "postgresql", "mongodb"};
        String favorite = "mysql";
        long maxCount = 0;
        for (String db : dbs) {
            long count = historyRepo.countByUserAndDatabaseType(user, db);
            if (count > maxCount) { maxCount = count; favorite = db; }
        }
        return favorite;
    }

    private String getMostUsedStatement(User user) {
        String[] stmts = {"SELECT", "INSERT", "UPDATE", "DELETE", "CREATE TABLE"};
        String most = "SELECT";
        long maxCount = 0;
        for (String s : stmts) {
            long count = historyRepo.countByUserAndStatementType(user, s);
            if (count > maxCount) { maxCount = count; most = s; }
        }
        return most;
    }

    private String calculateTrend(User user, double overallRate) {
        long weekTotal   = historyRepo.countTotalLastWeekByUser(user);
        if (weekTotal == 0) return "STABLE";
        long weekSuccess = historyRepo.countSuccessfulLastWeekByUser(user);
        double weekRate  = (weekSuccess * 100.0) / weekTotal;
        if (weekRate > overallRate + 5)  return "IMPROVING";
        if (weekRate < overallRate - 5)  return "DECLINING";
        return "STABLE";
    }

    private double getWeeklySuccessRate(User user) {
        long weekTotal = historyRepo.countTotalLastWeekByUser(user);
        if (weekTotal == 0) return 0;
        long weekSuccess = historyRepo.countSuccessfulLastWeekByUser(user);
        double rate = (weekSuccess * 100.0) / weekTotal;
        return Math.round(rate * 10.0) / 10.0;
    }

    private String getTopError(User user) {
        // Retorna el primer error del historial del usuario
        var recent = historyRepo.findTop10ByUserOrderByAnalyzedAtDesc(user);
        return recent.stream()
            .filter(q -> !q.isValid() && q.getErrorMessages() != null)
            .map(q -> {
                String msg = q.getErrorMessages();
                return msg.length() > 80 ? msg.substring(0, 80) + "..." : msg;
            })
            .findFirst()
            .orElse(null);
    }
}
