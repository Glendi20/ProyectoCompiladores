# DataQuery SQL Compiler
## Estructura Completa del Sistema, Funcionamiento y Vista de Usuario

**Proyecto Final · Compiladores · UMG 2026 · Erick Ramazzini**

---

# PARTE 1 — ¿QUÉ ES EL SISTEMA Y PARA QUÉ SIRVE?

DataQuery es una **plataforma web de compilación SQL**. El usuario escribe una consulta SQL,
la plataforma la analiza en tiempo real y devuelve:

- Si la consulta es correcta o tiene errores
- Exactamente dónde está el error y por qué
- Sugerencias de optimización personalizadas
- Una visualización paso a paso del proceso de compilación

La diferencia con ejecutar SQL directamente en una base de datos es que DataQuery
**analiza la consulta como código**, sin necesidad de conectarse a ninguna BD real.
Es como un compilador de C++ que detecta errores antes de ejecutar el programa.

---

# PARTE 2 — ESTRUCTURA GENERAL DEL SISTEMA

El sistema tiene **3 capas** que se comunican entre sí:

```
╔══════════════════════════════════════════════════════════════╗
║                   CAPA 1: FRONTEND                           ║
║                   (Lo que ve el usuario)                     ║
║                                                              ║
║   Tecnología: React + Vite   Puerto: localhost:3000          ║
║                                                              ║
║   ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐      ║
║   │  Login   │ │ Editor   │ │Dashboard │ │ Admin    │      ║
║   │ Register │ │   SQL    │ │Estudiante│ │ Vista    │      ║
║   └──────────┘ └──────────┘ └──────────┘ └──────────┘      ║
╚══════════════════════════════════════════════════════════════╝
                          │
                          │ HTTP / JSON + JWT
                          │ (cada petición lleva el token del usuario)
                          ▼
╔══════════════════════════════════════════════════════════════╗
║                   CAPA 2: BACKEND                            ║
║              (Lógica, compilador y seguridad)                ║
║                                                              ║
║   Tecnología: Java 22 + Spring Boot   Puerto: localhost:8080 ║
║                                                              ║
║   ┌─────────────────────────────────────────────────────┐   ║
║   │               COMPILADOR SQL (4 fases)              │   ║
║   │  [Léxico] → [Sintáctico] → [Semántico] → [Optimizad]│   ║
║   └─────────────────────────────────────────────────────┘   ║
║                                                              ║
║   ┌──────────────┐  ┌──────────────┐  ┌─────────────────┐  ║
║   │ Autenticación│  │  Dashboard   │  │  IA Tipo 3      │  ║
║   │ JWT + BCrypt │  │  Métricas    │  │  Sugerencias    │  ║
║   └──────────────┘  └──────────────┘  └─────────────────┘  ║
╚══════════════════════════════════════════════════════════════╝
                          │
                          │ JDBC / JPA
                          ▼
╔══════════════════════════════════════════════════════════════╗
║                   CAPA 3: BASE DE DATOS                      ║
║                   (Persistencia real)                        ║
║                                                              ║
║   Tecnología: SQL Server 2022   Servidor: servidor-bd           ║
║   BD: nombre_base_de_datos                                           ║
║                                                              ║
║   ┌────────────┐ ┌─────────────────┐ ┌──────────────────┐  ║
║   │   users    │ │  query_history  │ │ error_frequency  │  ║
║   │ (cuentas)  │ │ (historial SQL) │ │  (IA Tipo 3)     │  ║
║   └────────────┘ └─────────────────┘ └──────────────────┘  ║
╚══════════════════════════════════════════════════════════════╝
```

---

# PARTE 3 — ROL DE CADA COMPONENTE

## 3.1 Componentes del Frontend

