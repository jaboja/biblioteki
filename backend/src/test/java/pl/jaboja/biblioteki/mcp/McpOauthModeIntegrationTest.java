package pl.jaboja.biblioteki.mcp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;


/**
 * Integration tests for the MCP authorization chain in OAUTH mode:
 * unauthenticated /mcp requests are rejected with 401 + WWW-Authenticate,
 * the authorization server exposes metadata and open DCR.
 */
@SpringBootTest(properties = "biblioteki.mcp.auth-mode=oauth")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
@ActiveProfiles("test")
class McpOauthModeIntegrationTest {

    private static final String DCR_BODY = """
        {
          "client_name": "mcp-integration-test",
          "redirect_uris": ["http://127.0.0.1:49152/callback"],
          "grant_types": ["authorization_code"],
          "response_types": ["code"],
          "token_endpoint_auth_method": "none"
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    private String dcrClientId() throws Exception {
        MvcResult registered = mockMvc.perform(post("/oauth2/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(DCR_BODY))
            .andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.client_id").isNotEmpty())
            .andReturn();
        return registered.getResponse().getContentAsString()
            .replaceAll("(?s).*\"client_id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    @Test
    void mcpWithoutTokenIsUnauthorizedWithBearerChallenge() throws Exception {
        mockMvc.perform(post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":"
                        + "{\"protocolVersion\":\"2025-03-26\",\"capabilities\":{},"
                        + "\"clientInfo\":{\"name\":\"test\",\"version\":\"1.0\"}}}"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string("WWW-Authenticate", Matchers.startsWith("Bearer")))
            .andExpect(header().string("WWW-Authenticate", Matchers.containsString("resource_metadata")));
    }

    @Test
    void authorizationServerMetadataIsExposed() throws Exception {
        mockMvc.perform(get("/.well-known/oauth-authorization-server"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.issuer").isNotEmpty())
            .andExpect(jsonPath("$.authorization_endpoint").isNotEmpty())
            .andExpect(jsonPath("$.token_endpoint").isNotEmpty())
            .andExpect(jsonPath("$.registration_endpoint").isNotEmpty());
    }

    @Test
    void dynamicClientRegistrationIsOpenAndReturnsClientId() throws Exception {
        mockMvc.perform(post("/oauth2/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(DCR_BODY))
            .andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.client_id").isNotEmpty());
    }

    @Test
    void authorizeWithoutSessionRedirectsToLogin() throws Exception {
        String clientId = dcrClientId();

        mockMvc.perform(get("/oauth2/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", "http://127.0.0.1:49152/callback")
                // no scope: the DCR-registered client has no registered scopes
                .queryParam("state", "test-state")
                .queryParam("code_challenge", "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")
                .queryParam("code_challenge_method", "S256")
                .header("Accept", MediaType.TEXT_HTML_VALUE))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", Matchers.containsString("/login")));
    }
}
