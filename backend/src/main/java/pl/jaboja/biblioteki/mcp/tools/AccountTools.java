package pl.jaboja.biblioteki.mcp.tools;

import java.util.List;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.context.McpSyncRequestContext;

import pl.jaboja.biblioteki.account.LibraryAccount;
import pl.jaboja.biblioteki.account.LibraryAccountRepository;
import pl.jaboja.biblioteki.auth.User;
import pl.jaboja.biblioteki.mcp.McpUserResolver;

/**
 * MCP tools for library account operations.
 * Provides read-only access to account data.
 * Note: Account modification (create, update, delete) is NOT exposed via MCP.
 */
public class AccountTools {

    /**
     * MCP-specific DTO joining a user's library account with its library definition.
     * libraryId = library identifier (enum key, as used in loan IDs like 'MBP_12345'),
     * libraryName = human-readable library name.
     * Excludes username and password for security.
     */
    public record McpAccountResponse(
            Long id,
            String libraryId,
            String libraryName,
            String location
    ) {
        static McpAccountResponse from(LibraryAccount a) {
            return new McpAccountResponse(
                    a.getId(),
                    a.getLibrary().name(),
                    a.getLibrary().getDisplayName(),
                    a.getLibrary().getLocation()
            );
        }
    }

    private final LibraryAccountRepository accountRepository;

    private final McpUserResolver userResolver;

    public AccountTools(LibraryAccountRepository accountRepository, McpUserResolver userResolver) {
        this.accountRepository = accountRepository;
        this.userResolver = userResolver;
    }

    private User getCurrentUser(McpSyncRequestContext context) {
        return userResolver.resolveUser(context.transportContext());
    }

    /**
     * Lists the current user's library accounts, each joined with its library definition.
     * Libraries for which the user has no account are not visible.
     * Usernames and passwords are never included; disabled accounts are omitted.
     */
    @McpTool(
        name = "list_accounts",
        description = "Returns the currently logged-in user's library accounts, joined with their library definitions. libraryId is the library identifier (as used in loan IDs, e.g. 'MBP' in 'MBP_12345'), libraryName is the human-readable name. Only enabled accounts are returned; libraries without an account are not visible. Usernames and passwords are never included.",
        title = "List Accounts"
    )
    public List<McpAccountResponse> listAccounts(McpSyncRequestContext context) {
        User currentUser = getCurrentUser(context);
        return accountRepository.findByUserAndEnabledTrue(currentUser)
            .stream()
            .map(McpAccountResponse::from)
            .toList();
    }
}
