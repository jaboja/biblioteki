package pl.jaboja.biblioteki.mcp;

import java.util.Map;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
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
    public LoansTools loansTools(LoansService loansService, McpUserResolver userResolver) {
        return new LoansTools(loansService, userResolver);
    }

    @Bean
    public AccountTools accountTools(LibraryAccountRepository accountRepository, McpUserResolver userResolver) {
        return new AccountTools(accountRepository, userResolver);
    }

    @Bean
    public WebMvcStreamableServerTransportProvider webMvcStreamableServerTransportProvider(
            @Qualifier("mcpServerJsonMapper") JsonMapper jsonMapper,
            McpServerStreamableHttpProperties serverProperties) {
        return WebMvcStreamableServerTransportProvider.builder()
            .jsonMapper(new JacksonMcpJsonMapper(jsonMapper))
            .mcpEndpoint(serverProperties.getMcpEndpoint())
            .keepAliveInterval(serverProperties.getKeepAliveInterval())
            .disallowDelete(serverProperties.isDisallowDelete())
            .contextExtractor(request -> {
                String authorization = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);
                if (authorization == null) {
                    return McpTransportContext.EMPTY;
                }
                return McpTransportContext.create(Map.of("authorization", authorization));
            })
            .build();
    }
}
