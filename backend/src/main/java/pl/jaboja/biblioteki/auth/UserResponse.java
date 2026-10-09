package pl.jaboja.biblioteki.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for user response (without sensitive data)
 */
public record UserResponse(
    @Schema(description = "Identyfikator użytkownika")
    Long id,

    @Schema(description = "Nazwa użytkownika")
    String username,

    @Schema(description = "Pełne imię i nazwisko")
    String fullName,

    @Schema(description = "Czy konto jest aktywne")
    boolean enabled,

    @Schema(description = "Role użytkownika")
    String roles
) {}
