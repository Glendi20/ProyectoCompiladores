package com.dataquery.model;

public class Suggestion {
    private String type;
    private String title;
    private String message;
    private String example;

    public Suggestion(String type, String title, String message, String example) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.example = example;
    }

    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getExample() { return example; }
}
