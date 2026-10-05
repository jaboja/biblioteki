package pl.jaboja.biblioteki.loans;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @Operation(
        summary = "Przedłuż wypożyczenie",
        description = "Przetwarza prolongatę pojedynczego wypożyczenia. Wymaga ID wypożyczenia w formacie 'LIBRARYID_rawLoanId'",
        responses = {
            @ApiResponse(responseCode = "200", description = "Prolongata udana"),
            @ApiResponse(responseCode = "400", description = "Błędny format ID wypożyczenia"),
            @ApiResponse(responseCode = "404", description = "Nie znaleziono konta dla biblioteki"),
            @ApiResponse(responseCode = "500", description = "Błąd serwera biblioteki")
        }
    )
    @PostMapping("/{loanId}/renew")
    public ResponseEntity<String> renewLoan(@PathVariable String loanId) {
        loansService.renewLoan(loanId);
        return ResponseEntity.ok("Loan " + loanId + " renewed successfully");
    }
}
