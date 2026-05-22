package com.dataquery.config;

import com.dataquery.model.Role;
import com.dataquery.model.User;
import com.dataquery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea el usuario profesor inicial si no existe.
 * Corre automáticamente una vez al iniciar la aplicación.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private UserRepository  userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String PROFESOR_EMAIL    = "profesor@dataquery.com";
    private static final String PROFESOR_PASSWORD = "Profesor@2026!";

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(PROFESOR_EMAIL)) return;

        User profesor = new User();
        profesor.setNombre("Profesor");
        profesor.setApellido("Admin");
        profesor.setEmail(PROFESOR_EMAIL);
        profesor.setPassword(passwordEncoder.encode(PROFESOR_PASSWORD));
        profesor.setRole(Role.PROFESSOR);

        userRepository.save(profesor);
        System.out.println("[DataQuery] Cuenta profesor creada: " + PROFESOR_EMAIL);
    }
}
