package pl.jaboja.biblioteki.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for user creation and update requests.
 */
public record UserRequest(
    @Schema(description = "Nazwa użytkownika", example = "user123")
    @NotBlank(message = "Nazwa użytkownika jest wymagana")
    @Size(min = 3, max = 50, message = "Nazwa użytkownika powinna mieć od 3 do 50 znaków")
    String username,

    @Schema(description = "Hasło", example = "Password1!")
    @Size(min = 6, message = "Hasło musi mieć przynajmniej 6 znaków")
    String password,

    @Schema(description = "Pełne imię i nazwisko", example = "Jan Kowalski")
    @Size(max = 100, message = "Pełne imię i nazwisko nie może mieć więcej niż 100 znaków")
    String fullName,

    @Schema(description = "Role użytkownika", example = "USER,ADMIN", allowableValues = {"USER", "ADMIN", "USER,ADMIN"})
    String roles,

    @Schema(description = "Czy konto jest aktywne")
    Boolean enabled
) {
    /**
     * DTO for user self-update (limited fields)
     */
    public record SelfUpdateRequest(
        @Schema(description = "Nazwa użytkownika", example = "user123")
        @Size(min = 3, max = 50, message = "Nazwa użytkownika powinna mieć od 3 do 50 znaków")
        String username,

        @Schema(description = "Hasło", example = "Password1!")
        @Size(min = 6, message = "Hasło musi mieć przynajmniej 6 znaków")
        String password,

        @Schema(description = "Pełne imię i nazwisko", example = "Jan Kowalski")
        @Size(max = 100, message = "Pełne imię i nazwisko nie może mieć więcej niż 100 znaków")
        String fullName
    ) {}
}