```
┌─────────────────────────────────────────────────────────────────┐
│  COMPONENTE          │  ROL / RESPONSABILIDAD                    │
├─────────────────────────────────────────────────────────────────┤
│  App.jsx             │  Define todas las rutas de navegación     │
│                      │  /login, /register, /editor, /dashboard   │
├─────────────────────────────────────────────────────────────────┤
│  AuthContext.jsx     │  Guarda el estado de sesión global.       │
│                      │  Sabe si hay usuario logueado y su rol    │
├─────────────────────────────────────────────────────────────────┤
│  api.js (Axios)      │  Maneja todas las peticiones al backend.  │
│                      │  Agrega automáticamente el JWT a cada una │
├─────────────────────────────────────────────────────────────────┤
│  Login.jsx           │  Formulario de inicio de sesión           │
│  Register.jsx        │  Formulario de registro de cuenta nueva   │
├─────────────────────────────────────────────────────────────────┤
│  Editor.jsx          │  Pantalla principal — une todos los       │
│                      │  paneles del compilador                   │
├─────────────────────────────────────────────────────────────────┤
│  SqlEditor.jsx       │  Editor Monaco (igual a VS Code).         │
│                      │  Resaltado SQL, Ctrl+Enter para analizar  │
├─────────────────────────────────────────────────────────────────┤
│  PipelineVisual.jsx  │  Barra con las 4 fases del compilador.    │
│                      │  Cada fase cambia de color según resultado│
├─────────────────────────────────────────────────────────────────┤
│  ResultPanel.jsx     │  Panel derecho con 6 tabs:                │
│                      │  Resultado, Tokens, AST, Sugerencias,     │
│                      │  Log, Símbolos                            │
├─────────────────────────────────────────────────────────────────┤
│  SchemaPanel.jsx     │  Muestra las tablas y columnas del schema │
│                      │  con tipos en colores (INT=azul,VARCHAR=verde)│
├─────────────────────────────────────────────────────────────────┤
│  DatabaseSelector    │  Botones MySQL / PostgreSQL /             │
│                      │  SQL Server / MongoDB                     │
├─────────────────────────────────────────────────────────────────┤
│  Dashboard.jsx       │  Métricas personales del estudiante:      │
│                      │  gráficas, historial, estadísticas        │
├─────────────────────────────────────────────────────────────────┤
│  ProfessorDashboard  │  Vista global para el administrador:      │
│                      │  estadísticas de todos los usuarios       │
├─────────────────────────────────────────────────────────────────┤
│  Navbar.jsx          │  Barra superior con navegación y datos    │
│                      │  del usuario logueado                     │
│  PrivateRoute.jsx    │  Bloquea páginas si no hay sesión activa  │
└─────────────────────────────────────────────────────────────────┘
```

## 3.2 Componentes del Backend

```
┌─────────────────────────────────────────────────────────────────┐
│  COMPONENTE              │  ROL / RESPONSABILIDAD                │
├─────────────────────────────────────────────────────────────────┤
│  CompilerController      │  Recibe POST /api/compile.            │
│                          │  Punto de entrada al compilador       │
├─────────────────────────────────────────────────────────────────┤
│  AuthController          │  Maneja /api/auth/register y /login  │
├─────────────────────────────────────────────────────────────────┤
│  DashboardController     │  Entrega estadísticas del historial   │
│  SchemaController        │  Entrega la lista de tablas y columnas│
├─────────────────────────────────────────────────────────────────┤
│  CompilerService         │  Orquesta las 4 fases del compilador. │
│                          │  Llama Lexer → Parser → Semantic →   │
│                          │  Optimizer y guarda en SQL Server     │
├─────────────────────────────────────────────────────────────────┤
│  Lexer                   │  FASE 1: divide el SQL en tokens      │
│  Parser                  │  FASE 2: construye el árbol sintáctico│
│  SemanticAnalyzer        │  FASE 3: valida tablas y columnas     │
│  OptimizerService        │  FASE 4: genera sugerencias de mejora │
├─────────────────────────────────────────────────────────────────┤
│  SymbolTable             │  El "diccionario" del compilador.     │
│                          │  Contiene las 7 tablas del schema     │
├─────────────────────────────────────────────────────────────────┤
│  JwtUtil                 │  Genera y valida tokens JWT           │
│  JwtAuthFilter           │  Intercepta CADA petición y verifica  │
│                          │  que el token sea válido              │
│  SecurityConfig          │  Define qué rutas necesitan login     │
├─────────────────────────────────────────────────────────────────┤
│  UserProfileService      │  Consulta el historial del usuario    │
│                          │  para personalizar las sugerencias    │
├─────────────────────────────────────────────────────────────────┤
│  DataInitializer         │  Crea la cuenta del profesor al       │
│                          │  arrancar si no existe                │
└─────────────────────────────────────────────────────────────────┘
```

## 3.3 Tablas de la Base de Datos

```
┌─────────────────────────────────────────────────────────────────┐
│  TABLA               │  QUÉ GUARDA                              │
├─────────────────────────────────────────────────────────────────┤
│  users               │  Cuentas de usuario: nombre, email,      │
│                      │  contraseña hasheada, rol, intentos      │
│                      │  fallidos de login, bloqueo              │
├─────────────────────────────────────────────────────────────────┤
│  query_history       │  Cada consulta analizada: el SQL,        │
│                      │  si fue válida, cuántos errores tuvo,    │
│                      │  qué motor usó, cuándo fue               │
├─────────────────────────────────────────────────────────────────┤
│  error_frequency     │  Qué errores ocurren más seguido en      │
│                      │  toda la plataforma. Alimenta la IA      │
└─────────────────────────────────────────────────────────────────┘
```

---

# PARTE 4 — CÓMO FUNCIONA EL COMPILADOR SQL

El compilador tiene **4 fases en secuencia**. Si una fase falla, las siguientes no se ejecutan.

