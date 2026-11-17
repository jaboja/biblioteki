package pl.jaboja.biblioteki.mcp;

import java.util.List;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientRegistrationAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientRegistrationAuthenticationValidator;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * OAuth 2.1 authorization server for MCP clients.
 * Always active: publishes authorization server metadata, supports dynamic
 * client registration (RFC 7591) and issues signed JWT access tokens.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(McpAuthProperties.class)
public class McpAuthorizationServerConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        http.oauth2AuthorizationServer(authorizationServer -> {
            http.securityMatcher(authorizationServer.getEndpointsMatcher());
            authorizationServer.oidc(Customizer.withDefaults());
            authorizationServer.clientRegistrationEndpoint(clientRegistration -> {
                clientRegistration.openRegistrationAllowed(true);
                clientRegistration.authenticationProviders(providers -> providers.forEach(provider -> {
                    if (provider instanceof OAuth2ClientRegistrationAuthenticationProvider clientRegistrationProvider) {
                        clientRegistrationProvider.setAuthenticationValidator(
                            OAuth2ClientRegistrationAuthenticationValidator.DEFAULT_REDIRECT_URI_VALIDATOR
                                .andThen(OAuth2ClientRegistrationAuthenticationValidator.DEFAULT_JWK_SET_URI_VALIDATOR));
                    }
                }));
            });
        });
        http.authorizeHttpRequests(authorize -> authorize
            .requestMatchers(clientRegistrationRequestMatcher()).permitAll()
            .anyRequest().authenticated());
        http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));
        http.exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")));
        return http.build();
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(McpAuthProperties properties) {
        return AuthorizationServerSettings.builder()
            .issuer(properties.getIssuerUrl())
            .build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        return new McpRegisteredClientRepository();
    }

    @Bean
    public JwtDecoder mcpJwtDecoder(JWKSource<SecurityContext> jwkSource, McpAuthProperties properties) {
        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
        JwtClaimValidator<List<String>> audienceValidator = new JwtClaimValidator<>("aud",
                aud -> aud != null && aud.contains(properties.getIssuerUrl()));
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.getIssuerUrl()), audienceValidator));
        return jwtDecoder;
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> mcpJwtCustomizer(McpAuthProperties properties) {
        return context -> {
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                context.getClaims().audience(List.of(properties.getIssuerUrl()));
                List<String> roles = context.getPrincipal().getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(authority -> authority.startsWith("ROLE_"))
                    .toList();
                context.getClaims().claim("roles", roles);
            }
        };
    }

    private static RequestMatcher clientRegistrationRequestMatcher() {
        return PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/oauth2/register");
    }
}
