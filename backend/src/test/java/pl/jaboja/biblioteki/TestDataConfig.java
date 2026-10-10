package pl.jaboja.biblioteki;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Test configuration that replaces the JWT filter with a no-op filter
 * to allow @WithMockUser annotations to work properly in tests.
 * This prevents tests from requiring actual JWT tokens.
 */
@TestConfiguration
public class TestDataConfig {

    /**
     * Replace JWT filter with no-op filter for tests
     * This allows @WithMockUser to work without JWT tokens
     */
    @Bean
    public OncePerRequestFilter jwtAuthenticationFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain) throws ServletException, IOException {
                // No-op: just continue the filter chain
                filterChain.doFilter(request, response);
            }
        };
    }
}
