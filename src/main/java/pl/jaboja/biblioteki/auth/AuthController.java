package pl.jaboja.biblioteki.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/**
 * REST controller for authentication operations.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Uwierzytelnianie użytkowników")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Login endpoint - returns JWT token.
     * Can be used by both REST clients and MCP clients.
     */
    @Operation(
        summary = "Zaloguj się",
        description = "Autentykacja użytkownika, zwraca token JWT do użycia w nagłówku Authorization: Bearer <token>"
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest loginRequest,
            HttpServletResponse response) {
        
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.username(),
                loginRequest.password()
            )
        );

        String token = jwtService.generateToken((User) authentication.getPrincipal());
        
        // Also set cookie for web login form
        ResponseCookie cookie = ResponseCookie.from("jwt", token)
            .httpOnly(true)
            .secure(false) // Set to true in production with HTTPS
            .path("/")
            .maxAge(Duration.ofHours(24))
            .build();
        
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(new AuthResponse(token));
    }

    /**
     * Logout endpoint - clears authentication cookie.
     */
    @Operation(summary = "Wyloguj się", description = "Wylogowuje użytkownika")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(0)
            .build();
        
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .build();
    }

    /**
     * Refresh token endpoint.
     */
    @Operation(
        summary = "Odśwież token",
        description = "Generuje nowy token JWT na podstawie ważnego tokenu"
    )
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        if (!jwtService.isTokenValid(token)) {
            return ResponseEntity.badRequest().build();
        }
        String username = jwtService.extractUsername(token);
        String roles = jwtService.getRolesFromToken(token);
        
        User user = new User();
        user.setUsername(username);
        user.setRoles(roles);
        
        String newToken = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(newToken));
    }

    public record LoginRequest(String username, String password) {}
    public record AuthResponse(String token) {}
}
