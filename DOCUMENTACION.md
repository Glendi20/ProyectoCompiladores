# DataQuery SQL Compiler — Documentación Completa
### Proyecto Final · Compiladores · UMG 2026

---

## ÍNDICE

1. ¿Qué es DataQuery?
2. Stack tecnológico y por qué se usó cada tecnología
3. Arquitectura general del sistema
4. Estructura de carpetas
5. El Compilador SQL — las 4 fases en detalle
6. Qué SQL soporta el compilador
7. La Tabla de Símbolos
8. Sistema de autenticación JWT
9. Optimizador e IA Tipo 3
10. Dashboard y métricas reales
11. Frontend — componentes y pantallas
12. Pipeline Visual, Log y Tabla de Símbolos (UI)
13. Patrones de diseño usados
14. Flujo de datos de extremo a extremo
15. Pruebas del sistema

---

## 1. ¿QUÉ ES DATAQUERY?

DataQuery es un **compilador SQL completo** implementado en Java que analiza consultas SQL
en tiempo real a través de una interfaz web. No ejecuta las consultas en una base de datos real,
sino que las **procesa como un compilador**: las tokeniza, construye un árbol sintáctico,
valida su semántica y genera sugerencias de optimización personalizadas.

**¿Para qué sirve?**
- Aprender a escribir SQL correcto antes de ejecutarlo en producción
- Detectar errores peligrosos (UPDATE sin WHERE, DROP DATABASE, etc.)
- Recibir sugerencias de rendimiento personalizadas según tu historial
- Ver cómo un compilador real procesa código fuente, fase por fase

---

## 2. STACK TECNOLÓGICO Y POR QUÉ SE USÓ CADA UNO

```
┌─────────────────────────────────────────────────────────────────┐
│                        FRONTEND                                  │
│                                                                  │
│  React 18 + Vite          ← UI reactiva, hot reload en dev       │
│  Monaco Editor            ← El mismo editor de VS Code           │
│  React Router             ← Navegación sin recargar la página    │
│  Recharts                 ← Gráficas del dashboard               │
│  Axios                    ← HTTP con interceptores JWT            │
└─────────────────────────────────────────────────────────────────┘
                             │ HTTP REST (JSON)
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                        BACKEND                                   │
│                                                                  │
│  Java 22 + Spring Boot 3  ← API REST robusta, auto-config        │
│  Spring Security          ← Autenticación y autorización         │
│  JWT (jjwt 0.11.5)        ← Tokens sin estado, seguros           │
│  BCrypt (factor 12)       ← Hash de contraseñas irreversible      │
│  Spring Data JPA          ← ORM, queries sin SQL manual           │
│  Maven                    ← Gestión de dependencias y build       │
└─────────────────────────────────────────────────────────────────┘
                             │ JDBC / JPA
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                     BASE DE DATOS                                │
│                                                                  │
│  SQL Server 2022          ← Persistencia de usuarios,            │
│  (servidor servidor-bd)         historial de consultas,             │
│  BD: nombre_base_de_datos            frecuencia de errores               │
└─────────────────────────────────────────────────────────────────┘
```

### ¿Por qué React y no HTML puro?
React permite actualizar solo las partes de la pantalla que cambiaron.
Cuando analizas una consulta, solo se actualiza el panel de resultados y el pipeline,
no toda la página. Esto hace la app mucho más rápida y fluida.

### ¿Por qué Monaco Editor y no un textarea?
Monaco es el mismo motor de VS Code. Tiene resaltado de sintaxis SQL,
numeración de líneas, y permite agregar atajos de teclado (Ctrl+Enter para analizar).
Un textarea normal no tiene nada de esto.

### ¿Por qué Spring Boot y no otro framework?
Spring Boot configura automáticamente el servidor web, la seguridad, la conexión
a la base de datos y las rutas REST. Lo que manualmente tomaría días, Spring Boot
lo hace automáticamente al arrancar.

### ¿Por qué JWT y no sesiones?
JWT (JSON Web Token) es **stateless**: el servidor no guarda ninguna sesión.
El token viaja en cada petición y el servidor solo verifica su firma criptográfica.
Esto es escalable: si se agregan más servidores, todos validan el mismo token sin
necesidad de compartir estado entre ellos.

### ¿Por qué SQL Server?
Es uno de los 4 motores que el proyecto debe soportar, y es el disponible en el
entorno de desarrollo (servidor servidor-bd). La BD persiste el historial real de
consultas de cada usuario, que luego alimenta las sugerencias personalizadas (IA Tipo 3).

---

## 3. ARQUITECTURA GENERAL DEL SISTEMA

```
NAVEGADOR (localhost:3000)
│
│  1. Usuario escribe SQL en Monaco Editor
│  2. Presiona "Analizar Consulta" o Ctrl+Enter
│  3. React hace POST /api/compile con el SQL y el motor seleccionado
│
│  [Axios agrega automáticamente el JWT en el header Authorization]
│
▼
SPRING BOOT (localhost:8080)
│
│  JwtAuthFilter valida el token
│  │
│  CompilerController recibe la petición
│  │
│  CompilerService orquesta las 4 fases:
│  │
│  ├── FASE 1: Lexer.tokenize(sql)
│  │     └─→ List<Token>
│  │
│  ├── FASE 2: Parser.parse(tokens)
│  │     └─→ StatementNode (AST)
│  │
│  ├── FASE 3: SemanticAnalyzer.analyze(ast)
│  │     └─→ errors[], warnings[]
│  │
│  └── FASE 4: OptimizerService.analyze(ast, db, user)
│        └─→ suggestions[]
│
│  CompilerService guarda el historial en SQL Server
│  Construye CompilerResponse y lo devuelve como JSON
│
▼
RESPUESTA JSON al navegador:
{
  valid: true/false,
  tokens: [...],
  ast: "SELECT\n  Columns: ...",
  statementType: "SELECT",
  errors: [...],
  warnings: [...],
  suggestions: [...],
  dialectNote: "MySQL 8.0 — Motor InnoDB...",
  optimizedQuery: "SELECT id, nombre FROM ..."
}
│
▼
REACT actualiza:
  - PipelineVisual (4 fases con estado)
  - Tab Resultado (errores/advertencias)
  - Tab Tokens
  - Tab AST
  - Tab Sugerencias
  - Tab Log
  - Tab Símbolos
```

