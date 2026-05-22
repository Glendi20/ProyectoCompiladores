package com.dataquery.compiler.symboltable;

public enum DataType {
    INT, VARCHAR, FLOAT;

    public String dataTypeToString() {
        switch (this) {
            case INT:     return "INT";
            case VARCHAR: return "VARCHAR";
            case FLOAT:   return "FLOAT";
            default:      return "UNKNOWN";
        }
    }
}
