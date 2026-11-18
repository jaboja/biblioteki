package pl.jaboja.biblioteki.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import io.modelcontextprotocol.common.McpTransportContext;
import pl.jaboja.biblioteki.auth.User;
import pl.jaboja.biblioteki.auth.UserRepository;

@ExtendWith(MockitoExtension.class)
class McpUserResolverTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private McpAuthProperties properties;

    @InjectMocks
    private McpUserResolver resolver;

    private static McpTransportContext context(String authorization) {
        Map<String, Object> attributes = new HashMap<>();
        if (authorization != null) {
            attributes.put("authorization", authorization);
        }
        return McpTransportContext.create(attributes);
    }

    private static User user(String username) {
        return new User(1L, username, "encoded", "Full Name", true, "USER");
    }

    @Test
    void localModeReturnsConfiguredUserWithoutToken() {
        when(properties.getAuthMode()).thenReturn(McpAuthProperties.AuthMode.LOCAL);
        when(properties.getLocalUsername()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(java.util.Optional.of(user("admin")));

        User result = resolver.resolveUser(context(null));

        assertThat(result.getUsername()).isEqualTo("admin");
    }

    @Test
    void localModeFailsFastWhenUserMissing() {
        when(properties.getAuthMode()).thenReturn(McpAuthProperties.AuthMode.LOCAL);
        when(properties.getLocalUsername()).thenReturn("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> resolver.resolveUser(context(null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ghost");
    }

    @Test
    void oauthModeWithoutHeaderFails() {
        when(properties.getAuthMode()).thenReturn(McpAuthProperties.AuthMode.OAUTH);

        assertThatThrownBy(() -> resolver.resolveUser(McpTransportContext.create(Map.of())))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Missing MCP bearer token");
    }

    @Test
    void oauthModeWithNonBearerHeaderFails() {
        when(properties.getAuthMode()).thenReturn(McpAuthProperties.AuthMode.OAUTH);

        assertThatThrownBy(() -> resolver.resolveUser(context("Basic dXNlcjpwYXNz")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Missing MCP bearer token");
    }

    @Test
    void oauthModeResolvesUserFromTokenSubject() {
        when(properties.getAuthMode()).thenReturn(McpAuthProperties.AuthMode.OAUTH);
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("user1").build();
        when(jwtDecoder.decode("token")).thenReturn(jwt);
        when(userRepository.findByUsername("user1")).thenReturn(java.util.Optional.of(user("user1")));

        User result = resolver.resolveUser(context("Bearer token"));

        assertThat(result.getUsername()).isEqualTo("user1");
    }

    @Test
    void oauthModeFailsWhenTokenUserUnknown() {
        when(properties.getAuthMode()).thenReturn(McpAuthProperties.AuthMode.OAUTH);
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("stranger").build();
        when(jwtDecoder.decode("token")).thenReturn(jwt);
        when(userRepository.findByUsername("stranger")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> resolver.resolveUser(context("Bearer token")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("stranger");
    }

    @Test
    void oauthModeWrapsJwtValidationFailure() {
        when(properties.getAuthMode()).thenReturn(McpAuthProperties.AuthMode.OAUTH);
        when(jwtDecoder.decode("expired")).thenThrow(new JwtException("token expired"));

        assertThatThrownBy(() -> resolver.resolveUser(context("Bearer expired")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Invalid MCP access token")
            .hasCauseInstanceOf(JwtException.class);
    }
}
