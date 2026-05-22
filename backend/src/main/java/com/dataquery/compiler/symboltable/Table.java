package com.dataquery.compiler.symboltable;

import java.util.ArrayList;
import java.util.List;

public class Table {
    public String name;
    public List<Column> columns;

    public Table(String name) {
        this.name = name;
        this.columns = new ArrayList<>();
    }

    public void addColumn(String name, DataType type) {
        columns.add(new Column(name, type));
    }

    public Column findColumn(String columnName) {
        for (Column col : columns) {
            if (col.name.equalsIgnoreCase(columnName)) return col;
        }
        return null;
    }
}
