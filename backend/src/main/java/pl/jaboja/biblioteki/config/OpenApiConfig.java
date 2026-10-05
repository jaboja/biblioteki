package pl.jaboja.biblioteki.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Konfiguracja OpenAPI dla dokumentacji REST API.
 * Dokumentacja dostępna pod:
 * - JSON: /api-docs
 * - Swagger UI: /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Biblioteki API")
                .version("0.0.1")
                .description("API do zarządzania wypożyczeniami z różnych bibliotek. Umożliwia monitorowanie stanów wypożyczeń, zarządzanie kontami dostępu oraz odświeżanie danych.")
                .contact(new Contact()
                    .name("Biblioteki")
                    .email("kontakt@example.com"))
                .license(new License()
                    .name("MIT")
                    .url("https://opensource.org/licenses/MIT")));
    }
}