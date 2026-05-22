package com.dataquery.compiler.ast;

/**
 * Representa una columna en ORDER BY con su dirección.
 * Ejemplo: ORDER BY edad DESC, nombre ASC
 */
public class OrderByClause {
    public String column;
    public boolean ascending; // true = ASC (default), false = DESC

    public OrderByClause(String column, boolean ascending) {
        this.column    = column;
        this.ascending = ascending;
    }

    public String toString() {
        return column + " " + (ascending ? "ASC" : "DESC");
    }
}
