package pl.jaboja.biblioteki.mcp.tools;

import org.springframework.ai.mcp.annotation.McpTool;
import pl.jaboja.biblioteki.account.LibraryAccount;
import pl.jaboja.biblioteki.account.LibraryAccountRepository;
import pl.jaboja.biblioteki.account.LibraryResponse;
import pl.jaboja.biblioteki.library.LibraryDefinition;

import java.util.List;
import java.util.stream.Stream;

/**
 * MCP tools for library account operations.
 * Provides read-only access to account data.
 * Note: Account modification (create, update, delete) is NOT exposed via MCP.
 */
public class AccountTools {
    /**
     * MCP-specific DTO for library account information.
     * Excludes username and password for security.
     */
    public record McpAccountResponse(
            Long id,
            String library,
            String libraryDisplayName
    ) {
        static McpAccountResponse from(LibraryAccount a) {
            return new McpAccountResponse(
                    a.getId(),
                    a.getLibrary().name(),
                    a.getLibrary().getDisplayName()
            );
        }
    }

    private final LibraryAccountRepository accountRepository;

    public AccountTools(LibraryAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Lists all enabled library accounts.
     * Note: Passwords and usernames are NOT included in the response.
     * Disabled accounts are omitted from the list.
     */
    @McpTool(
        name = "list_accounts",
        description = "Zwraca listę aktywnych kont dostępu do bibliotek (bez nazw użytkowników i haseł).",
        title = "List Accounts"
    )
    public List<McpAccountResponse> listAccounts() {
        return accountRepository.findAll()
            .stream()
            .filter(LibraryAccount::isEnabled)
            .map(McpAccountResponse::from)
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
        return Stream.of(LibraryDefinition.values())
            .map(l -> new LibraryResponse(l.name(), l.getDisplayName(), l.getLocation()))
            .toList();
    }
}