```
  TEXTO SQL ingresado
         │
         ▼
  ╔═════════════╗
  ║   FASE 1    ║  ¿Qué hace?  Divide el texto en "piezas" llamadas tokens
  ║   LÉXICO    ║  ¿Falla si?  El SQL tiene caracteres inválidos
  ║             ║  Produce:    Lista de tokens con tipo, valor, línea y columna
  ╚═════════════╝
         │
         ▼  List<Token>
  ╔═════════════╗
  ║   FASE 2    ║  ¿Qué hace?  Organiza los tokens en una estructura de árbol (AST)
  ║  SINTÁCTICO ║  ¿Falla si?  La gramática es incorrecta (falta FROM, paréntesis, etc.)
  ║             ║  Produce:    Árbol Sintáctico Abstracto (AST)
  ╚═════════════╝
         │
         ▼  AST (StatementNode)
  ╔═════════════╗
  ║   FASE 3    ║  ¿Qué hace?  Valida que tablas y columnas existan, tipos compatibles
  ║  SEMÁNTICO  ║  ¿Falla si?  Columna inventada, UPDATE sin WHERE, tipos incompatibles
  ║             ║  Produce:    Lista de errores y advertencias
  ╚═════════════╝
         │
         ▼  errors[], warnings[]
  ╔═════════════╗
  ║   FASE 4    ║  ¿Qué hace?  Analiza el AST y genera consejos de mejora
  ║ OPTIMIZADOR ║  ¿Falla si?  Nunca falla — siempre genera al menos 1 sugerencia
  ║    IA T3    ║  Produce:    Lista de sugerencias personalizadas
  ╚═════════════╝
         │
         ▼
  RESULTADO FINAL (JSON)
```

---

## 4.1 Fase 1 — Análisis Léxico en detalle

**¿Qué es un token?**
Es la unidad mínima de significado en SQL. El Lexer recorre el texto letra por letra
y agrupa caracteres en tokens.

```
SQL de entrada:
  SELECT nombre FROM usuarios WHERE id = 1

Tokens que produce el Lexer:
  ┌──────────────┬────────────┬───────┬────────┐
  │    TIPO      │   VALOR    │ LÍNEA │ COLUMNA│
  ├──────────────┼────────────┼───────┼────────┤
  │ SELECT       │ "SELECT"   │   1   │   1    │
  │ IDENTIFIER   │ "nombre"   │   1   │   8    │
  │ FROM         │ "FROM"     │   1   │  15    │
  │ IDENTIFIER   │ "usuarios" │   1   │  20    │
  │ WHERE        │ "WHERE"    │   1   │  29    │
  │ IDENTIFIER   │ "id"       │   1   │  35    │
  │ EQUAL        │ "="        │   1   │  38    │
  │ NUMBER       │ "1"        │   1   │  40    │
  │ END_OF_FILE  │ ""         │   1   │  41    │
  └──────────────┴────────────┴───────┴────────┘
```

**Tipos de tokens que reconoce:**
```
PALABRAS CLAVE:   SELECT, FROM, WHERE, INSERT, UPDATE, DELETE, CREATE,
                  ALTER, DROP, JOIN, GROUP BY, HAVING, ORDER BY, LIMIT,
                  WITH, CASE WHEN, OVER, PARTITION, UNION, DISTINCT...

TIPOS DE DATOS:   INT, VARCHAR, FLOAT, BOOLEAN, DATE, TEXT, DECIMAL,
                  BIGINT, DATETIME, NVARCHAR, MONEY, TINYINT...

OPERADORES:       = > < >= <= != <> AND OR NOT IN LIKE BETWEEN IS
                  + - * / %

LITERALES:        "texto entre comillas"  → tipo STRING
                  123  o  3.14           → tipo NUMBER
                  TRUE / FALSE           → tipo BOOLEAN_LITERAL

DELIMITADORES:    ( )  ,  ;  .  [ ]
```

---

## 4.2 Fase 2 — Análisis Sintáctico en detalle

**¿Qué es el AST?**
El Árbol Sintáctico Abstracto (AST) es la representación estructurada de la consulta.
En vez de texto, es un objeto Java con propiedades organizadas.

```
SQL:
  SELECT nombre, COUNT(*) AS total
  FROM usuarios
  WHERE activo = 1
  GROUP BY nombre
  ORDER BY total DESC

AST generado (SelectNode):
  SelectNode
  ├── selectAll: false
  ├── columns: ["nombre", "COUNT(*) AS total"]
  ├── tableName: "usuarios"
  ├── tableAlias: null
  ├── joins: [] (vacío, no hay JOINs)
  ├── whereCondition:
  │     ConditionNode
  │     ├── left: ExpressionNode(tipo=COLUMN, valor="activo")
  │     ├── op: EQUAL
  │     └── right: ExpressionNode(tipo=NUMBER, valor="1")
  ├── groupByColumns: ["nombre"]
  ├── havingCondition: null
  └── orderBy:
        OrderByClause(columna="total", ascending=false)
```

**Tipos de AST que puede generar:**
```
SelectNode      → para SELECT
InsertNode      → para INSERT INTO
UpdateNode      → para UPDATE
DeleteNode      → para DELETE FROM
CreateTableNode → para CREATE TABLE
AlterTableNode  → para ALTER TABLE
DropNode        → para DROP TABLE / DROP DATABASE
```