---

## 4. ESTRUCTURA DE CARPETAS

```
Proyecto Final Copiladores/
│
├── backend/                              ← Proyecto Spring Boot (Java)
│   └── src/main/java/com/dataquery/
│       │
│       ├── compiler/                     ← EL COMPILADOR SQL
│       │   ├── lexer/
│       │   │   ├── Lexer.java            ← FASE 1: divide el SQL en tokens
│       │   │   ├── Token.java            ← Un token: tipo + valor + línea + columna
│       │   │   └── TokenType.java        ← Enum con todos los tipos posibles
│       │   │
│       │   ├── parser/
│       │   │   └── Parser.java           ← FASE 2: construye el AST a partir de tokens
│       │   │
│       │   ├── ast/                      ← Nodos del Árbol Sintáctico Abstracto
│       │   │   ├── StatementNode.java    ← Clase base de todos los nodos
│       │   │   ├── SelectNode.java       ← Representa un SELECT completo
│       │   │   ├── InsertNode.java       ← Representa un INSERT
│       │   │   ├── UpdateNode.java       ← Representa un UPDATE
│       │   │   ├── DeleteNode.java       ← Representa un DELETE
│       │   │   ├── CreateTableNode.java  ← Representa un CREATE TABLE
│       │   │   ├── AlterTableNode.java   ← Representa un ALTER TABLE
│       │   │   ├── DropNode.java         ← Representa un DROP
│       │   │   ├── ConditionNode.java    ← Una condición WHERE/HAVING
│       │   │   ├── JoinNode.java         ← Un JOIN con su tipo y condición ON
│       │   │   ├── ExpressionNode.java   ← Un valor: número, string, columna
│       │   │   └── OrderByClause.java    ← Una columna ORDER BY con ASC/DESC
│       │   │
│       │   ├── semantic/
│       │   │   └── SemanticAnalyzer.java ← FASE 3: valida tablas, columnas y tipos
│       │   │
│       │   └── symboltable/
│       │       ├── SymbolTable.java      ← Las 7 tablas del schema con columnas y tipos
│       │       ├── Table.java            ← Una tabla con su lista de columnas
│       │       ├── Column.java           ← Una columna con nombre y tipo
│       │       └── DataType.java         ← Enum: INT, VARCHAR, FLOAT, BOOLEAN, DATE, TEXT
│       │
│       ├── service/
│       │   ├── CompilerService.java      ← Orquesta las 4 fases, guarda historial
│       │   ├── OptimizerService.java     ← FASE 4: genera sugerencias de optimización
│       │   └── UserProfileService.java   ← Construye el perfil del usuario para IA Tipo 3
│       │
│       ├── controller/
│       │   ├── CompilerController.java   ← POST /api/compile
│       │   ├── AuthController.java       ← POST /api/auth/register y /api/auth/login
│       │   ├── DashboardController.java  ← GET /api/dashboard/my-stats
│       │   └── SchemaController.java     ← GET /api/schema y /api/databases
│       │
│       ├── security/
│       │   ├── JwtUtil.java              ← Genera y valida tokens JWT
│       │   ├── JwtAuthFilter.java        ← Intercepta cada petición y valida el JWT
│       │   └── SecurityConfig.java       ← Define qué rutas son públicas y cuáles protegidas
│       │
│       ├── model/
│       │   ├── User.java                 ← Entidad usuario (tabla users en SQL Server)
│       │   ├── QueryHistory.java         ← Entidad historial de consultas
│       │   └── ErrorFrequency.java       ← Entidad frecuencia de errores (para IA)
│       │
│       ├── repository/
│       │   ├── UserRepository.java       ← Acceso a tabla users
│       │   ├── QueryHistoryRepository.java ← Acceso a tabla query_history
│       │   └── ErrorFrequencyRepository.java ← Acceso a tabla error_frequency
│       │
│       └── config/
│           └── DataInitializer.java      ← Crea la cuenta profesor@dataquery.com al inicio
│
├── frontend/                             ← Proyecto React + Vite
│   └── src/
│       ├── App.jsx                       ← Router principal con todas las rutas
│       ├── main.jsx                      ← Punto de entrada de React
│       │
│       ├── context/
│       │   └── AuthContext.jsx           ← Estado global de autenticación
│       │
│       ├── services/
│       │   └── api.js                    ← Axios con interceptor JWT automático
│       │
│       ├── pages/
│       │   ├── Editor.jsx                ← Página principal del compilador
│       │   ├── Dashboard.jsx             ← Dashboard del estudiante
│       │   └── ProfessorDashboard.jsx    ← Vista del administrador/profesor
│       │
│       └── components/
│           ├── PipelineVisual.jsx        ← Barra visual de las 4 fases del compilador
│           ├── ResultPanel.jsx           ← Panel derecho con 6 tabs de resultados
│           ├── SqlEditor.jsx             ← Monaco Editor con resaltado SQL
│           ├── SchemaPanel.jsx           ← Panel izquierdo con tablas y columnas
│           ├── DatabaseSelector.jsx      ← Selector MySQL/PostgreSQL/SQL Server/MongoDB
│           ├── auth/
│           │   ├── Login.jsx             ← Formulario de inicio de sesión
│           │   └── Register.jsx          ← Formulario de registro
│           ├── common/
│           │   ├── Navbar.jsx            ← Barra de navegación superior
│           │   └── PrivateRoute.jsx      ← Protege rutas que requieren login
│           └── dashboard/
│               ├── MetricCard.jsx        ← Tarjeta con número grande
│               ├── StatsChart.jsx        ← Gráfica de barras (Recharts)
│               ├── ErrorsChart.jsx       ← Gráfica de errores frecuentes
│               └── ActivityFeed.jsx      ← Lista de últimas consultas
│
├── backend/src/test/                     ← Pruebas unitarias e integración
│   └── java/com/dataquery/compiler/
│       ├── lexer/LexerTest.java          ← 35+ pruebas del Lexer
│       ├── parser/ParserTest.java        ← 40+ pruebas del Parser
│       ├── semantic/SemanticAnalyzerTest.java ← 35+ pruebas del Analizador Semántico
│       └── pipeline/
│           ├── CompilerPipelineTest.java ← 25+ pruebas del pipeline completo
│           └── PipelineDataTest.java     ← 40 pruebas de los nuevos componentes UI
│
└── test-nuevas-funciones.ps1             ← Script PowerShell: ~60 pruebas de integración
```

