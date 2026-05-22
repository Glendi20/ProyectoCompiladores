package com.dataquery.dto;

/**
 * Perfil calculado de un usuario basado en su historial de queries.
 * Alimenta la IA Tipo 3 para generar sugerencias personalizadas.
 */
public class UserProfile {
    private String nombre;
    private long   totalQueries;
    private double successRate;       // porcentaje de queries válidas
    private String topError;          // error más repetido
    private String favoriteDatabase;  // BD más usada
    private String mostUsedStatement; // tipo de sentencia más usada
    private String trend;             // IMPROVING, DECLINING, STABLE, NEW
    private long   selectStarCount;   // veces que usó SELECT *
    private double weeklySuccessRate; // tasa de éxito de los últimos 7 días

    public UserProfile() {}

    public String getNombre()            { return nombre; }
    public void   setNombre(String v)    { this.nombre = v; }
    public long   getTotalQueries()      { return totalQueries; }
    public void   setTotalQueries(long v){ this.totalQueries = v; }
    public double getSuccessRate()       { return successRate; }
    public void   setSuccessRate(double v){ this.successRate = v; }
    public String getTopError()          { return topError; }
    public void   setTopError(String v)  { this.topError = v; }
    public String getFavoriteDatabase()  { return favoriteDatabase; }
    public void   setFavoriteDatabase(String v){ this.favoriteDatabase = v; }
    public String getMostUsedStatement() { return mostUsedStatement; }
    public void   setMostUsedStatement(String v){ this.mostUsedStatement = v; }
    public String getTrend()             { return trend; }
    public void   setTrend(String v)     { this.trend = v; }
    public long   getSelectStarCount()   { return selectStarCount; }
    public void   setSelectStarCount(long v){ this.selectStarCount = v; }
    public double getWeeklySuccessRate() { return weeklySuccessRate; }
    public void   setWeeklySuccessRate(double v){ this.weeklySuccessRate = v; }

    public boolean isNew()       { return "NEW".equals(trend); }
    public boolean isImproving() { return "IMPROVING".equals(trend); }
}