**Errores que detecta la Fase 2 (sintácticos):**
```
❌ SELECT nombre usuarios WHERE id = 1
   Error: Se esperaba FROM después de las columnas

❌ INSERT INTO usuarios nombre VALUES ('Juan')
   Error: Se esperaba paréntesis ( antes de los nombres de columna

❌ SELECT * FROM usuarios WHERE
   Error: Se esperaba una condición después de WHERE
```

---

## 4.3 Fase 3 — Análisis Semántico en detalle

**¿Qué valida?**
Mientras la Fase 2 verifica la gramática (estructura), la Fase 3 verifica el significado
usando la Tabla de Símbolos (las 7 tablas del schema).

**Reglas de validación:**

```
VALIDACIÓN DE TABLAS:
  ✅  SELECT * FROM usuarios
      → usuarios existe en la Tabla de Símbolos

  ⚠️  SELECT * FROM clientes
      → clientes NO existe → Warning (puede ser tabla real del usuario)
      → No genera ERROR porque podría ser una tabla real en su BD

VALIDACIÓN DE COLUMNAS:
  ✅  SELECT nombre, email FROM usuarios
      → nombre y email existen en la tabla usuarios

  ❌  SELECT telefono FROM usuarios
      → telefono NO existe en usuarios → ERROR

VALIDACIÓN DE TIPOS:
  ✅  SELECT * FROM usuarios WHERE id = 1
      → id es INT, 1 es NUMBER → compatibles

  ❌  SELECT * FROM usuarios WHERE nombre = 99
      → nombre es VARCHAR, 99 es NUMBER → INCOMPATIBLES → ERROR

DETECCIÓN DE PELIGROS CRÍTICOS:
  ❌  UPDATE empleados SET salario = 0
      → No tiene WHERE → afectaría TODOS los registros
      → ERROR: "PELIGRO CRÍTICO: UPDATE sin WHERE"

  ❌  DELETE FROM pedidos
      → No tiene WHERE → eliminaría TODOS los registros
      → ERROR: "PELIGRO CRÍTICO: DELETE sin WHERE"

  ❌  DROP DATABASE produccion
      → ERROR: "EXTREMADAMENTE PELIGROSO"

ADVERTENCIAS (no bloquean, son consejos):
  ⚠️  CREATE TABLE nueva (nombre VARCHAR(100))
      → Sin PRIMARY KEY → Warning

  ⚠️  DROP TABLE usuarios
      → Operación irreversible → Warning de backup

CASOS ESPECIALES (tolerados):
  ✅  WITH ventas AS (SELECT * FROM pedidos) SELECT * FROM ventas
      → "ventas" es una CTE, no se marca como tabla desconocida

  ✅  SELECT CASE WHEN activo = 1 THEN 'activo' END FROM usuarios
      → Expresiones complejas (CASE, Window Functions) se saltan sin error

  ✅  SELECT COUNT(*) AS total FROM usuarios ORDER BY total
      → "total" es un alias definido en SELECT, no se busca como columna
```

---

## 4.4 Fase 4 — Optimizador / IA Tipo 3 en detalle

**¿Qué es IA Tipo 3?**
Es un sistema de reglas inteligentes que se personaliza según el historial real del usuario
guardado en SQL Server. Cuantas más consultas hayas hecho, más personalizado es el mensaje.

**Las 12 reglas:**

```
REGLA 1 — SELECT *
  Detecta: SELECT * FROM tabla
  Mensaje estándar: "Evita SELECT * — especifica solo las columnas que necesitas"
  Mensaje si ya usaste SELECT * 2+ veces: "Ya usaste SELECT * N veces — es hora de corregirlo"
  Mensaje si ya usaste SELECT * 5+ veces: "¡Llevas N veces usando SELECT *! Ya es hora de romper este hábito"

REGLA 2 — Sin WHERE en SELECT
  Detecta: SELECT ... FROM tabla (sin WHERE)
  Mensaje: "Sin WHERE la consulta escanea TODA la tabla"

REGLA 3 — Sin LIMIT
  Detecta: SELECT sin LIMIT y sin WHERE
  Mensaje: "Pagina tus resultados con LIMIT para no retornar miles de filas"

REGLA 4 — DISTINCT sospechoso
  Detecta: SELECT DISTINCT
  Mensaje: "DISTINCT puede ser síntoma de un JOIN mal hecho"

REGLA 5 — JOINs sin índice
  Detecta: cualquier JOIN
  Mensaje: "Las columnas en ON deben tener índice para evitar Full Table Scan"

REGLA 6 — GROUP BY sin agregación
  Detecta: GROUP BY sin COUNT/SUM/AVG/MAX/MIN
  Mensaje: "GROUP BY necesita una función de agregación"

REGLA 7 — ORDER BY sin LIMIT
  Detecta: ORDER BY sin LIMIT
  Mensaje: "Ordenar toda la tabla sin limitar es muy costoso"

REGLA 8 — Subquery en WHERE
  Detecta: WHERE columna IN (SELECT ...)
  Mensaje: "Reescribe la subquery como JOIN — puede ser 10-100x más rápido"

REGLA 9 — Window Function sin PARTITION BY
  Detecta: ROW_NUMBER() OVER (ORDER BY ...) sin PARTITION BY
  Mensaje: "Sin PARTITION BY la función procesa todas las filas juntas"

REGLA 10 — CASE WHEN sin ELSE
  Detecta: CASE WHEN ... END sin ELSE
  Mensaje: "Sin ELSE el CASE devuelve NULL silenciosamente"

REGLA 11 — UNION lento
  Detecta: UNION (sin ALL)
  Mensaje: "UNION ALL es más rápido si no necesitas eliminar duplicados"

REGLA 12 — CTE y re-ejecución
  Detecta: WITH cte AS (...)
  Mensaje: "Cuidado: el CTE puede ejecutarse múltiples veces si lo referencias varias veces"

+ DIALECTO — Consejo específico del motor elegido (MySQL/PostgreSQL/SQL Server/MongoDB)
+ DANGER   — Para UPDATE/DELETE sin WHERE (prioridad máxima)
+ PERSONAL — Basado en el historial personal del usuario
```

