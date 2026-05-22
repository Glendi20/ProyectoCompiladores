package com.dataquery.model;

import java.util.List;

public class CompilerResponse {
    private boolean        valid;
    private String         statementType;   // SELECT, INSERT, UPDATE, DELETE, etc.
    private List<TokenDto> tokens;
    private String         ast;
    private List<String>   errors;
    private List<String>   warnings;
    private List<Suggestion> suggestions;
    private String         dialectNote;
    private String         optimizedQuery;

    public boolean         isValid()           { return valid; }
    public void            setValid(boolean v) { this.valid = v; }

    public String          getStatementType()        { return statementType; }
    public void            setStatementType(String t){ this.statementType = t; }

    public List<TokenDto>  getTokens()              { return tokens; }
    public void            setTokens(List<TokenDto> t){ this.tokens = t; }

    public String          getAst()                 { return ast; }
    public void            setAst(String a)         { this.ast = a; }

    public List<String>    getErrors()              { return errors; }
    public void            setErrors(List<String> e){ this.errors = e; }

    public List<String>    getWarnings()               { return warnings; }
    public void            setWarnings(List<String> w) { this.warnings = w; }

    public List<Suggestion> getSuggestions()              { return suggestions; }
    public void             setSuggestions(List<Suggestion> s){ this.suggestions = s; }

    public String          getDialectNote()              { return dialectNote; }
    public void            setDialectNote(String d)      { this.dialectNote = d; }

    public String          getOptimizedQuery()           { return optimizedQuery; }
    public void            setOptimizedQuery(String q)   { this.optimizedQuery = q; }
}