---

## 5. EL COMPILADOR SQL — LAS 4 FASES EN DETALLE

El compilador convierte una cadena de texto SQL en información estructurada
pasando por 4 fases secuenciales, igual que un compilador de lenguaje de programación.

```
Texto SQL ingresado por el usuario
           │
           ▼
┌──────────────────────┐
│   FASE 1: LÉXICO     │  Lexer.java
│   (Tokenización)     │
└──────────────────────┘
           │
           │  List<Token>
           ▼
┌──────────────────────┐
│  FASE 2: SINTÁCTICO  │  Parser.java
│  (Construcción AST)  │
└──────────────────────┘
           │
           │  StatementNode (AST)
           ▼
┌──────────────────────┐
│  FASE 3: SEMÁNTICO   │  SemanticAnalyzer.java
│  (Validación lógica) │
└──────────────────────┘
           │
           │  errors[], warnings[]
           ▼
┌──────────────────────┐
│  FASE 4: OPTIMIZADOR │  OptimizerService.java
│  (Sugerencias IA)    │
└──────────────────────┘
           │
           │  suggestions[]
           ▼
      CompilerResponse (JSON)
```

---

### FASE 1 — ANÁLISIS LÉXICO (Lexer.java)

**¿Qué hace?**
Recorre el texto SQL carácter por carácter y lo divide en **tokens**.
Un token es la unidad mínima con significado: una palabra clave, un número,
un operador, un identificador, etc.

**Ejemplo:**
```
SQL de entrada:  SELECT nombre FROM usuarios WHERE id = 1

Tokens generados:
  Token[SELECT,    "SELECT", línea:1, col:1]
  Token[IDENTIFIER,"nombre", línea:1, col:8]
  Token[FROM,      "FROM",   línea:1, col:15]
  Token[IDENTIFIER,"usuarios",línea:1, col:20]
  Token[WHERE,     "WHERE",  línea:1, col:29]
  Token[IDENTIFIER,"id",     línea:1, col:35]
  Token[EQUAL,     "=",      línea:1, col:38]
  Token[NUMBER,    "1",      línea:1, col:40]
  Token[END_OF_FILE,"",      línea:1, col:41]
```

**¿Cómo funciona internamente?**
```
Lexer lee carácter por carácter:
  'S' → ¿es letra? → acumula hasta espacio → "SELECT"
       → ¿está en KEYWORDS map? → sí → TokenType.SELECT

  '=' → caso especial → TokenType.EQUAL
  '>' → ¿siguiente es '='? → TokenType.GREATER_EQUAL
        ¿no? → TokenType.GREATER

  '\'' → modo string → acumula hasta '\'' → TokenType.STRING
  '1'  → ¿es dígito? → acumula hasta no-dígito → TokenType.NUMBER
  '--' → comentario de línea → ignora hasta fin de línea
  '/*' → comentario de bloque → ignora hasta '*/'
```

**Archivo:** `backend/src/main/java/com/dataquery/compiler/lexer/Lexer.java`

**Dónde se usa:** `CompilerService.java` línea 49 — `new Lexer(query).tokenize()`

---

### FASE 2 — ANÁLISIS SINTÁCTICO (Parser.java)

**¿Qué hace?**
Toma la lista de tokens y construye un **Árbol Sintáctico Abstracto (AST)**.
El AST es una estructura de datos que representa la consulta de forma organizada.
Si los tokens no forman una consulta SQL válida, lanza una excepción con el error.

**Ejemplo:**
```
Tokens: SELECT, nombre, FROM, usuarios, WHERE, id, =, 1

AST generado (SelectNode):
  SelectNode
  ├── columns: ["nombre"]
  ├── tableName: "usuarios"
  ├── selectAll: false
  └── whereCondition: ConditionNode
        ├── left: ExpressionNode(COLUMN, "id")
        ├── op: EQUAL
        └── right: ExpressionNode(NUMBER, "1")
```

**¿Cómo funciona internamente?**
```
El Parser tiene un cursor sobre la lista de tokens.
Cada método consume tokens y construye partes del AST:

parse()
 ├── ¿WITH? → parseWithClauses() → guarda nombres de CTEs
 └── parseStatement()
       ├── ¿SELECT? → parseSelect()
       │     ├── parseColumnList() → lee columnas hasta FROM
       │     ├── parseFrom() → lee nombre de tabla y alias
       │     ├── parseJoins() → lee INNER/LEFT/RIGHT/FULL JOIN
       │     ├── parseWhere() → parseSingleCondition() + AND/OR encadenados
       │     ├── parseGroupBy() → lee columnas
       │     ├── parseHaving() → parseSingleCondition()
       │     ├── parseOrderBy() → columna + ASC/DESC
       │     └── parseLimit() → número / TOP N / FETCH NEXT N
       │
       ├── ¿INSERT? → parseInsert()
       ├── ¿UPDATE? → parseUpdate()
       ├── ¿DELETE? → parseDelete()
       ├── ¿CREATE? → parseCreate()
       ├── ¿ALTER?  → parseAlter()
       └── ¿DROP?   → parseDrop()
```

**El método clave — consumeTokensAsString():**
```java
// Cuando el Parser encuentra una expresión compleja que no puede
// parsear estructura a estructura (CASE WHEN, window functions,
// aritmética, subqueries en SELECT), usa este método para
// consumir todos los tokens hasta una coma o FROM y guardarlos
// como string. Esto permite que el compilador NO falle con SQL avanzado.

String consumeTokensAsString(boolean stopAtComma)
```