---

# PARTE 5 — VISTA DEL USUARIO (PASO A PASO)

## 5.1 Pantalla de Registro

```
┌─────────────────────────────────────────────┐
│               DataQuery                     │
│           Crea tu cuenta gratuita           │
│                                             │
│  Nombre:    [________________]              │
│  Apellido:  [________________]              │
│  Email:     [________________]              │
│  Contraseña:[________________]              │
│             (mín. 8 chars, 1 mayúscula,     │
│              1 número)                      │
│                                             │
│         [ Crear cuenta ]                   │
│                                             │
│  ¿Ya tienes cuenta? Inicia sesión           │
└─────────────────────────────────────────────┘

Lo que pasa al crear cuenta:
  1. El frontend valida: longitud, mayúscula, número
  2. POST /api/auth/register → backend
  3. Backend verifica que el email no exista ya
  4. BCrypt hashea la contraseña (irreversible)
  5. Guarda el usuario en SQL Server
  6. Genera JWT y lo devuelve
  7. Frontend guarda el JWT en localStorage
  8. Redirige directamente al Editor SQL
```

## 5.2 Pantalla del Editor SQL (pantalla principal)

```
┌────────────────────────────────────────────────────────────────────────────────┐
│  ⚡ DataQuery          [ Editor SQL ]  [ Dashboard ]      Erick  Estudiante Salir│
├──────────────┬──────────────────────────────────────────┬──────────────────────┤
│              │                                          │                      │
│  BASE DE     │   PIPELINE                               │  ✅ Consulta válida  │
│  DATOS       │   ┌──────────┐ ┌──────────┐             │                      │
│              │   │1 🔤      │→│2 🌳      │→            │  ℹ️ MySQL 8.0        │
│  ● MySQL     │   │ Análisis │ │ Análisis │             │                      │
│  ○ PostgreSQL│   │ Léxico   │ │Sintáctico│             │  [Resultado][Tokens] │
│  ○ SQL Server│   │ 42 tokens│ │ SELECT   │             │  [AST][Sugerencias]  │
│  ○ MongoDB   │   └──────────┘ └──────────┘             │  [Log][Símbolos]     │
│              │   ┌──────────┐ ┌──────────┐             │                      │
│  SCHEMA DE   │   │3 🔍      │→│4 ⚡      │             │  Tab activo:         │
│  BD          │   │ Análisis │ │Optimiza- │             │  Sugerencias (3)     │
│              │   │Semántico │ │ción IA   │             │                      │
│  ▼ usuarios  │   │sin errores│ │3 suger. │             │  ⚡ Evita SELECT *   │
│  ├ id INT    │   └──────────┘ └──────────┘             │  📚 Agrega WHERE    │
│  ├ nombre VAR│                                          │  🔧 Consejo MySQL   │
│  ├ email VAR │   EDITOR SQL              Ctrl+Enter     │                      │
│  └ activo INT│   ┌────────────────────────────────────┐│                      │
│              │   │ 1  SELECT id, nombre, email         ││                      │
│  ▼ productos │   │ 2  FROM usuarios                    ││                      │
│  ├ id INT    │   │ 3  WHERE activo = 1                 ││                      │
│  ├ nombre VAR│   │                                     ││                      │
│  └ precio FL │   └────────────────────────────────────┘│                      │
│              │                                          │                      │
│              │      [ ▶ Analizar Consulta ]            │                      │
└──────────────┴──────────────────────────────────────────┴──────────────────────┘
```

**Los 3 paneles:**

