package pl.jaboja.biblioteki.loans;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@Tag(name = "Wypożyczenia", description = "Operacje związane z wypożyczeniami z bibliotek")
public class LoansController {

    private final LoansService loansService;

    @Operation(
        summary = "Pobierz listę wypożyczeń",
        description = "Zwraca aktualny stan wypożyczeń (z cache lub świeże przy pierwszym wywołaniu / po wygaśnięciu TTL 10 min)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Lista wypożyczeń",
                content = @Content(schema = @Schema(implementation = LoansResponse.class)))
        }
    )
    @GetMapping
    public ResponseEntity<LoansResponse> getLoans() {
        var result = loansService.fetchAll();
        return ResponseEntity.ok(LoansResponse.from(result));
    }

    @Operation(
        summary = "Wymuś odświeżenie wypożyczeń",
        description = "Wymusza natychmiastowe ponowne pobranie danych z bibliotek, ignorując cache",
        responses = {
            @ApiResponse(responseCode = "200", description = "Odświeżona lista wypożyczeń",
                content = @Content(schema = @Schema(implementation = LoansResponse.class)))
        }
    )
    @PostMapping("/refresh")
    public ResponseEntity<LoansResponse> refresh() {
        var result = loansService.refresh();
        return ResponseEntity.ok(LoansResponse.from(result));
    }

    // --- DTO odpowiedzi ---

    @Schema(description = "Odpowiedź z listą wypożyczeń")
    record LoansResponse(
        @Schema(description = "Data pobrania danych", example = "2026-10-03T18:00:00Z")
        Instant fetchedAt,
        @Schema(description = "Lista wypożyczeń")
        List<Loan> loans,
        @Schema(description = "Lista błędów (pusta jeśli wszystko OK)")
        List<String> errors
    ) {
        static LoansResponse from(LoansService.LoansResult result) {
            return new LoansResponse(Instant.now(), result.loans(), result.errors());
        }
    }
}
