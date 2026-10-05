package pl.jaboja.biblioteki.loans;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * DTO for loan list response.
 */
@Schema(description = "Odpowiedź z listą wypożyczeń")
public record LoansResponse(
    @Schema(description = "Data pobrania danych", example = "2026-10-03T18:00:00Z")
    Instant fetchedAt,
    @Schema(description = "Lista wypożyczeń")
    List<Loan> loans,
    @Schema(description = "Lista błędów (pusta jeśli wszystko OK)")
    List<String> errors
) {
    public static LoansResponse from(LoansService.LoansResult result) {
        return new LoansResponse(Instant.now(), result.loans(), result.errors());
    }
}