**Archivo:** `backend/src/main/java/com/dataquery/compiler/parser/Parser.java`

**Dónde se usa:** `CompilerService.java` línea 59 — `new Parser(tokens).parse()`

---

### FASE 3 — ANÁLISIS SEMÁNTICO (SemanticAnalyzer.java)

**¿Qué hace?**
Valida que la consulta tenga sentido lógico contra el schema de la base de datos.
El análisis sintáctico verificó que la estructura es correcta (gramática),
el análisis semántico verifica que las tablas y columnas existan y los tipos sean compatibles.

**Reglas que aplica:**

| Qué verifica | Error / Advertencia |
|---|---|
| La tabla existe en el schema | Warning (puede ser CTE o tabla real del usuario) |
| La columna existe en la tabla | ERROR |
| UPDATE sin WHERE | ERROR — "PELIGRO CRÍTICO" |
| DELETE sin WHERE | ERROR — "PELIGRO CRÍTICO" |
| DROP DATABASE | ERROR — "EXTREMADAMENTE PELIGROSO" |
| Tipos incompatibles en WHERE | ERROR — VARCHAR = NUMBER |
| CREATE TABLE sin PRIMARY KEY | Warning |
| CREATE TABLE columna duplicada | ERROR |
| ALTER TABLE columna inexistente | ERROR |
| INSERT columnas ≠ valores | ERROR |
| ORDER BY alias de SELECT | OK — no genera error |
| CASE WHEN con espacios | OK — expresión compleja permitida |
| CTE en tabla desconocida | OK — los nombres de CTEs se ignoran |

**¿Cómo sabe qué tablas y columnas existen?**
Usa la **Tabla de Símbolos** (SymbolTable.java) que tiene 7 tablas predefinidas
con sus columnas y tipos. Ver sección 7 para el detalle completo.

**Archivo:** `backend/src/main/java/com/dataquery/compiler/semantic/SemanticAnalyzer.java`

**Dónde se usa:** `CompilerService.java` línea 75 — `new SemanticAnalyzer(symbolTable)`

---

### FASE 4 — OPTIMIZADOR / IA TIPO 3 (OptimizerService.java)

**¿Qué hace?**
Analiza el AST y genera sugerencias educativas personalizadas.
Se llama "IA Tipo 3" porque usa el historial real del usuario en SQL Server
para personalizar los mensajes. No es machine learning — es lógica basada en reglas
que se adapta según el perfil del usuario.

**Las 12 reglas implementadas:**

| # | Regla | Tipo |
|---|---|---|
| 1 | SELECT * — personalizado según historial | optimization |
| 2 | Sin WHERE en SELECT — escanea toda la tabla | best-practice |
| 3 | Sin LIMIT — retorna miles de filas | best-practice |
| 4 | DISTINCT puede ser síntoma de JOIN mal hecho | best-practice |
| 5 | JOINs sin índice en columna ON | optimization |
| 6 | GROUP BY sin función de agregación | best-practice |
| 7 | ORDER BY sin LIMIT es costoso | optimization |
| 8 | Subquery en WHERE → reescribir como JOIN | optimization |
| 9 | Window Function sin PARTITION BY | best-practice |
| 10 | CASE WHEN sin ELSE → devuelve NULL silencioso | best-practice |
| 11 | UNION → sugiere UNION ALL si aplica | optimization |
| 12 | CTE → advertencia sobre re-ejecución múltiple | best-practice |
| + | Sugerencia de dialecto (MySQL/PostgreSQL/SQL Server/MongoDB) | dialect |
| + | Peligros detectados (UPDATE/DELETE sin WHERE) | danger |
| + | Sugerencias personalizadas del historial | personal |

**¿Cómo funciona la personalización (IA Tipo 3)?**
```
UserProfileService.buildProfile(user)
  │
  └── Consulta SQL Server: historial de los últimos 30 días del usuario
        ├── cuántas veces usó SELECT *
        ├── cuántas consultas totales tiene
        ├── si es usuario nuevo (< 5 consultas)
        └── qué errores comete con más frecuencia

  └── Devuelve UserProfile con:
        nombre, isNew(), selectStarCount, totalQueries

OptimizerService usa UserProfile para personalizar mensajes:
  - Si selectStarCount >= 5: "¡Llevas 5 veces usando SELECT *! Ya es hora de romper este hábito"
  - Si selectStarCount >= 2: "Ya usaste SELECT * 2 veces"
  - Si es nuevo: mensaje básico explicativo
```

**Archivo:** `backend/src/main/java/com/dataquery/service/OptimizerService.java`

---

## 6. QUÉ SQL SOPORTA EL COMPILADOR

