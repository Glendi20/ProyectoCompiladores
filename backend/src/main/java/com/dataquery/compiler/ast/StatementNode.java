package com.dataquery.compiler.ast;

/**
 * Clase base abstracta para todos los tipos de sentencia SQL.
 * Cada tipo (SELECT, INSERT, UPDATE, etc.) extiende esta clase.
 */
public abstract class StatementNode extends ASTNode {
    public abstract String getStatementType();
}
