package pl.jaboja.biblioteki.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import pl.jaboja.biblioteki.mcp.McpAuthProperties;

/**
 * Security configuration for the application.
 * Secures all /api/* endpoints with JWT authentication.
 * The /mcp endpoint is secured with OAuth 2.1 (see McpAuthorizationServerConfig),
 * or open in the local development mode (biblioteki.mcp.auth-mode=local).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain mcpSecurityFilterChain(HttpSecurity http, McpAuthProperties mcpAuthProperties,
            JwtDecoder mcpJwtDecoder) throws Exception {
        http
            .securityMatcher("/mcp/**", "/mcp")
            .csrf(csrf -> csrf.disable());

        if (mcpAuthProperties.getAuthMode() == McpAuthProperties.AuthMode.OAUTH) {
            http
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(mcpJwtDecoder)));
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }

        return http.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/", "/login", "/login.html", "/css/**", "/js/**", "/error").permitAll()
                .requestMatchers("/api-docs/**", "/swagger-ui/**").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()

                // Secure all other API endpoints
                .requestMatchers("/api/**").authenticated()

                // All other requests
                .anyRequest().permitAll()
            )

            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )

            // Form login backs the OAuth 2.1 authorization code flow (browser step)
            .formLogin(Customizer.withDefaults())

            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
