package it.thcs.fse.fsersaservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Configurazione sicurezza del microservizio fse-rsa-service.
 *
 * Regole esplicite su tutti i path:
 *
 * APERTI (senza autenticazione):
 *   /actuator/health/**   — liveness/readiness probe
 *   /actuator/info        — info applicazione
 *
 * PROTETTI con X-API-Key:
 *   /api/**               — tutti gli endpoint applicativi
 *
 * NEGATI (403) per tutto il resto:
 *   /actuator/metrics, /actuator/env, ecc. — non esposti
 *   qualsiasi altro path non classificato
 *
 * Il confronto API key è timing-safe tramite MessageDigest.isEqual.
 */
@Configuration
public class SecurityConfig {

    private static final String API_KEY_HEADER = "X-API-Key";

    private static final String BODY_UNAUTHORIZED =
            "{\"error\":\"Unauthorized\",\"message\":\"API key mancante o non valida\"}";

    private static final String BODY_FORBIDDEN =
            "{\"error\":\"Forbidden\",\"message\":\"Endpoint non disponibile\"}";

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                // La protezione applicativa è demandata al WebFilter custom.
                // Qui manteniamo permitAll e lasciamo che il filtro gestisca
                // accesso consentito, 401 e 403 in base al path richiesto.
                .authorizeExchange(exchange -> exchange.anyExchange().permitAll())
                .build();
    }

    @Bean
    @Order(1)
    public WebFilter apiKeyWebFilter(
            @Value("${fse.security.api-key}") String expectedApiKey
    ) {
        return new ApiKeyWebFilter(expectedApiKey);
    }

    private static class ApiKeyWebFilter implements WebFilter {

        private final byte[] expectedKeyBytes;

        ApiKeyWebFilter(String expectedApiKey) {
            if (expectedApiKey == null || expectedApiKey.isBlank()) {
                throw new IllegalStateException(
                        "fse.security.api-key non configurata — il microservizio non può avviarsi senza una API key");
            }
            this.expectedKeyBytes = expectedApiKey.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
            String path = exchange.getRequest().getPath().value();

            // 1. Actuator health e info: aperti esplicitamente
            if (isActuatorExempt(path)) {
                return chain.filter(exchange);
            }

            // 2. Endpoint applicativi: richiedono API key
            if (path.startsWith("/api/")) {
                String requestKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);
                if (!isValidKey(requestKey)) {
                    return writeResponse(exchange, HttpStatus.UNAUTHORIZED, BODY_UNAUTHORIZED);
                }
                return chain.filter(exchange);
            }

            // 3. Tutto il resto (altri /actuator/**, path sconosciuti): 403
            // Questo include /actuator/metrics, /actuator/env, /actuator/beans, ecc.
            return writeResponse(exchange, HttpStatus.FORBIDDEN, BODY_FORBIDDEN);
        }

        private boolean isActuatorExempt(String path) {
            return path.startsWith("/actuator/health")
                    || path.equals("/actuator/info");
        }

        /**
         * Confronto timing-safe: MessageDigest.isEqual garantisce tempo costante
         * indipendentemente dal punto di divergenza tra le due chiavi,
         * prevenendo timing attack basati sulla misurazione del tempo di risposta.
         */
        private boolean isValidKey(String requestKey) {
            if (requestKey == null || requestKey.isBlank()) {
                return false;
            }
            byte[] requestKeyBytes = requestKey.getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(expectedKeyBytes, requestKeyBytes);
        }

        private Mono<Void> writeResponse(ServerWebExchange exchange,
                                         HttpStatus status,
                                         String body) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(status);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        }
    }
}