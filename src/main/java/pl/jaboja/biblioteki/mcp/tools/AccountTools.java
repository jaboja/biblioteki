package pl.jaboja.biblioteki.mcp.tools;

import org.springframework.ai.mcp.annotation.McpTool;
import pl.jaboja.biblioteki.account.AccountResponse;
import pl.jaboja.biblioteki.account.LibraryAccountRepository;
import pl.jaboja.biblioteki.account.LibraryResponse;
import pl.jaboja.biblioteki.library.LibraryDefinition;

import java.util.List;

/**
 * MCP tools for library account operations.
 * Provides read-only access to account data.
 * Note: Account modification (create, update, delete) is NOT exposed via MCP.
 */
public class AccountTools {

    private final LibraryAccountRepository accountRepository;

    public AccountTools(LibraryAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Lists all configured library accounts.
     * This is the same data returned by GET /api/accounts.
     * Note: Passwords are NOT included in the response.
     */
    @McpTool(
        name = "list_accounts",
        description = "Zwraca listę wszystkich skonfigurowanych kont dostępu do bibliotek (bez haseł).",
        title = "List Accounts"
    )
    public List<AccountResponse> listAccounts() {
        return accountRepository.findAll()
            .stream()
            .map(AccountResponse::from)
            .toList();
    }

    /**
     * Lists all available library definitions.
     * This is the same data returned by GET /api/accounts/libraries.
     */
    @McpTool(
        name = "list_libraries",
        description = "Zwraca listę wszystkich dostępnych definicji bibliotek (MBP, DBP, ZNO).",
        title = "List Libraries"
    )
    public List<LibraryResponse> listLibraries() {
        return List.of(LibraryDefinition.values())
            .stream()
            .map(l -> new LibraryResponse(l.name(), l.getDisplayName(), l.getLocation()))
            .toList();
    }
}
