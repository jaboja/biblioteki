package pl.jaboja.biblioteki.mcp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.Assert;

/**
 * In-memory repository for dynamically registered MCP clients (RFC 7591).
 * Registrations are lost on application restart.
 */
public class McpRegisteredClientRepository implements RegisteredClientRepository {

    private final Map<String, RegisteredClient> byId = new ConcurrentHashMap<>();

    private final Map<String, RegisteredClient> byClientId = new ConcurrentHashMap<>();

    @Override
    public void save(RegisteredClient registeredClient) {
        Assert.notNull(registeredClient, "registeredClient cannot be null");
        this.byId.put(registeredClient.getId(), registeredClient);
        this.byClientId.put(registeredClient.getClientId(), registeredClient);
    }

    @Override
    public RegisteredClient findById(String id) {
        Assert.hasText(id, "id cannot be empty");
        return this.byId.get(id);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        Assert.hasText(clientId, "clientId cannot be empty");
        return this.byClientId.get(clientId);
    }
}
