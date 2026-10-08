package pl.jaboja.biblioteki.mcp.tools;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import pl.jaboja.biblioteki.auth.User;
import pl.jaboja.biblioteki.loans.LoansResponse;
import pl.jaboja.biblioteki.loans.LoansService;

/**
 * MCP tools for library loans operations.
 * Provides read-only access to loan data.
 */
public class LoansTools {

    private final LoansService loansService;

    public LoansTools(LoansService loansService) {
        this.loansService = loansService;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getId();
        }
        throw new IllegalStateException("Użytkownik nie jest uwierzytelniony");
    }

    /**
     * Retrieves the current list of loans (from cache).
     * This is the same data returned by GET /api/loans.
     */
    @McpTool(
        name = "list_loans",
        description = "Pobierz listę wypożyczeń z cache (10-minutowy TTL). Zwraca aktualny stan wypożyczeń z bibliotek.",
        title = "List Loans"
    )
    public LoansResponse listLoans() {
        Long userId = getCurrentUserId();
        var result = loansService.fetchAll(userId);
        return LoansResponse.from(result);
    }

    /**
     * Forces a refresh of loan data from the library systems.
     * This is the same as POST /api/loans/refresh.
     */
    @McpTool(
        name = "refresh_loans",
        description = "Wymusza natychmiastowe odświeżenie danych wypożyczeń z bibliotek, ignorując cache.",
        title = "Refresh Loans"
    )
    public LoansResponse refreshLoans() {
        Long userId = getCurrentUserId();
        var result = loansService.refresh(userId);
        return LoansResponse.from(result);
    }

    /**
     * Renews a single loan.
     * This is the same as POST /api/loans/{loanId}/renew.
     */
    @McpTool(
        name = "renew_loan",
        description = "Przetwarza prolongatę pojedynczego wypożyczenia. Wymaga ID wypożyczenia w formacie 'LIBRARYID_rawLoanId' (np. 'MBP_12345').",
        title = "Renew Loan"
    )
    public String renewLoan(String loanId) {
        Long userId = getCurrentUserId();
        loansService.renewLoan(userId, loanId);
        return "Wypożyczenie " + loanId + " przedłużone pomyślnie";
    }
}
