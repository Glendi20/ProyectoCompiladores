package com.dataquery.model;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Entidad JPA que representa un usuario del sistema.
 * Implementa UserDetails para integrarse con Spring Security.
 * Hibernate crea automáticamente la tabla 'users' en SQL Server.
 */
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(unique = true, nullable = false, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // Para rate limiting de login — se guarda en BD para persistir entre reinicios
    @Column(name = "login_attempts")
    private int loginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (role == null) role = Role.STUDENT;
    }

    // ── UserDetails (Spring Security) ────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getPassword()               { return password; }
    @Override public String getUsername()               { return email; }
    @Override public boolean isAccountNonExpired()      { return true; }
    @Override public boolean isCredentialsNonExpired()  { return true; }
    @Override public boolean isEnabled()                { return true; }

    @Override
    public boolean isAccountNonLocked() {
        return lockedUntil == null || lockedUntil.isBefore(LocalDateTime.now());
    }

    // ── Getters y Setters ─────────────────────────────────────────────────

    public Long getId()                    { return id; }
    public String getNombre()              { return nombre; }
    public void   setNombre(String v)      { this.nombre = v; }
    public String getApellido()            { return apellido; }
    public void   setApellido(String v)    { this.apellido = v; }
    public String getEmail()               { return email; }
    public void   setEmail(String v)       { this.email = v; }
    public void   setPassword(String v)    { this.password = v; }
    public Role   getRole()                { return role; }
    public void   setRole(Role v)          { this.role = v; }
    public LocalDateTime getCreatedAt()    { return createdAt; }
    public LocalDateTime getLastLogin()    { return lastLogin; }
    public void   setLastLogin(LocalDateTime v) { this.lastLogin = v; }
    public int    getLoginAttempts()       { return loginAttempts; }
    public void   setLoginAttempts(int v)  { this.loginAttempts = v; }
    public LocalDateTime getLockedUntil()  { return lockedUntil; }
    public void   setLockedUntil(LocalDateTime v) { this.lockedUntil = v; }

    public String getFullName() { return nombre + " " + apellido; }
}
