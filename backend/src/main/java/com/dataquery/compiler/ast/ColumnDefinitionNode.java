package com.dataquery.compiler.ast;

/**
 * Representa la definición de una columna en CREATE TABLE o ALTER TABLE.
 * Ejemplo: id INT NOT NULL AUTO_INCREMENT PRIMARY KEY
 */
public class ColumnDefinitionNode extends ASTNode {
    public String name;
    public String dataType;        // "INT", "VARCHAR(255)", "FLOAT", etc.
    public boolean notNull;
    public boolean primaryKey;
    public boolean unique;
    public boolean autoIncrement;
    public String  defaultValue;   // valor por defecto como string
    public String  references;     // nombre de tabla referenciada (FOREIGN KEY)
    public String  referencesCol;  // columna referenciada

    public ColumnDefinitionNode(String name, String dataType) {
        this.name     = name;
        this.dataType = dataType;
    }

    @Override
    public void buildString(StringBuilder sb, int indent) {
        String sp = " ".repeat(indent);
        sb.append(sp).append(name).append(" ").append(dataType);
        if (notNull)        sb.append(" NOT NULL");
        if (primaryKey)     sb.append(" PRIMARY KEY");
        if (unique)         sb.append(" UNIQUE");
        if (autoIncrement)  sb.append(" AUTO_INCREMENT");
        if (defaultValue != null) sb.append(" DEFAULT ").append(defaultValue);
        if (references != null)   sb.append(" REFERENCES ").append(references).append("(").append(referencesCol).append(")");
        sb.append("\n");
    }
}
