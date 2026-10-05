package pl.jaboja.biblioteki.account;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for library information response.
 */
@Schema(description = "Informacje o dostępnej bibliotece")
public record LibraryResponse(
    @Schema(description = "Identyfikator biblioteki", example = "UW")
    String id,
    @Schema(description = "Nazwa biblioteki", example = "Biblioteka Uniwersytecka")
    String name,
    @Schema(description = "Lokalizacja biblioteki", example = "Warszawa")
    String location
) {}
