package com.dataquery.controller;

import com.dataquery.model.CompilerRequest;
import com.dataquery.model.CompilerResponse;
import com.dataquery.model.User;
import com.dataquery.service.CompilerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class CompilerController {

    @Autowired
    private CompilerService compilerService;

    @PostMapping("/compile")
    public ResponseEntity<CompilerResponse> compile(@RequestBody CompilerRequest request,
                                                    @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(compilerService.compile(request, user));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status",  "ok",
            "message", "DataQuery SQL Compiler API funcionando correctamente",
            "version", "2.0"
        ));
    }

    @GetMapping("/databases")
    public ResponseEntity<List<Map<String, String>>> getDatabases() {
        return ResponseEntity.ok(List.of(
            db("mysql",      "MySQL",      "Base de datos relacional open source más popular", "🐬"),
            db("postgresql", "PostgreSQL", "BD objeto-relacional avanzada y extensible",        "🐘"),
            db("sqlserver",  "SQL Server", "BD empresarial de Microsoft con T-SQL",             "🏢"),
            db("mongodb",    "MongoDB",    "BD NoSQL orientada a documentos JSON/BSON",         "🍃")
        ));
    }

    @GetMapping("/schema")
    public ResponseEntity<List<Map<String, Object>>> getSchema() {
        return ResponseEntity.ok(List.of(
            table("usuarios",      cols("id:INT","nombre:VARCHAR","apellido:VARCHAR","email:VARCHAR","edad:INT","ciudad:VARCHAR","fecha_registro:VARCHAR","activo:INT")),
            table("categorias",    cols("id:INT","nombre:VARCHAR","descripcion:VARCHAR")),
            table("productos",     cols("id:INT","nombre:VARCHAR","descripcion:VARCHAR","precio:FLOAT","stock:INT","categoria_id:INT","activo:INT")),
            table("departamentos", cols("id:INT","nombre:VARCHAR","gerente_id:INT","presupuesto:FLOAT")),
            table("empleados",     cols("id:INT","nombre:VARCHAR","apellido:VARCHAR","email:VARCHAR","salario:FLOAT","departamento_id:INT","fecha_contrato:VARCHAR","activo:INT")),
            table("pedidos",       cols("id:INT","usuario_id:INT","empleado_id:INT","fecha:VARCHAR","estado:VARCHAR","total:FLOAT")),
            table("pedido_items",  cols("id:INT","pedido_id:INT","producto_id:INT","cantidad:INT","precio_unit:FLOAT","subtotal:FLOAT"))
        ));
    }

    @GetMapping("/examples")
    public ResponseEntity<List<Map<String, String>>> getExamples() {
        return ResponseEntity.ok(List.of(
            ex("SELECT básico",            "SELECT id, nombre, email FROM usuarios WHERE activo = 1"),
            ex("SELECT con JOIN",          "SELECT u.nombre, p.total, p.fecha\nFROM usuarios u\nINNER JOIN pedidos p ON u.id = p.usuario_id\nWHERE p.estado = 'completado'\nORDER BY p.fecha DESC\nLIMIT 10"),
            ex("SELECT con agregación",    "SELECT d.nombre, COUNT(e.id) AS total_empleados, AVG(e.salario) AS salario_promedio\nFROM departamentos d\nINNER JOIN empleados e ON d.id = e.departamento_id\nGROUP BY d.nombre\nHAVING AVG(e.salario) > 5000\nORDER BY salario_promedio DESC"),
            ex("INSERT",                   "INSERT INTO usuarios (nombre, apellido, email, edad, ciudad)\nVALUES ('Juan', 'García', 'juan@mail.com', 28, 'Guatemala')"),
            ex("UPDATE con WHERE",         "UPDATE productos\nSET precio = precio * 1.10, activo = 1\nWHERE categoria_id = 2 AND stock > 0"),
            ex("DELETE con WHERE",         "DELETE FROM pedido_items\nWHERE pedido_id = 15 AND cantidad = 0"),
            ex("CREATE TABLE",             "CREATE TABLE IF NOT EXISTS clientes (\n  id INT AUTO_INCREMENT PRIMARY KEY,\n  nombre VARCHAR(100) NOT NULL,\n  email VARCHAR(150) UNIQUE NOT NULL,\n  fecha_registro DATE DEFAULT NULL\n)"),
            ex("ALTER TABLE",              "ALTER TABLE empleados\nADD COLUMN telefono VARCHAR(20) NOT NULL DEFAULT '0000-0000'"),
            ex("DROP TABLE",               "DROP TABLE IF EXISTS clientes"),
            ex("SELECT * (mala práctica)", "SELECT * FROM productos")
        ));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Map<String, String> db(String id, String name, String desc, String icon) {
        return Map.of("id", id, "name", name, "description", desc, "icon", icon);
    }

    private Map<String, Object> table(String name, List<Map<String, String>> columns) {
        return Map.of("name", name, "columns", columns);
    }

    private List<Map<String, String>> cols(String... defs) {
        List<Map<String, String>> list = new ArrayList<>();
        for (String d : defs) {
            String[] parts = d.split(":");
            list.add(Map.of("name", parts[0], "type", parts[1]));
        }
        return list;
    }

    private Map<String, String> ex(String title, String query) {
        return Map.of("title", title, "query", query);
    }
}