### DQL — Consultas de datos
```sql
-- SELECT básico
SELECT id, nombre, email FROM usuarios

-- SELECT con funciones
SELECT COUNT(*), SUM(salario), AVG(precio), MAX(id), MIN(id) FROM tabla

-- SELECT con alias
SELECT nombre AS n, apellido AS a FROM usuarios AS u

-- SELECT DISTINCT
SELECT DISTINCT ciudad FROM usuarios

-- SELECT con WHERE encadenado
SELECT * FROM usuarios WHERE ciudad = 'Guatemala' AND edad > 18 OR activo = 1

-- SELECT con BETWEEN, IN, LIKE, IS NULL
SELECT * FROM productos WHERE precio BETWEEN 10 AND 100
SELECT * FROM empleados WHERE departamento_id IN (1, 2, 3)
SELECT * FROM usuarios WHERE nombre LIKE '%García%'
SELECT * FROM pedidos WHERE fecha IS NOT NULL

-- SELECT con todos los tipos de JOIN
SELECT * FROM pedidos p INNER JOIN usuarios u ON p.usuario_id = u.id
SELECT * FROM a LEFT JOIN b ON a.id = b.a_id
SELECT * FROM a RIGHT JOIN b ON a.id = b.a_id
SELECT * FROM a FULL OUTER JOIN b ON a.id = b.a_id
SELECT * FROM a CROSS JOIN b

-- SELECT con GROUP BY + HAVING
SELECT ciudad, COUNT(*) AS total FROM usuarios GROUP BY ciudad HAVING COUNT(*) > 5

-- SELECT con ORDER BY + LIMIT
SELECT * FROM productos ORDER BY precio DESC LIMIT 20 OFFSET 40

-- SELECT con CASE WHEN
SELECT nombre,
  CASE WHEN salario > 5000 THEN 'Senior'
       WHEN salario > 3000 THEN 'Mid'
       ELSE 'Junior' END AS nivel
FROM empleados

-- SELECT con subquery en WHERE
SELECT nombre FROM empleados WHERE departamento_id IN (
  SELECT id FROM departamentos WHERE presupuesto > 10000
)

-- SELECT con EXISTS
SELECT * FROM usuarios u WHERE EXISTS (SELECT 1 FROM pedidos WHERE usuario_id = u.id)

-- SELECT con Window Functions
SELECT nombre, salario,
  ROW_NUMBER() OVER (PARTITION BY departamento_id ORDER BY salario DESC) AS ranking
FROM empleados

-- SELECT con CTE (WITH)
WITH ventas AS (
  SELECT usuario_id, SUM(total) AS monto FROM pedidos GROUP BY usuario_id
)
SELECT u.nombre, v.monto FROM usuarios u INNER JOIN ventas v ON u.id = v.usuario_id

-- UNION / UNION ALL / INTERSECT / EXCEPT
SELECT nombre FROM usuarios UNION ALL SELECT nombre FROM empleados

-- SQL Server: TOP
SELECT TOP 10 * FROM productos ORDER BY precio DESC

-- SQL Server / PostgreSQL: FETCH NEXT
SELECT * FROM empleados ORDER BY salario DESC FETCH NEXT 5 ROWS ONLY
```

### DML — Manipulación de datos
```sql
INSERT INTO usuarios (nombre, apellido, email) VALUES ('Juan', 'Pérez', 'j@mail.com')
INSERT INTO productos (nombre, precio) VALUES ('Laptop', 999.99), ('Mouse', 25.00)
UPDATE empleados SET salario = 7500, activo = 1 WHERE id = 12
DELETE FROM pedidos WHERE estado = 'cancelado' AND fecha < '2025-01-01'
```

### DDL — Definición de estructura
```sql
CREATE TABLE clientes (
  id INT PRIMARY KEY AUTO_INCREMENT,
  nombre VARCHAR(200) NOT NULL,
  email VARCHAR(100) UNIQUE,
  saldo DECIMAL(10,2) DEFAULT 0.0
)
CREATE TABLE IF NOT EXISTS logs (...)
ALTER TABLE empleados ADD COLUMN telefono VARCHAR(20)
ALTER TABLE empleados DROP COLUMN fecha_nacimiento
ALTER TABLE old_name RENAME TO new_name
DROP TABLE IF EXISTS tabla_temporal
DROP DATABASE produccion   ← detectado como PELIGRO EXTREMO
```

### DCL / TCL — Control
```sql
BEGIN TRANSACTION
COMMIT
ROLLBACK
SAVEPOINT punto1
```

---

## 7. LA TABLA DE SÍMBOLOS

La Tabla de Símbolos es el "diccionario" del compilador. Contiene el schema
de la base de datos que el Analizador Semántico usa para validar columnas y tipos.

**Archivo:** `backend/src/main/java/com/dataquery/compiler/symboltable/SymbolTable.java`

```
SymbolTable (7 tablas)
│
├── usuarios
│   ├── id            INT
│   ├── nombre        VARCHAR
│   ├── apellido      VARCHAR
│   ├── email         VARCHAR
│   ├── edad          INT
│   ├── ciudad        VARCHAR
│   ├── fecha_registro VARCHAR
│   └── activo        INT
│
├── productos
│   ├── id            INT
│   ├── nombre        VARCHAR
│   ├── descripcion   VARCHAR
│   ├── precio        FLOAT
│   ├── stock         INT
│   ├── categoria_id  INT
│   └── activo        INT
│
├── categorias
│   ├── id            INT
│   ├── nombre        VARCHAR
│   └── descripcion   VARCHAR
│
├── empleados
│   ├── id            INT
│   ├── nombre        VARCHAR
│   ├── apellido      VARCHAR
│   ├── email         VARCHAR
│   ├── salario       FLOAT
│   ├── departamento_id INT
│   ├── fecha_contrato VARCHAR
│   └── activo        INT
│
├── departamentos
│   ├── id            INT
│   ├── nombre        VARCHAR
│   ├── gerente_id    INT
│   └── presupuesto   FLOAT
│
├── pedidos
│   ├── id            INT
│   ├── usuario_id    INT
│   ├── empleado_id   INT
│   ├── fecha         VARCHAR
│   ├── estado        VARCHAR
│   └── total         FLOAT
│
└── pedido_items
    ├── id            INT
    ├── pedido_id     INT
    ├── producto_id   INT
    ├── cantidad      INT
    ├── precio_unit   FLOAT
    └── subtotal      FLOAT
```

**Tipos de datos soportados:**
- `INT` — enteros (id, cantidad, stock)
- `VARCHAR` — texto variable (nombre, email, estado)
- `FLOAT` — decimales (precio, salario, total)
- `BOOLEAN` — verdadero/falso
- `DATE` — fechas
- `TEXT` — texto largo

**Reglas de compatibilidad de tipos:**
```
INT   == INT    → compatible
INT   == FLOAT  → compatible (numérico con numérico)
FLOAT == FLOAT  → compatible
VARCHAR == VARCHAR → compatible
VARCHAR == INT  → INCOMPATIBLE → ERROR semántico
INT   == VARCHAR → INCOMPATIBLE → ERROR semántico
```

---

## 8. SISTEMA DE AUTENTICACIÓN JWT