```
PANEL IZQUIERDO (260px) — Configuración
├── Selector de motor: MySQL / PostgreSQL / SQL Server / MongoDB
│   Cambia el motor para obtener sugerencias específicas de cada BD
└── Schema de BD: muestra las 7 tablas con sus columnas y tipos
    Ayuda al usuario a saber qué columnas puede usar en sus consultas

PANEL CENTRAL — El compilador
├── PipelineVisual: 4 fases con estado visual
│   • Gris    = sin consulta analizada
│   • Azul    = analizando (cargando)
│   • Verde   = fase completada sin errores
│   • Amarillo = completada con advertencias
│   • Rojo    = completada con errores
└── Monaco Editor: editor de código profesional
    • Resaltado de sintaxis SQL en colores
    • Numeración de líneas
    • Ctrl+Enter para analizar sin usar el mouse

PANEL DERECHO (380px) — Resultados (6 tabs)
├── Resultado:    Errores y advertencias encontrados
├── Tokens:       Todos los tokens generados por el Lexer
├── AST:          El árbol sintáctico en formato texto
├── Sugerencias:  Consejos de optimización con ejemplos
├── Log:          Traza completa del proceso de compilación
└── Símbolos:     Tabla de Símbolos con todas las tablas y columnas
```

## 5.3 Los 6 Tabs del Panel de Resultados

### Tab 1 — Resultado
```
Muestra el resumen del análisis:

Si la consulta es VÁLIDA:
  ┌─────────────────────────────────┐
  │  ✅ Consulta válida             │
  │                                 │
  │  La consulta pasó el análisis   │
  │  léxico, sintáctico y semántico │
  │  correctamente.                 │
  └─────────────────────────────────┘

Si tiene ERRORES:
  ┌─────────────────────────────────┐
  │  ❌ Consulta con errores        │
  │                                 │
  │  Errores:                       │
  │  ⛔ La columna 'telefono' no    │
  │     existe en la tabla 'usuario'│
  │                                 │
  │  Advertencias:                  │
  │  ⚠️  La tabla 'ventas' no está  │
  │     en el schema de ejemplo     │
  └─────────────────────────────────┘
```

### Tab 2 — Tokens
```
Muestra todos los tokens generados por el Lexer.
Útil para entender cómo el compilador lee el SQL.

  12 token(s) encontrados:

  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
  │ SELECT      │ │ IDENTIFIER  │ │ FROM        │
  │ 'SELECT'    │ │ 'nombre'    │ │ 'FROM'      │
  │ L1:C1       │ │ L1:C8       │ │ L1:C15      │
  └─────────────┘ └─────────────┘ └─────────────┘
```

### Tab 3 — AST
```
Muestra el Árbol Sintáctico Abstracto en formato texto.
Es la estructura interna de la consulta.

  SELECT
    Columns: nombre, email
    FROM: usuarios
    WHERE:
      nombre = 'Juan'
    ORDER BY: nombre ASC
```

### Tab 4 — Sugerencias
```
Muestra los consejos de optimización con ejemplos de código.
Cada sugerencia tiene un tipo y color diferente:

  ⚡ optimization  — mejora de rendimiento  (naranja)
  📚 best-practice — buena práctica SQL     (azul)
  🔧 dialect       — específico del motor   (verde)
  ⛔ danger        — operación peligrosa    (rojo)
  🎯 personal      — basado en tu historial (morado)

Ejemplo de tarjeta de sugerencia:
  ┌──────────────────────────────────────────────┐
  │ ⚡  Evita SELECT *              optimization  │
  │                                              │
  │ Seleccionar todas las columnas con *         │
  │ transfiere datos innecesarios por la red.    │
  │ Especifica solo las columnas que necesitas.  │
  │                                              │
  │ Ejemplo:                                     │
  │ SELECT id, nombre, email FROM usuarios       │
  └──────────────────────────────────────────────┘
```

### Tab 5 — Log de Compilación
```
Muestra la traza completa del proceso, estilo terminal.

  [00:01] [LÉXICO]   Iniciando análisis léxico...
  [00:02] [LÉXICO]   42 tokens generados correctamente
  [00:03] [SINTÁCT]  Construyendo árbol sintáctico (AST)...
  [00:04] [SINTÁCT]  AST generado — tipo: SELECT
  [00:05] [SEMÁNT]   Validando tablas, columnas y tipos...
  [00:06] [SEMÁNT]   Validación semántica exitosa
  [00:07] [OPTIM]    Aplicando reglas de optimización...
  [00:08] [OPTIM]    3 sugerencia(s) generadas
  [00:09] [SISTEMA]  Compilación completada — ÉXITO
```

### Tab 6 — Tabla de Símbolos
```
Muestra las 7 tablas del schema en formato árbol.

  TABLE  usuarios  8 cols
  ├── id              INT
  ├── nombre          VARCHAR
  ├── apellido        VARCHAR
  ├── email           VARCHAR
  ├── edad            INT
  ├── ciudad          VARCHAR
  ├── fecha_registro  VARCHAR
  └── activo          INT

  TABLE  productos  7 cols
  ├── id              INT
  ├── nombre          VARCHAR
  ...
```

## 5.4 Pantalla del Dashboard (Estudiante)

