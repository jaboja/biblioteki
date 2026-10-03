package pl.jaboja.biblioteki.loans;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoansController {

    private final LoansService loansService;

    /**
     * GET /api/loans
     *
     * Zwraca aktualny stan wypożyczeń (z cache lub świeże przy pierwszym
     * wywołaniu / po wygaśnięciu TTL 10 min).
     *
     * Odpowiedź:
     * {
     *   "fetchedAt": "2026-04-01T12:00:00Z",
     *   "cached": true,
     *   "loans": [...],
     *   "errors": [...]           // puste jeśli wszystko OK
     * }
     */
    @GetMapping
    public ResponseEntity<LoansResponse> getLoans() {
        var result = loansService.fetchAll();
        return ResponseEntity.ok(LoansResponse.from(result));
    }

    /**
     * POST /api/loans/refresh
     *
     * Wymusza natychmiastowe ponowne pobranie danych z bibliotek,
     * ignorując i evictując cache.  Przydatne po zmianie kont
     * lub na żądanie użytkownika.
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoansResponse> refresh() {
        var result = loansService.refresh();
        return ResponseEntity.ok(LoansResponse.from(result));
    }

    // --- DTO odpowiedzi ---

    record LoansResponse(
        Instant fetchedAt,
        List<Loan> loans,
        List<String> errors
    ) {
        static LoansResponse from(LoansService.LoansResult result) {
            return new LoansResponse(Instant.now(), result.loans(), result.errors());
        }
    }
}
