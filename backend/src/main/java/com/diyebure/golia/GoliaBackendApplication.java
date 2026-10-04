package com.diyebure.golia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Punto de entrada del backend GolIA.
 *
 * <p>Expone una API REST (auth, predicciones y ranking) respaldada por MySQL.
 * Se excluye {@link UserDetailsServiceAutoConfiguration} para que Spring Boot
 * no registre un {@code inMemoryUserDetailsManager} que activaria la
 * autenticacion basica HTTP sobre todos los endpoints, bloqueando el login y el
 * registro. La seguridad la gestiona exclusivamente {@code SecurityConfig} con
 * JWT.</p>
 */
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class GoliaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoliaBackendApplication.class, args);
    }
}