```
┌──────────────────────────────────────────────────────────────────┐
│  ⚡ DataQuery       [ Editor SQL ]  [ Dashboard ]    Erick Salir  │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Mi Dashboard — Estadísticas de mis consultas                    │
│                                                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │  Total   │  │ Exitosas │  │Con Error │  │  Tasa    │        │
│  │    47    │  │    35    │  │    12    │  │   74%    │        │
│  │consultas │  │          │  │          │  │ de éxito │        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
│                                                                  │
│  Por Tipo de Sentencia         Por Motor de BD                   │
│  ┌─────────────────────┐       ┌─────────────────────┐          │
│  │ SELECT ████████ 28  │       │ MySQL    ████████ 30 │          │
│  │ INSERT ████ 10      │       │ PostgreSQL ████ 12   │          │
│  │ UPDATE ██ 5         │       │ SQL Server ██ 5      │          │
│  │ DELETE ██ 4         │       │ MongoDB    █ 2       │          │
│  └─────────────────────┘       └─────────────────────┘          │
│                                                                  │
│  Últimas consultas:                                              │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ ✅ SELECT nombre, email FROM usuarios WHERE...  MySQL   │    │
│  │    hace 5 minutos                                        │    │
│  │ ❌ SELECT columna_falsa FROM productos          MySQL   │    │
│  │    hace 12 minutos                                       │    │
│  └─────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────┘
```

## 5.5 Vista del Administrador (Profesor)

```
Solo accesible con: profesor@dataquery.com

Muestra estadísticas GLOBALES de TODOS los usuarios:
  - Total de usuarios registrados en la plataforma
  - Total de consultas analizadas en el sistema
  - Errores más frecuentes de toda la plataforma
  - Patrones de uso (qué tipo de SQL se usa más)
```

---

# PARTE 6 — SISTEMA DE SEGURIDAD

## 6.1 Autenticación JWT

```
¿Qué es JWT?
JWT = JSON Web Token. Es un string codificado que contiene
la identidad del usuario y tiene una firma digital.

Estructura:
  eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9    ← encabezado (base64)
  .eyJzdWIiOiJ1c3VhcmlvQGVtYWlsLmNvbSIsImlhdCI6...  ← datos del usuario
  .firma_digital                             ← verificación

¿Cómo funciona?
  1. Usuario hace login → backend genera un JWT firmado
  2. Frontend guarda el JWT en localStorage del navegador
  3. Cada petición al backend incluye el JWT en el header:
     Authorization: Bearer eyJ0eXAi...
  4. Backend verifica la firma → si es válida, procesa la petición
  5. El JWT expira en 24 horas → usuario debe volver a hacer login

¿Por qué es seguro?
  • La firma usa una clave secreta que solo el servidor conoce
  • No se puede falsificar sin conocer esa clave
  • No se guardan sesiones en el servidor — el token es autocontenido
```

## 6.2 Protección de contraseñas (BCrypt)

```
¿Qué es BCrypt?
Es un algoritmo de hash que convierte la contraseña en un string
irreversible. Ni el servidor puede saber cuál era la contraseña original.

Ejemplo:
  Contraseña original:  "MiPass123"
  Hash BCrypt (factor 12): "$2a$12$K8Yh3V.../X7wQpR..."

Al hacer login:
  BCrypt NO descifra el hash.
  En cambio, aplica el mismo proceso a la contraseña ingresada
  y compara el resultado con el hash guardado.
```

## 6.3 Rate Limiting (protección contra ataques)

```
Si alguien intenta adivinar una contraseña:

  Intento 1 → incorrecto → failedAttempts = 1
  Intento 2 → incorrecto → failedAttempts = 2
  ...
  Intento 5 → incorrecto → failedAttempts = 5
               → CUENTA BLOQUEADA POR 15 MINUTOS
               → Error: "Cuenta bloqueada. Intenta en 15 minutos"
```

## 6.4 Rutas protegidas

```
PÚBLICAS (sin login):
  POST /api/auth/register   ← registro
  POST /api/auth/login      ← login

PROTEGIDAS (requieren JWT válido):
  POST /api/compile         ← compilar SQL
  GET  /api/schema          ← tablas del schema
  GET  /api/databases       ← lista de motores
  GET  /api/dashboard/my-stats  ← estadísticas personales

SOLO PROFESOR (requieren rol PROFESSOR):
  GET  /api/dashboard/global/stats  ← estadísticas globales
```

---

# PARTE 7 — FLUJO COMPLETO DE UNA CONSULTA

Desde que el usuario escribe el SQL hasta que ve los resultados:

