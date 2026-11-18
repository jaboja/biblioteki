package pl.jaboja.biblioteki.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

class McpRegisteredClientRepositoryTest {

    private final McpRegisteredClientRepository repository = new McpRegisteredClientRepository();

    private RegisteredClient client(String id, String clientId) {
        return RegisteredClient.withId(id)
            .clientId(clientId)
            .clientIdIssuedAt(Instant.now())
            .clientSecret("secret")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://127.0.0.1/callback")
            .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
            .build();
    }

    @Test
    void savesAndFindsById() {
        RegisteredClient c = client("id-1", "client-1");
        repository.save(c);

        assertThat(repository.findById("id-1")).isSameAs(c);
    }

    @Test
    void savesAndFindsByClientId() {
        RegisteredClient c = client("id-1", "client-1");
        repository.save(c);

        assertThat(repository.findByClientId("client-1")).isSameAs(c);
    }

    @Test
    void unknownClientReturnsNull() {
        assertThat(repository.findById("nope")).isNull();
        assertThat(repository.findByClientId("nope")).isNull();
    }

    @Test
    void savingWithSameIdReplacesClient() {
        RegisteredClient first = client("id-1", "client-1");
        RegisteredClient second = client("id-1", "client-2");
        repository.save(first);
        repository.save(second);

        assertThat(repository.findById("id-1")).isSameAs(second);
        assertThat(repository.findByClientId("client-1")).isNull();
        assertThat(repository.findByClientId("client-2")).isSameAs(second);
    }

    @Test
    void savingWithSameClientIdReplacesLookup() {
        RegisteredClient first = client("id-1", "client-1");
        RegisteredClient second = client("id-2", "client-1");
        repository.save(first);
        repository.save(second);

        assertThat(repository.findById("id-1")).isNull();
        assertThat(repository.findByClientId("client-1")).isSameAs(second);
    }

    @Test
    void rejectsNullArguments() {
        assertThatThrownBy(() -> repository.save(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repository.findById(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repository.findByClientId(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
