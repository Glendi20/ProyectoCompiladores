package com.dataquery.dto;

public class AuthResponse {
    private String token;
    private String nombre;
    private String apellido;
    private String email;
    private String role;
    private long   expiresIn;   // milisegundos

    public AuthResponse(String token, String nombre, String apellido,
                        String email, String role, long expiresIn) {
        this.token     = token;
        this.nombre    = nombre;
        this.apellido  = apellido;
        this.email     = email;
        this.role      = role;
        this.expiresIn = expiresIn;
    }

    public String getToken()     { return token; }
    public String getNombre()    { return nombre; }
    public String getApellido()  { return apellido; }
    public String getEmail()     { return email; }
    public String getRole()      { return role; }
    public long   getExpiresIn() { return expiresIn; }
}
