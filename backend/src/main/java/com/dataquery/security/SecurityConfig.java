package com.dataquery.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración central de Spring Security.
 * Define qué endpoints son públicos, cuáles requieren login y cuáles requieren rol PROFESSOR.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // permite usar @PreAuthorize en los controllers
public class SecurityConfig {

    @Autowired private UserDetailsServiceImpl userDetailsService;
    @Autowired private JwtAuthFilter          jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Desactivar CSRF — usamos JWT, no sesiones de servidor
            .csrf(csrf -> csrf.disable())

            // Configurar CORS para permitir peticiones desde React (puerto 3000)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Sin estado de sesión — JWT es stateless
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Reglas de autorización por endpoint
            .authorizeHttpRequests(auth -> auth

                // ── Públicos — sin login ──────────────────────────────
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/databases").permitAll()
                .requestMatchers("/h2-console/**").permitAll()   // solo en dev

                // ── Solo PROFESSOR ────────────────────────────────────
                .requestMatchers("/api/dashboard/global/**").hasRole("PROFESSOR")
                .requestMatchers("/api/dashboard/users/**").hasRole("PROFESSOR")

                // ── Cualquier usuario autenticado ─────────────────────
                .requestMatchers("/api/compile").authenticated()
                .requestMatchers("/api/schema").authenticated()
                .requestMatchers("/api/examples").authenticated()
                .requestMatchers("/api/dashboard/**").authenticated()

                // ── Todo lo demás requiere login ─────────────────────
                .anyRequest().authenticated()
            )

            // Agregar el filtro JWT antes del filtro estándar de autenticación
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // Proveedor de autenticación personalizado
            .authenticationProvider(authenticationProvider());

        return http.build();
    }

    /** BCrypt factor 12 — tarda ~300ms por hash, hace brute force muy lento */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /** CORS: permite peticiones del frontend React en puerto 3000 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
