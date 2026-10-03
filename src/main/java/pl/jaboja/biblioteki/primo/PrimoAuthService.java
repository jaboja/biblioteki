package pl.jaboja.biblioteki.primo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.jaboja.biblioteki.library.LibraryDefinition;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrimoAuthService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String USER_AGENT =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
        "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.4 Safari/605.1.15";

    /**
     * Loguje się do instancji Primo VE i zwraca token JWT.
     * Każde wywołanie tworzy izolowany CookieManager, więc sesje
     * różnych kont nie interferują ze sobą.
     *
     * @throws PrimoException gdy logowanie się nie powiedzie
     */
    public PrimoSession login(LibraryDefinition lib, String username, String password) {
        // Izolowany cookie store per sesja – tak samo jak ephemeral URLSession w Swifcie
        var cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        var client = HttpClient.newBuilder()
            .cookieHandler(cookieManager)
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .version(HttpClient.Version.HTTP_1_1)
            .build();

        try {
            // 1. GET / – inicjalizacja ciasteczek sesji (JSESSIONID, urm_*)
            var initUrl = lib.getBaseUrl() + "/";
            var initReq = HttpRequest.newBuilder(URI.create(initUrl))
                .GET()
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,*/*")
                .timeout(Duration.ofSeconds(10))
                .build();
            client.send(initReq, HttpResponse.BodyHandlers.discarding());
            log.debug("Initialized session cookies for {}", lib.name());

            // 2. POST /primaws/suprimaLogin
            var loginUrl = lib.getBaseUrl() + "/primaws/suprimaLogin";
            var body = formEncode(Map.of(
                "authenticationProfile", "Alma",
                "username",              username,
                "password",              password,
                "view",                  lib.getVid(),
                "institution",           lib.getInstCode(),
                "targetUrl",             ""
            ));
            var referer = lib.getBaseUrl() + "/nde/login?vid=" + lib.getVid() + "&lang=pl";

            var loginReqBuilder = HttpRequest.newBuilder(URI.create(loginUrl))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                .header("Accept",       "application/json, text/plain, */*")
                .header("Accept-Language", "pl-PL,pl;q=0.9")
                .header("User-Agent",   USER_AGENT)
                .header("Origin",       lib.getBaseUrl())
                .header("Referer",      referer)
                .header("Sec-Fetch-Site", "same-origin")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Dest", "empty")
                .timeout(Duration.ofSeconds(15));
            if (lib.isNde()) {
                loginReqBuilder.header("is-nde", "true");
            }

            var loginResp = client.send(loginReqBuilder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (loginResp.statusCode() < 200 || loginResp.statusCode() >= 300) {
                throw new PrimoException("Login HTTP " + loginResp.statusCode() + " for " + lib.name());
            }

            JsonNode json = objectMapper.readTree(loginResp.body());
            String rawJwt = json.path("jwtData").asText(null);
            if (rawJwt == null || rawJwt.isBlank()) {
                throw new PrimoException("Missing jwtData in login response for " + lib.name());
            }
            // Serwer opakowuje token cudzysłowami – stripujemy
            String jwt = rawJwt.replace("\"", "");
            log.debug("Login successful for {} ({})", username, lib.name());
            return new PrimoSession(lib, jwt, client);

        } catch (PrimoException e) {
            throw e;
        } catch (Exception e) {
            throw new PrimoException("Login failed for " + lib.name() + ": " + e.getMessage(), e);
        }
    }

    // --- Helpers ---

    private static String formEncode(Map<String, String> params) {
        return params.entrySet().stream()
            .map(e -> URLEncoder.encode(e.getKey(),   StandardCharsets.UTF_8)
                + "=" +
                URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
    }
}
