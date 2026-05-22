package com.dataquery.compiler.symboltable;

import java.util.HashMap;
import java.util.Map;

/**
 * Tabla de Símbolos — schema de la base de datos.
 * Contiene 7 tablas relacionadas entre sí, simulando un sistema real de ventas.
 */
public class SymbolTable {

    private final Map<String, Table> tables = new HashMap<>();

    public SymbolTable() {

        // ── usuarios ──────────────────────────────────────────────────────
        Table usuarios = new Table("usuarios");
        usuarios.addColumn("id",            DataType.INT);
        usuarios.addColumn("nombre",        DataType.VARCHAR);
        usuarios.addColumn("apellido",      DataType.VARCHAR);
        usuarios.addColumn("email",         DataType.VARCHAR);
        usuarios.addColumn("edad",          DataType.INT);
        usuarios.addColumn("ciudad",        DataType.VARCHAR);
        usuarios.addColumn("fecha_registro",DataType.VARCHAR);
        usuarios.addColumn("activo",        DataType.INT);
        tables.put("usuarios", usuarios);

        // ── categorias ────────────────────────────────────────────────────
        Table categorias = new Table("categorias");
        categorias.addColumn("id",          DataType.INT);
        categorias.addColumn("nombre",      DataType.VARCHAR);
        categorias.addColumn("descripcion", DataType.VARCHAR);
        tables.put("categorias", categorias);

        // ── productos ─────────────────────────────────────────────────────
        Table productos = new Table("productos");
        productos.addColumn("id",           DataType.INT);
        productos.addColumn("nombre",       DataType.VARCHAR);
        productos.addColumn("descripcion",  DataType.VARCHAR);
        productos.addColumn("precio",       DataType.FLOAT);
        productos.addColumn("stock",        DataType.INT);
        productos.addColumn("categoria_id", DataType.INT);
        productos.addColumn("activo",       DataType.INT);
        tables.put("productos", productos);

        // ── departamentos ─────────────────────────────────────────────────
        Table departamentos = new Table("departamentos");
        departamentos.addColumn("id",       DataType.INT);
        departamentos.addColumn("nombre",   DataType.VARCHAR);
        departamentos.addColumn("gerente_id",DataType.INT);
        departamentos.addColumn("presupuesto",DataType.FLOAT);
        tables.put("departamentos", departamentos);

        // ── empleados ─────────────────────────────────────────────────────
        Table empleados = new Table("empleados");
        empleados.addColumn("id",              DataType.INT);
        empleados.addColumn("nombre",          DataType.VARCHAR);
        empleados.addColumn("apellido",        DataType.VARCHAR);
        empleados.addColumn("email",           DataType.VARCHAR);
        empleados.addColumn("salario",         DataType.FLOAT);
        empleados.addColumn("departamento_id", DataType.INT);
        empleados.addColumn("fecha_contrato",  DataType.VARCHAR);
        empleados.addColumn("activo",          DataType.INT);
        tables.put("empleados", empleados);

        // ── pedidos ───────────────────────────────────────────────────────
        Table pedidos = new Table("pedidos");
        pedidos.addColumn("id",           DataType.INT);
        pedidos.addColumn("usuario_id",   DataType.INT);
        pedidos.addColumn("empleado_id",  DataType.INT);
        pedidos.addColumn("fecha",        DataType.VARCHAR);
        pedidos.addColumn("estado",       DataType.VARCHAR);
        pedidos.addColumn("total",        DataType.FLOAT);
        tables.put("pedidos", pedidos);

        // ── pedido_items ──────────────────────────────────────────────────
        Table items = new Table("pedido_items");
        items.addColumn("id",           DataType.INT);
        items.addColumn("pedido_id",    DataType.INT);
        items.addColumn("producto_id",  DataType.INT);
        items.addColumn("cantidad",     DataType.INT);
        items.addColumn("precio_unit",  DataType.FLOAT);
        items.addColumn("subtotal",     DataType.FLOAT);
        tables.put("pedido_items", items);
    }

    public Table findTable(String name) {
        if (name == null) return null;
        for (Map.Entry<String, Table> e : tables.entrySet())
            if (e.getKey().equalsIgnoreCase(name)) return e.getValue();
        return null;
    }

    public Map<String, Table> getAllTables() { return tables; }

    public String getAvailableTables() {
        return String.join(", ", tables.keySet());
    }
}