```
  USUARIO escribe en el editor:
  "SELECT nombre FROM usuarios WHERE id = 1"
               │
               ▼
  CLIC en "Analizar Consulta"
               │
  El frontend envía al backend:
  POST /api/compile
  {
    query: "SELECT nombre FROM usuarios WHERE id = 1",
    database: "mysql"
  }
  + Header: Authorization: Bearer eyJ0eXAi...
               │
               ▼
  BACKEND — Spring Security verifica el JWT
  → Válido: continúa
  → Inválido: devuelve 403 Forbidden
               │
               ▼
  FASE 1 — Lexer divide en tokens:
  [SELECT][nombre][FROM][usuarios][WHERE][id][=][1][EOF]
  → 8 tokens
               │
               ▼
  FASE 2 — Parser construye el AST:
  SelectNode {
    columns: ["nombre"],
    tableName: "usuarios",
    whereCondition: (id = 1)
  }
               │
               ▼
  FASE 3 — SemanticAnalyzer valida:
  → "usuarios" existe en SymbolTable ✅
  → "nombre" existe en usuarios ✅
  → "id" es INT, "1" es NUMBER → compatible ✅
  → errors = [], warnings = []
               │
               ▼
  FASE 4 — OptimizerService analiza:
  → UserProfileService: usuario tiene 12 consultas, selectStarCount = 3
  → Regla 2: tiene WHERE → no aplica "sin WHERE"
  → Regla 3: tiene WHERE → no aplica LIMIT
  → Dialecto MySQL → agrega tip de EXPLAIN
  → suggestions = [{ type: "dialect", title: "Consejo MySQL", ... }]
               │
               ▼
  GUARDAR EN SQL SERVER:
  QueryHistory {
    userId: 5,
    queryPreview: "SELECT nombre FROM usuarios WHERE id = 1",
    statementType: "SELECT",
    databaseType: "mysql",
    isValid: true,
    errorCount: 0,
    suggestionsCount: 1,
    analyzedAt: "2026-05-21 15:30:00"
  }
               │
               ▼
  RESPUESTA JSON al frontend:
  {
    valid: true,
    tokens: [8 tokens],
    ast: "SELECT\n  Columns: nombre\n  FROM: usuarios...",
    statementType: "SELECT",
    errors: [],
    warnings: [],
    suggestions: [{ type:"dialect", title:"Consejo MySQL", ... }],
    dialectNote: "MySQL 8.0 — Motor InnoDB, ACID, índices B-Tree."
  }
               │
               ▼
  REACT actualiza la interfaz:
  → PipelineVisual: 4 fases en VERDE
  → Badge: "✅ Consulta válida"
  → Tab Sugerencias: badge con "1"
  → Tab Log: traza completa visible
  → Tab Símbolos: 7 tablas del schema
```

---

# PARTE 8 — RESUMEN DE FUNCIONALIDADES

```
FUNCIONALIDAD                        ESTADO
─────────────────────────────────────────────────────────
Análisis Léxico (tokenización)       ✅ Implementado
Análisis Sintáctico (AST)            ✅ Implementado
Análisis Semántico (validación)      ✅ Implementado
Optimizador con 12 reglas            ✅ Implementado
IA Tipo 3 (personalización real)     ✅ Implementado
Soporte MySQL                        ✅ Implementado
Soporte PostgreSQL                   ✅ Implementado
Soporte SQL Server (T-SQL)           ✅ Implementado
Soporte MongoDB                      ✅ Implementado
JWT Auth (registro + login)          ✅ Implementado
Rate limiting (5 intentos → 15 min) ✅ Implementado
Dashboard estudiante                 ✅ Implementado
Dashboard administrador              ✅ Implementado
Monaco Editor                        ✅ Implementado
Pipeline Visual (4 fases)            ✅ Implementado
Log de compilación                   ✅ Implementado
Tabla de Símbolos visual             ✅ Implementado
Schema panel                         ✅ Implementado
Historial persistente SQL Server     ✅ Implementado
~178 pruebas automatizadas           ✅ Implementado
─────────────────────────────────────────────────────────

SQL SOPORTADO:
SELECT básico, columnas, alias, DISTINCT            ✅
WHERE con AND / OR / NOT                            ✅
BETWEEN, IN, LIKE, IS NULL, IS NOT NULL             ✅
INNER / LEFT / RIGHT / FULL OUTER / CROSS JOIN      ✅
GROUP BY + HAVING                                   ✅
ORDER BY ASC/DESC + LIMIT + OFFSET                  ✅
TOP N (SQL Server)                                  ✅
FETCH NEXT N ROWS ONLY (SQL Server / PostgreSQL)    ✅
INSERT INTO con múltiples filas                     ✅
UPDATE + SET con WHERE                              ✅
DELETE FROM con WHERE                               ✅
CREATE TABLE con constraints                        ✅
CREATE TABLE IF NOT EXISTS                          ✅
ALTER TABLE (ADD / DROP / MODIFY / RENAME)          ✅
DROP TABLE IF EXISTS                                ✅
DROP DATABASE (detectado como PELIGRO)              ✅
WITH / CTE (Common Table Expressions)               ✅
UNION / UNION ALL / INTERSECT / EXCEPT              ✅
Subqueries en WHERE (IN / EXISTS)                   ✅
CASE WHEN / THEN / ELSE / END                       ✅
Window Functions (ROW_NUMBER, RANK, DENSE_RANK)     ✅
OVER (PARTITION BY ... ORDER BY ...)               ✅
Funciones: COUNT, SUM, AVG, MAX, MIN               ✅
Funciones de fecha: GETDATE, DATEADD, YEAR...      ✅
Funciones de string: SUBSTRING, TRIM, REPLACE      ✅
CAST / CONVERT / TRY_CAST                          ✅
BEGIN TRANSACTION / COMMIT / ROLLBACK              ✅
```

---

*DataQuery SQL Compiler — Proyecto Final Compiladores UMG 2026 — Erick Ramazzini*
