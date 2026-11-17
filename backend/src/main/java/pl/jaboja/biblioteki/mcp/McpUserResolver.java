package pl.jaboja.biblioteki.mcp;

import io.modelcontextprotocol.common.McpTransportContext;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import pl.jaboja.biblioteki.auth.User;
import pl.jaboja.biblioteki.auth.UserRepository;

/**
 * Resolves the User for an MCP tool call.
 * OAUTH mode: validates the bearer token from the transport context and loads
 * the user by the token subject. LOCAL mode: returns the configured local
 * development user without any token validation.
 */
@Component
public class McpUserResolver {

    private static final String AUTHORIZATION_KEY = "authorization";

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserRepository userRepository;

    private final McpAuthProperties properties;

    private final JwtDecoder jwtDecoder;

    public McpUserResolver(UserRepository userRepository, McpAuthProperties properties, JwtDecoder jwtDecoder) {
        this.userRepository = userRepository;
        this.properties = properties;
        this.jwtDecoder = jwtDecoder;
    }

    public User resolveUser(McpTransportContext transportContext) {
        if (properties.getAuthMode() == McpAuthProperties.AuthMode.LOCAL) {
            return userRepository.findByUsername(properties.getLocalUsername())
                .orElseThrow(() -> new IllegalStateException(
                        "MCP local user not found: " + properties.getLocalUsername()));
        }

        Object authorization = transportContext.get(AUTHORIZATION_KEY);
        if (!(authorization instanceof String header) || !header.startsWith(BEARER_PREFIX)) {
            throw new IllegalStateException("Missing MCP bearer token");
        }
        try {
            Jwt jwt = jwtDecoder.decode(header.substring(BEARER_PREFIX.length()));
            return userRepository.findByUsername(jwt.getSubject())
                .orElseThrow(() -> new IllegalStateException("MCP token user not found: " + jwt.getSubject()));
        }
        catch (JwtException e) {
            throw new IllegalStateException("Invalid MCP access token", e);
        }
    }
}
