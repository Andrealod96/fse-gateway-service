package it.thcs.fse.fsersaservice.infrastructure.gateway.client;

import it.thcs.fse.fsersaservice.config.properties.GatewayProperties;
import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.request.GatewayDocumentValidationRequest;
import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.response.GatewayValidationResponseDto;
import it.thcs.fse.fsersaservice.infrastructure.gateway.exception.GatewayClientException;
import it.thcs.fse.fsersaservice.infrastructure.gateway.jwt.JwtGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class WebClientFseGatewayClient implements FseGatewayClient {

    private static final String FSE_JWT_SIGNATURE_HEADER = "FSE-JWT-Signature";

    // FIX A: @Qualifier per selezionare esplicitamente il bean "gatewayWebClient"
    // senza @Qualifier Spring può fallire con NoUniqueBeanDefinitionException
    // se nel contesto WebFlux sono presenti più bean di tipo WebClient
    private final WebClient gatewayWebClient;
    private final GatewayProperties gatewayProperties;
    private final JwtGenerator jwtGenerator;

    public WebClientFseGatewayClient(
            @Qualifier("gatewayWebClient") WebClient gatewayWebClient,
            GatewayProperties gatewayProperties,
            JwtGenerator jwtGenerator
    ) {
        this.gatewayWebClient = gatewayWebClient;
        this.gatewayProperties = gatewayProperties;
        this.jwtGenerator = jwtGenerator;
    }

    @Override
    public Mono<GatewayValidationResponseDto> validateDocument(GatewayDocumentValidationRequest request) {
        // FIX B: generateGatewayHeaders è il nome corretto del metodo in JwtGenerator
        // (generateGatewayJwtHeaders non esiste)
        return jwtGenerator.generateGatewayHeaders(request)
                .flatMap(jwtHeaders -> gatewayWebClient.post()
                        .uri(gatewayProperties.getValidationPath())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(headers -> {
                            headers.setBearerAuth(jwtHeaders.authorizationBearerToken());
                            // FIX C: fseJwtSignatureToken() è il nome corretto dell'accessor
                            // nel record GatewayJwtHeaders (signatureToken() non esiste)
                            headers.set(FSE_JWT_SIGNATURE_HEADER, jwtHeaders.fseJwtSignatureToken());
                        })
                        .body(BodyInserters.fromMultipartData(buildMultipartData(request)))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, this::mapGatewayError)
                        .bodyToMono(GatewayValidationResponseDto.class)
                );
    }

    private MultiValueMap<String, HttpEntity<?>> buildMultipartData(GatewayDocumentValidationRequest request) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        builder.part("requestBody", request.getRequestBody())
                .contentType(MediaType.APPLICATION_JSON);

        builder.part("file", request.getFileBytes())
                .filename(request.getFileName())
                .contentType(MediaType.APPLICATION_PDF);

        return builder.build();
    }

    private Mono<? extends Throwable> mapGatewayError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> {
                    int statusCode = response.statusCode().value();
                    String message = "Errore chiamata Gateway /documents/validation. HTTP "
                            + statusCode
                            + (body.isBlank() ? "" : " - " + body);

                    // FIX D: GatewayClientException ha solo il costruttore a 4 parametri
                    // (String message, int httpStatus, String responseBody, String traceId)
                    // il costruttore con solo String non esiste
                    return new GatewayClientException(message, statusCode, body, null);
                });
    }
}