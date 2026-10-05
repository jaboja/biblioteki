package pl.jaboja.biblioteki.mcp;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.jaboja.biblioteki.account.LibraryAccountRepository;
import pl.jaboja.biblioteki.loans.LoansService;
import pl.jaboja.biblioteki.mcp.tools.AccountTools;
import pl.jaboja.biblioteki.mcp.tools.LoansTools;

/**
 * Configuration class for MCP server.
 * Registers all MCP tools and enables the MCP server.
 */
@Configuration
public class McpConfig {

    @Bean
    public LoansTools loansTools(LoansService loansService) {
        return new LoansTools(loansService);
    }

    @Bean
    public AccountTools accountTools(LibraryAccountRepository accountRepository) {
        return new AccountTools(accountRepository);
    }
}
