package com.dataquery.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "El nombre es requerido")
    private String nombre;

    @NotBlank(message = "El apellido es requerido")
    private String apellido;

    @Email(message = "Email inválido")
    @NotBlank(message = "El email es requerido")
    private String email;

    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @NotBlank(message = "La contraseña es requerida")
    private String password;

    public String getNombre()           { return nombre; }
    public void   setNombre(String v)   { this.nombre = v; }
    public String getApellido()         { return apellido; }
    public void   setApellido(String v) { this.apellido = v; }
    public String getEmail()            { return email; }
    public void   setEmail(String v)    { this.email = v; }
    public String getPassword()         { return password; }
    public void   setPassword(String v) { this.password = v; }
}
