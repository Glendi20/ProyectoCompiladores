package com.dataquery.compiler.lexer;

public class Token {

    public final TokenType type;
    public final String value;
    public final int line;
    public final int column;

    public Token(TokenType type, String value, int line, int column) {
        this.type = type;
        this.value = value;
        this.line = line;
        this.column = column;
    }

    public String typeToString() {
        return type.name();
    }

    @Override
    public String toString() {
        return "[" + typeToString() + "] '" + value + "' (L" + line + ":C" + column + ")";
    }
}
