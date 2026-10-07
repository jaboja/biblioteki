package pl.jaboja.biblioteki.config;

import java.time.Duration;

public class HttpConfig {

    public static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                    "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.4 Safari/605.1.15";

    public static final Duration TIMEOUT = Duration.ofSeconds(10);

}
