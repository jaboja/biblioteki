package pl.jaboja.biblioteki.account;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for library account response.
 */
@Schema(description = "Odpowiedź z informacjami o koncie")
public record AccountResponse(
    @Schema(description = "Identyfikator konta", example = "1")
    Long id,
    @Schema(description = "Identyfikator biblioteki", example = "UW")
    String library,
    @Schema(description = "Wyświetlana nazwa biblioteki", example = "Biblioteka Uniwersytecka")
    String libraryDisplayName,
    @Schema(description = "Nazwa użytkownika", example = "jan.kowalski")
    String username,
    @Schema(description = "Czy konto jest aktywne", example = "true")
    boolean enabled
) {
    public static AccountResponse from(LibraryAccount a) {
        return new AccountResponse(
            a.getId(),
            a.getLibrary().name(),
            a.getLibrary().getDisplayName(),
            a.getUsername(),
            a.isEnabled()
        );
    }
}