### Flujo completo de registro e inicio de sesión

```
REGISTRO:
  Frontend (Register.jsx)
       │ POST /api/auth/register
       │ { nombre, apellido, email, password }
       ▼
  AuthController.register()
       │
       ├── Valida formato (email válido, password >= 8 chars, mayúscula, número)
       ├── Verifica que el email no exista ya en la BD
       ├── BCrypt.encode(password, factor=12) → hash irreversible
       ├── Guarda User en SQL Server
       └── Genera JWT → devuelve { token, nombre, apellido, email, role }

  Frontend guarda en localStorage: dq_token, dq_user
  React AuthContext actualiza el estado global
  Usuario redirigido a /editor


LOGIN:
  Frontend (Login.jsx)
       │ POST /api/auth/login
       │ { email, password }
       ▼
  AuthController.login()
       │
       ├── Busca el usuario por email
       ├── BCrypt.matches(password, hashGuardado) → verifica sin desencriptar
       │   ↳ si falla: incrementa failedAttempts
       │   ↳ si llega a 5: bloquea por 15 minutos
       ├── Genera JWT firmado con secreto + expira en 24 horas
       └── Devuelve { token, userId, nombre, apellido, email, role }
```

### Estructura del JWT
```
JWT = base64(header) . base64(payload) . firma

header:  { alg: "HS256", typ: "JWT" }
payload: { sub: "usuario@email.com", iat: 1716300000, exp: 1716386400 }
firma:   HMAC-SHA256(header + "." + payload, secreto)

El secreto se configura como variable de entorno en el servidor.
```

### Cómo se valida cada petición
```
Petición llega al backend
       │
       ▼
JwtAuthFilter.doFilterInternal()
       │
       ├── Lee el header: Authorization: Bearer eyJ0eXAi...
       ├── JwtUtil.extractEmail(token) → descifra el email
       ├── JwtUtil.isTokenValid(token) → verifica firma + no expirado
       │
       ├── Si válido: carga el User de la BD y lo pone en el contexto de seguridad
       └── Si inválido: devuelve 401 Unauthorized
                         Frontend recibe 401 → borra localStorage → redirige a /login
```

### Roles
```
STUDENT   → puede compilar, ver su dashboard
PROFESSOR → puede compilar + ver dashboard global de todos los usuarios
```

