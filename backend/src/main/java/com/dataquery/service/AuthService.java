package com.dataquery.service;

import com.dataquery.dto.AuthResponse;
import com.dataquery.dto.LoginRequest;
import com.dataquery.dto.RegisterRequest;
import com.dataquery.model.Role;
import com.dataquery.model.User;
import com.dataquery.repository.UserRepository;
import com.dataquery.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Maneja el registro, login y seguridad de autenticación.
 * Incluye rate limiting: bloquea la cuenta después de 5 intentos fallidos.
 */
@Service
public class AuthService {

    private static final int    MAX_ATTEMPTS   = 5;
    private static final int    LOCK_MINUTES   = 15;

    @Autowired private UserRepository       userRepository;
    @Autowired private PasswordEncoder      passwordEncoder;
    @Autowired private JwtUtil              jwtUtil;
    @Autowired private AuthenticationManager authManager;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    // ── Registro ────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest req) {

        // Verificar que el email no esté registrado
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException(
                "Ya existe una cuenta con el email: " + req.getEmail());
        }

        // Validar fortaleza de contraseña
        validatePassword(req.getPassword());

        // Crear el usuario con contraseña hasheada (BCrypt factor 12)
        User user = new User();
        user.setNombre(req.getNombre());
        user.setApellido(req.getApellido());
        user.setEmail(req.getEmail().toLowerCase().trim());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(Role.STUDENT); // todos empiezan como estudiante

        userRepository.save(user);

        String token = jwtUtil.generateToken(user);
        return buildResponse(token, user);
    }

    // ── Login ────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest req) {
        String email = req.getEmail().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));

        // Verificar si la cuenta está bloqueada por intentos fallidos
        if (!user.isAccountNonLocked()) {
            throw new LockedException(
                "Cuenta bloqueada por " + LOCK_MINUTES + " min por múltiples intentos fallidos. " +
                "Intenta de nuevo más tarde.");
        }

        // Intentar autenticar
        try {
            Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, req.getPassword()));

            // Login exitoso — resetear contador de intentos
            user.setLoginAttempts(0);
            user.setLockedUntil(null);
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            User authenticatedUser = (User) auth.getPrincipal();
            String token = jwtUtil.generateToken(authenticatedUser);
            return buildResponse(token, authenticatedUser);

        } catch (BadCredentialsException e) {
            // Incrementar contador de intentos fallidos
            int attempts = user.getLoginAttempts() + 1;
            user.setLoginAttempts(attempts);

            if (attempts >= MAX_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                userRepository.save(user);
                throw new LockedException(
                    "Cuenta bloqueada " + LOCK_MINUTES + " minutos por " +
                    MAX_ATTEMPTS + " intentos fallidos.");
            }

            userRepository.save(user);
            int remaining = MAX_ATTEMPTS - attempts;
            throw new BadCredentialsException(
                "Credenciales incorrectas. Intentos restantes: " + remaining);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private void validatePassword(String password) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("La contraseña debe tener al menos una mayúscula");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("La contraseña debe tener al menos un número");
        }
    }

    private AuthResponse buildResponse(String token, User user) {
        return new AuthResponse(
            token,
            user.getNombre(),
            user.getApellido(),
            user.getEmail(),
            user.getRole().name(),
            jwtExpiration
        );
    }
}
