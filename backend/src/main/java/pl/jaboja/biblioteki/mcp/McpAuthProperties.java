package pl.jaboja.biblioteki.mcp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "biblioteki.mcp")
public class McpAuthProperties {

    public enum AuthMode {
        LOCAL, OAUTH
    }

    private AuthMode authMode = AuthMode.OAUTH;

    private String localUsername = "user";

    private String issuerUrl = "http://127.0.0.1:8292";

    public AuthMode getAuthMode() {
        return authMode;
    }

    public void setAuthMode(AuthMode authMode) {
        this.authMode = authMode;
    }

    public String getLocalUsername() {
        return localUsername;
    }

    public void setLocalUsername(String localUsername) {
        this.localUsername = localUsername;
    }

    public String getIssuerUrl() {
        return issuerUrl;
    }

    public void setIssuerUrl(String issuerUrl) {
        this.issuerUrl = issuerUrl;
    }
}
