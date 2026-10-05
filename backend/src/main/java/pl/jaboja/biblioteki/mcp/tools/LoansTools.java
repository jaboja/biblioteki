package pl.jaboja.biblioteki.mcp.tools;

import org.springframework.ai.mcp.annotation.McpTool;
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
        var result = loansService.fetchAll();
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
        var result = loansService.refresh();
        return LoansResponse.from(result);
    }
}