**Archivos clave:**
- `security/JwtUtil.java` — genera y valida tokens
- `security/JwtAuthFilter.java` — intercepta cada petición
- `security/SecurityConfig.java` — rutas públicas: /auth/**, /h2-console/**
- `config/DataInitializer.java` — crea profesor@dataquery.com al arrancar

---

## 9. OPTIMIZADOR E IA TIPO 3

### ¿Qué es "IA Tipo 3"?
Es el nivel más avanzado de personalización. No es machine learning,
sino un sistema de reglas que se alimenta de datos reales del historial del usuario.

```
SQL Server (nombre_base_de_datos)
│
├── query_history
│   ├── user_id
│   ├── query_preview (primeros 500 chars del SQL)
│   ├── statement_type (SELECT, INSERT, etc.)
│   ├── database_type (mysql, postgresql, etc.)
│   ├── is_valid
│   ├── error_count
│   ├── suggestions_count
│   ├── error_json
│   └── analyzed_at
│
└── error_frequency
    ├── error_pattern (texto normalizado del error)
    ├── statement_type
    ├── database_type
    └── frequency (cuántas veces ocurrió)
```

### Flujo de personalización
```
Usuario analiza una consulta
       │
       ▼
CompilerService llama UserProfileService.buildProfile(user)
       │
       ▼
UserProfileService consulta query_history:
  SELECT COUNT(*) WHERE user_id = ?                     → totalQueries
  SELECT COUNT(*) WHERE user_id = ? AND is_valid = false → failedQueries
  SELECT COUNT(*) WHERE user_id = ? AND query_preview LIKE '%SELECT *%' → selectStarCount
       │
       ▼
Devuelve UserProfile {
  nombre: "Erick",
  totalQueries: 47,
  failedQueries: 12,
  selectStarCount: 8,
  isNew: false
}
       │
       ▼
OptimizerService usa el perfil para personalizar mensajes:
  Si selectStarCount >= 5:
    "¡Llevas 8 veces usando SELECT *! Ya es hora de romper este hábito, Erick"
  Si es nuevo (< 5 consultas):
    "Evita SELECT * — explicación básica"
```

---

## 10. DASHBOARD Y MÉTRICAS REALES

### Dashboard del Estudiante (/dashboard)
```
GET /api/dashboard/my-stats
       │
       └── DashboardController consulta SQL Server:
             ├── Total de consultas del usuario
             ├── Cuántas fueron válidas vs con errores
             ├── Porcentaje de éxito
             ├── Distribución por tipo (SELECT/INSERT/UPDATE/...)
             ├── Distribución por dialecto (MySQL/PostgreSQL/...)
             └── Últimas 10 consultas con preview y timestamp
```

**Componentes del Dashboard:**
```
Dashboard.jsx
├── MetricCard — "Total Consultas: 47"
├── MetricCard — "Exitosas: 35 (74%)"
├── MetricCard — "Con Errores: 12"
├── StatsChart — gráfica de barras por tipo de sentencia
├── ErrorsChart — gráfica de errores más frecuentes
└── ActivityFeed — últimas consultas con preview y dialecto
```

### Dashboard del Administrador (/dashboard/global)
```
GET /api/dashboard/global/stats
       │
       └── Devuelve estadísticas de TODOS los usuarios:
             ├── Total de usuarios registrados
             ├── Total de consultas del sistema
             └── Errores más frecuentes de la plataforma
```

---

## 11. FRONTEND — COMPONENTES Y PANTALLAS

### Flujo de navegación
```
/ (raíz)
└── Redirige a /login si no hay sesión, o a /editor si hay sesión

/login  ← Login.jsx
/register ← Register.jsx

/editor ← Editor.jsx (requiere login)
/dashboard ← Dashboard.jsx (requiere login)
/dashboard/global ← ProfessorDashboard.jsx (requiere rol PROFESSOR)
```

### Editor.jsx — La pantalla principal
```
┌──────────────────────────────────────────────────────────────┐
│  Navbar: DataQuery | Editor SQL | Dashboard | Usuario | Salir │
├──────────────┬───────────────────────────────┬───────────────┤
│  Panel Izq.  │      Panel Central            │  Panel Der.   │
│  260px       │                               │  380px        │
│              │  [PipelineVisual]             │               │
│ DatabaseSel. │  ┌──────────────────────────┐│  ResultPanel  │
│              │  │ PIPELINE                 ││  ├─ Resultado  │
│ SchemaPanel  │  │ [1 LÉXICO] [2 SINTÁCT.] ││  ├─ Tokens    │
│              │  │ [3 SEMÁNT.][4 OPTIM.]   ││  ├─ AST       │
│ ┌──usuarios  │  └──────────────────────────┘│  ├─ Sugerencias│
│ │ id INT     │                               │  ├─ Log       │
│ │ nombre VAR │  [SqlEditor — Monaco]         │  └─ Símbolos  │
│ └──productos │                               │               │
│              │  [ Analizar Consulta ]        │               │
└──────────────┴───────────────────────────────┴───────────────┘
```

### AuthContext.jsx — Estado global de autenticación
```javascript
// Provee a toda la app:
{
  user: { id, nombre, apellido, email, role },
  token: "eyJ0eXAi...",
  login(token, userData) → guarda en estado + localStorage,
  logout() → limpia estado + localStorage + redirige a /login
}
```

### PrivateRoute.jsx — Protección de rutas
```javascript
// Si no hay token → redirige a /login
// Si hay token → renderiza el componente hijo
<Route path="/editor" element={<PrivateRoute><Editor /></PrivateRoute>} />
```

---

## 12. PIPELINE VISUAL, LOG Y TABLA DE SÍMBOLOS

Estos 3 componentes nuevos muestran visualmente el proceso del compilador.

### PipelineVisual.jsx
```
Recibe: result (respuesta del API), loading (boolean)
Muestra: 4 tarjetas horizontales conectadas con flechas

Estado de cada fase:
  idle    → gris, "—"               (sin consulta)
  loading → azul pulsando           (analizando)
  ok      → verde                   (fase exitosa)
  warn    → amarillo/naranja        (solo advertencias)
  error   → rojo                    (errores encontrados)

Métricas que muestra:
  Fase 1: "42 tokens"
  Fase 2: "SELECT"
  Fase 3: "sin errores" / "2 error(s)" / "3 advertencia(s)"
  Fase 4: "5 sugerencia(s)"
```

### Tab "Log" (en ResultPanel)
```
Muestra una traza estilo terminal de todo lo que pasó:

[00:01] [LÉXICO]   Iniciando análisis léxico...
[00:02] [LÉXICO]   42 tokens generados correctamente
[00:03] [SINTÁCT]  Construyendo árbol sintáctico (AST)...
[00:04] [SINTÁCT]  AST generado — tipo: SELECT
[00:05] [SEMÁNT]   Validando tablas, columnas y tipos...
[00:06] [SEMÁNT]   Validación semántica exitosa
[00:07] [OPTIM]    Aplicando reglas de optimización...
[00:08] [OPTIM]    5 sugerencia(s) de optimización generadas
[00:09] [SISTEMA]  Compilación completada — ÉXITO
```

### Tab "Símbolos" (en ResultPanel)
```
Muestra la Tabla de Símbolos en formato árbol:

TABLE  usuarios  8 cols
├── id            INT
├── nombre        VARCHAR
├── apellido      VARCHAR
├── email         VARCHAR
├── edad          INT
├── ciudad        VARCHAR
├── fecha_registro VARCHAR
└── activo        INT

TABLE  productos  7 cols
├── id            INT
...
```

---

## 13. PATRONES DE DISEÑO USADOS

| Patrón | Dónde se aplica | Por qué |
|--------|----------------|---------|
| **N-Tier (Capas)** | Controller → Service → Repository → DB | Separación de responsabilidades. Cada capa tiene un único propósito |
| **MVC** | Controller recibe → Service procesa → JSON responde | Separa la lógica de presentación de la lógica de negocio |
| **Pipeline** | Lexer → Parser → SemanticAnalyzer → Optimizer | Cada fase tiene entrada/salida definida. Fácil de extender con una nueva fase |
| **Repository** | UserRepository, QueryHistoryRepository | Abstrae el acceso a la BD. Si cambias de SQL Server a PostgreSQL, solo cambias la config, no el código |
| **DTO (Data Transfer Object)** | CompilerRequest, CompilerResponse, TokenDto | Evita exponer entidades internas de la BD hacia el frontend |
| **Strategy** | OptimizerService aplica reglas distintas según el tipo de AST | Cada tipo de sentencia tiene su propia estrategia de análisis |
| **JWT Stateless** | JwtAuthFilter + JwtUtil | Sin sesiones en el servidor. El estado está en el token del cliente |
| **Observer / Context** | AuthContext en React | El estado de autenticación notifica automáticamente a todos los componentes que lo consumen |

---

## 14. FLUJO DE DATOS DE EXTREMO A EXTREMO

Este es el camino completo que recorre una consulta SQL desde que el usuario la escribe
hasta que ve los resultados:

```
1. USUARIO escribe en Monaco Editor (SqlEditor.jsx)
   SQL: "SELECT nombre FROM usuarios WHERE id = 1"

2. CLICK en "Analizar Consulta" (o Ctrl+Enter)
   Editor.jsx → handleCompile()

3. AXIOS hace la petición HTTP
   POST http://localhost:3000/api/compile
   Header: Authorization: Bearer eyJ0eXAi...
   Body: { query: "SELECT...", database: "mysql" }

4. VITE PROXY redirige
   localhost:3000/api → localhost:8080/api

5. SPRING BOOT recibe la petición
   JwtAuthFilter → valida token → extrae email → carga User de SQL Server

6. CompilerController.compile(request, user)
   Delega a CompilerService.compile(request, user)

7. FASE 1 — Lexer.tokenize("SELECT nombre FROM usuarios WHERE id = 1")
   Devuelve: [SELECT, nombre, FROM, usuarios, WHERE, id, =, 1, EOF]

8. FASE 2 — Parser.parse(tokens)
   Devuelve: SelectNode {
     columns: ["nombre"],
     tableName: "usuarios",
     whereCondition: ConditionNode(id = 1)
   }

9. FASE 3 — SemanticAnalyzer.analyze(SelectNode)
   SymbolTable.findTable("usuarios") → encontrada
   SymbolTable.findColumn("nombre") en usuarios → encontrada (VARCHAR)
   SymbolTable.findColumn("id") en usuarios → encontrada (INT)
   resolveType(ExpressionNode("1")) → INT
   areCompatible(INT, INT) → true
   Resultado: errors=[], warnings=[]

10. FASE 4 — OptimizerService.analyze(ast, "mysql", table, userProfile)
    UserProfileService.buildProfile(user) → { selectStarCount: 0, isNew: true }
    Regla 2: whereCondition != null → no aplica
    Regla 3: limit == null && whereCondition != null → no aplica LIMIT
    Regla 5: joins.isEmpty() → no aplica
    Regla dialecto: database = "mysql" → agrega tip de EXPLAIN
    Resultado: suggestions=[{ title: "Consejo MySQL", ... }]

11. GUARDAR EN SQL SERVER
    QueryHistory { userId, queryPreview, statementType: "SELECT",
                   databaseType: "mysql", isValid: true,
                   errorCount: 0, suggestionsCount: 1 }
    historyRepo.save(history)

12. RESPUESTA JSON al frontend
    {
      valid: true,
      tokens: [{type:"SELECT", value:"SELECT", line:1, column:1}, ...],
      ast: "SELECT\n  Columns: nombre\n  FROM: usuarios\n  WHERE:\n    ...",
      statementType: "SELECT",
      errors: [],
      warnings: [],
      suggestions: [{ type:"dialect", title:"Consejo MySQL", message:"...", example:"EXPLAIN..." }],
      dialectNote: "MySQL 8.0 — Motor InnoDB, ACID, índices B-Tree."
    }

13. REACT actualiza el estado
    setResult(data) → React re-renderiza:
      PipelineVisual: Fase1=ok(8 tokens) / Fase2=ok(SELECT) / Fase3=ok / Fase4=ok(1 sug.)
      ResultPanel: badge verde "Consulta válida"
      Tab Sugerencias: muestra el tip de MySQL con badge 1
      Tab Log: muestra la traza de compilación
      Tab Símbolos: muestra las 7 tablas del schema
```

---

## 15. PRUEBAS DEL SISTEMA

### Pruebas unitarias Java (mvn test)
```
backend/src/test/
├── LexerTest.java           35 pruebas — tokenización de SQL
├── ParserTest.java          40 pruebas — construcción de AST
├── SemanticAnalyzerTest.java 38 pruebas — validación semántica
├── CompilerPipelineTest.java 25 pruebas — pipeline end-to-end
└── PipelineDataTest.java    40 pruebas — campos para componentes UI

Total: ~178 pruebas unitarias
```

### Pruebas de integración (test-nuevas-funciones.ps1)
```
Requiere backend corriendo en localhost:8080.
Ejecutar: .\test-nuevas-funciones.ps1

9 bloques de prueba (~60 assertions):
  Bloque 1: Campos del response (valid, tokens, ast, statementType, etc.)
  Bloque 2: Fase 1 — tokens generados con línea y columna
  Bloque 3: Fase 2 — statementType correcto para cada tipo de sentencia
  Bloque 4: Fase 3 — estados ok/error/warn del SemanticAnalyzer
  Bloque 5: Fase 4 — sugerencias generadas
  Bloque 6: Datos para el Log de Compilación
  Bloque 7: Endpoint /schema para la Tabla de Símbolos
  Bloque 8: Consultas avanzadas (CTE, Window Functions, CASE WHEN, etc.)
  Bloque 9: Notas de dialecto para los 4 motores
```

### Pruebas manuales verificadas (17/17)
```
✅ Health check del servidor
✅ Registro de nuevo usuario
✅ Login correcto
✅ SELECT básico válido
✅ SELECT * con sugerencias IA personalizadas
✅ UPDATE sin WHERE → peligro detectado
✅ Subquery en WHERE → sugiere JOIN
✅ CTE (WITH) → advertencia re-ejecución
✅ Window Function sin PARTITION BY → detectada
✅ Dashboard /my-stats funciona
✅ Email duplicado → error correcto
✅ Login incorrecto → bloqueado (rate limiting)
✅ UNION → sugiere UNION ALL
✅ CREATE TABLE IF NOT EXISTS → válido
✅ CASE WHEN sin ELSE → válido + sugerencia
✅ DELETE sin WHERE → error crítico
✅ Sin JWT → 403 Forbidden
```

---

## CREDENCIALES DE ACCESO

```
Administrador (profesor):
  Email:    profesor@dataquery.com
  Password: (configurado en el servidor)
  Rol:      PROFESSOR — accede a /dashboard/global

Usuario de prueba:
  Email:    test@prueba.com
  Password: (configurado en el servidor)
  Rol:      STUDENT

Registro abierto:
  Cualquier usuario puede registrarse desde /register
  Requisitos de contraseña: 8+ chars, 1 mayúscula, 1 número
```

## ARRANCAR EL PROYECTO

```bash
# Terminal 1 — Backend (puerto 8080)
cd backend
mvn spring-boot:run
# Esperar: "Started DataQueryApplication in X.XXX seconds"

# Terminal 2 — Frontend (puerto 3000)
cd frontend
npm run dev
# Abrir: http://localhost:3000

# Apagar todo
taskkill /F /IM java.exe
taskkill /F /IM node.exe
```

---

*DataQuery SQL Compiler — Proyecto Final Compiladores UMG 2026*
