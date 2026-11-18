package pl.jaboja.biblioteki.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for the MCP server in the default LOCAL (development) mode:
 * no token is required, a session connects, and all expected tools are
 * registered with English descriptions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
@ActiveProfiles("test")
class McpLocalModeIntegrationTest {

    /** Dev default (application.yml): local calls without token. */
    private static final String INITIALIZE_BODY = """
        {"jsonrpc":"2.0","id":1,"method":"initialize","params":{
          "protocolVersion":"2025-03-26","capabilities":{},
          "clientInfo":{"name":"test","version":"1.0"}}}
        """;

    private static final String TOOLS_LIST_BODY =
        "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper();

    /** Response bodies may be raw JSON or an SSE envelope with a data: line. */
    private static JsonNode parsePayload(String body, ObjectMapper mapper) throws Exception {
        if (body.contains("data:")) {
            Matcher m = Pattern.compile("^data:(.*)$", Pattern.MULTILINE).matcher(body);
            if (m.find()) {
                body = m.group(1).trim();
            }
        }
        return mapper.readTree(body);
    }

    private String openSession() throws Exception {
        MvcResult initialize = mockMvc.perform(post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .content(INITIALIZE_BODY))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andReturn();

        JsonNode payload = parsePayload(initialize.getResponse().getContentAsString(), mapper);
        assertThat(payload.path("result").path("serverInfo").path("name").asText())
            .as("MCP initialize handshake")
            .isNotEmpty();

        String sessionId = initialize.getResponse().getHeader("Mcp-Session-Id");
        assertThat(sessionId).as("Mcp-Session-Id assigned by the transport").isNotNull();

        mockMvc.perform(post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .header("Mcp-Session-Id", sessionId)
                .content("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isAccepted());

        return sessionId;
    }

    @Test
    void mcpSessionConnectsWithoutToken() throws Exception {
        openSession();
    }

    @Test
    void allMcpToolsAreRegisteredWithEnglishDescriptions() throws Exception {
        String sessionId = openSession();

        MvcResult tools = mockMvc.perform(post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .header("Mcp-Session-Id", sessionId)
                .content(TOOLS_LIST_BODY))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andReturn();

        JsonNode payload = parsePayload(tools.getResponse().getContentAsString(), mapper);
        JsonNode items = payload.path("result").path("tools");

        assertThat(items).hasSize(4);
        assertThat(namesOf(items)).containsExactlyInAnyOrder(
            "list_loans", "refresh_loans", "renew_loan", "list_accounts");

        for (JsonNode tool : items) {
            assertThat(tool.path("description").asText())
                .as("description of %s", tool.path("name").asText())
                .isNotBlank()
                .matches(s -> s.matches("(?s)[A-Z].*"),
                    "%s description should start with a capital letter (English)".formatted(tool.path("name").asText()));
        }
    }

    private String[] namesOf(JsonNode tools) {
        return java.util.stream.StreamSupport.stream(tools.spliterator(), false)
            .map(t -> t.path("name").asText())
            .toArray(String[]::new);
    }
}
