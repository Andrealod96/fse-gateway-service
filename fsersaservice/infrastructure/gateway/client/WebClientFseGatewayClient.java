package it.thcs.fse.fsersaservice.infrastructure.gateway.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.thcs.fse.fsersaservice.config.properties.GatewayProperties;
import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.request.GatewayDocumentValidationRequest;
import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.response.GatewayProblemDetailsDto;
import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.response.GatewayTransactionInspectResponseDto;
import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.response.GatewayValidationResponseDto;
import it.thcs.fse.fsersaservice.infrastructure.gateway.exception.GatewayClientException;
import it.thcs.fse.fsersaservice.infrastructure.gateway.exception.GatewayResponseParsingException;
import it.thcs.fse.fsersaservice.infrastructure.gateway.exception.GatewayTransportException;
import it.thcs.fse.fsersaservice.infrastructure.gateway.jwt.GatewayJwtHeaders;
import it.thcs.fse.fsersaservice.infrastructure.gateway.jwt.JwtGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;

@Component
public class WebClientFseGatewayClient implements FseGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(WebClientFseGatewayClient.class);
    private static final String HEADER_FSE_JWT_SIGNATURE = "FSE-JWT-Signature";

    private final WebClient fseGatewayWebClient;
    private final GatewayProperties gatewayProperties;
    private final JwtGenerator jwtGenerator;
    private final ObjectMapper objectMapper;

    public WebClientFseGatewayClient(
            WebClient fseGatewayWebClient,
            GatewayProperties gatewayProperties,
            JwtGenerator jwtGenerator,
            ObjectMapper objectMapper
    ) {
        this.fseGatewayWebClient = fseGatewayWebClient;
        this.gatewayProperties = gatewayProperties;
        this.jwtGenerator = jwtGenerator;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<GatewayValidationResponseDto> validateDocument(GatewayDocumentValidationRequest request) {
        return jwtGenerator.generateGatewayHeaders(request)
                .flatMap(headers -> doValidateDocument(request, headers))
                .timeout(Duration.ofSeconds(gatewayProperties.getResponseTimeoutSeconds()))
                .onErrorMap(this::shouldMapAsTransportError, this::toGatewayTransportException);
    }

    @Override
    public Mono<GatewayTransactionInspectResponseDto> getStatusByWorkflowInstanceId(String workflowInstanceId) {
        if (!StringUtils.hasText(workflowInstanceId)) {
            return Mono.error(new GatewayResponseParsingException(
                    "workflowInstanceId mancante per interrogazione stato",
                    null,
                    null
            ));
        }

        return jwtGenerator.generateGatewayHeadersForWorkflowStatus()
                .flatMap(headers -> doGetStatusByWorkflowInstanceId(workflowInstanceId.trim(), headers))
                .timeout(Duration.ofSeconds(gatewayProperties.getResponseTimeoutSeconds()))
                .onErrorMap(this::shouldMapAsTransportError, this::toGatewayTransportException);
    }

    @Override
    public Mono<GatewayTransactionInspectResponseDto> getStatusByTraceId(String traceId) {
        if (!StringUtils.hasText(traceId)) {
            return Mono.error(new GatewayResponseParsingException(
                    "traceId mancante per interrogazione stato",
                    null,
                    null
            ));
        }

        return jwtGenerator.generateAuthenticationBearerToken()
                .flatMap(authToken -> doGetStatusByTraceId(traceId.trim(), authToken))
                .timeout(Duration.ofSeconds(gatewayProperties.getResponseTimeoutSeconds()))
                .onErrorMap(this::shouldMapAsTransportError, this::toGatewayTransportException);
    }

    private Mono<GatewayValidationResponseDto> doValidateDocument(
            GatewayDocumentValidationRequest request,
            GatewayJwtHeaders jwtHeaders
    ) {
        MultipartBodyBuilder multipartBuilder = new MultipartBodyBuilder();
        multipartBuilder.part(
                "requestBody",
                serializeRequestBody(request),
                MediaType.APPLICATION_JSON
        );
        multipartBuilder.part(
                "file",
                pdfResource(request),
                MediaType.APPLICATION_PDF
        );

        return fseGatewayWebClient.post()
                .uri(gatewayProperties.getValidationPath())
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(jwtHeaders.authorizationBearerToken()))
                .header(HEADER_FSE_JWT_SIGNATURE, jwtHeaders.fseJwtSignatureToken())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(multipartBuilder.build())
                .exchangeToMono(response -> handleTypedResponse(
                        response,
                        gatewayProperties.getValidationPath(),
                        this::parseValidationSuccessResponse
                ));
    }

    private Mono<GatewayTransactionInspectResponseDto> doGetStatusByWorkflowInstanceId(
            String workflowInstanceId,
            GatewayJwtHeaders jwtHeaders
    ) {
        return fseGatewayWebClient.get()
                .uri(gatewayProperties.getStatusByWorkflowPath(), workflowInstanceId)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(jwtHeaders.authorizationBearerToken()))
                .header(HEADER_FSE_JWT_SIGNATURE, jwtHeaders.fseJwtSignatureToken())
                .exchangeToMono(response -> handleTypedResponse(
                        response,
                        gatewayProperties.getStatusByWorkflowPath(),
                        this::parseTransactionInspectSuccessResponse
                ));
    }

    private Mono<GatewayTransactionInspectResponseDto> doGetStatusByTraceId(
            String traceId,
            String authenticationBearerToken
    ) {
        return fseGatewayWebClient.get()
                .uri(gatewayProperties.getStatusByTracePath(), traceId)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(authenticationBearerToken))
                .exchangeToMono(response -> handleTypedResponse(
                        response,
                        gatewayProperties.getStatusByTracePath(),
                        this::parseTransactionInspectSuccessResponse
                ));
    }

    private <T> Mono<T> handleTypedResponse(
            ClientResponse response,
            String endpointPath,
            BiFunction<HttpStatusCode, String, T> successParser
    ) {
        HttpStatusCode httpStatus = response.statusCode();

        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(rawBody -> {
                    if (httpStatus.is2xxSuccessful()) {
                        return Mono.fromCallable(() -> successParser.apply(httpStatus, rawBody));
                    }
                    return Mono.error(toGatewayClientException(endpointPath, httpStatus, rawBody));
                });
    }

    private GatewayValidationResponseDto parseValidationSuccessResponse(HttpStatusCode httpStatus, String rawBody) {
        GatewayValidationResponseDto dto = parseRequiredBody(httpStatus, rawBody, GatewayValidationResponseDto.class);

        if (!StringUtils.hasText(dto.getTraceId())) {
            throw new GatewayResponseParsingException(
                    "Gateway FSE 2.0 ha restituito una risposta 2xx senza traceID",
                    httpStatus.value(),
                    rawBody
            );
        }

        if (!StringUtils.hasText(dto.getWorkflowInstanceId())) {
            throw new GatewayResponseParsingException(
                    "Gateway FSE 2.0 ha restituito una risposta 2xx senza workflowInstanceId",
                    httpStatus.value(),
                    rawBody
            );
        }

        return dto;
    }

    private GatewayTransactionInspectResponseDto parseTransactionInspectSuccessResponse(
            HttpStatusCode httpStatus,
            String rawBody
    ) {
        GatewayTransactionInspectResponseDto dto =
                parseRequiredBody(httpStatus, rawBody, GatewayTransactionInspectResponseDto.class);

        if (!StringUtils.hasText(dto.getTraceId())) {
            throw new GatewayResponseParsingException(
                    "Gateway FSE 2.0 ha restituito una risposta status 2xx senza traceID",
                    httpStatus.value(),
                    rawBody
            );
        }

        if (dto.getTransactionData() == null) {
            throw new GatewayResponseParsingException(
                    "Gateway FSE 2.0 ha restituito una risposta status 2xx senza transactionData",
                    httpStatus.value(),
                    rawBody
            );
        }

        return dto;
    }

    private <T> T parseRequiredBody(HttpStatusCode httpStatus, String rawBody, Class<T> responseType) {
        if (!StringUtils.hasText(rawBody)) {
            throw new GatewayResponseParsingException(
                    "Gateway FSE 2.0 ha restituito una risposta 2xx con body vuoto",
                    httpStatus.value(),
                    null
            );
        }

        final T dto;
        try {
            dto = objectMapper.readValue(rawBody, responseType);
        } catch (Exception ex) {
            throw new GatewayResponseParsingException(
                    "Gateway FSE 2.0 ha restituito una risposta 2xx non parsabile",
                    httpStatus.value(),
                    rawBody,
                    ex
            );
        }

        if (dto == null) {
            throw new GatewayResponseParsingException(
                    "Gateway FSE 2.0 ha restituito una risposta 2xx con DTO nullo",
                    httpStatus.value(),
                    rawBody
            );
        }

        return dto;
    }

    private GatewayClientException toGatewayClientException(
            String endpointPath,
            HttpStatusCode httpStatus,
            String rawBody
    ) {
        GatewayProblemDetailsDto problem = tryParseProblem(rawBody);

        String traceId = problem != null ? problem.getTraceId() : null;
        String workflowInstanceId = problem != null ? problem.getWorkflowInstanceId() : null;
        String title = problem != null ? problem.getTitle() : null;
        String detail = problem != null ? problem.getDetail() : null;

        String message = "Errore chiamata Gateway "
                + endpointPath
                + ". HTTP "
                + httpStatus.value()
                + " - "
                + (StringUtils.hasText(rawBody) ? rawBody : "<body-vuoto>");

        return new GatewayClientException(
                message,
                httpStatus.value(),
                traceId,
                workflowInstanceId,
                title,
                detail,
                rawBody
        );
    }

    private GatewayProblemDetailsDto tryParseProblem(String rawBody) {
        if (!StringUtils.hasText(rawBody)) {
            return null;
        }

        try {
            return objectMapper.readValue(rawBody, GatewayProblemDetailsDto.class);
        } catch (Exception ex) {
            log.warn("Impossibile parsare il problem+json del Gateway: {}", ex.getMessage());
            return null;
        }
    }

    private String serializeRequestBody(GatewayDocumentValidationRequest request) {
        if (request == null || request.getRequestBody() == null) {
            throw new GatewayResponseParsingException(
                    "RequestBody Gateway assente",
                    null,
                    null
            );
        }

        try {
            return objectMapper.writeValueAsString(request.getRequestBody());
        } catch (JsonProcessingException ex) {
            throw new GatewayResponseParsingException(
                    "Impossibile serializzare il requestBody della richiesta Gateway",
                    null,
                    null,
                    ex
            );
        }
    }

    private ByteArrayResource pdfResource(GatewayDocumentValidationRequest request) {
        if (request == null || request.getFileBytes() == null || request.getFileBytes().length == 0) {
            throw new GatewayResponseParsingException(
                    "File PDF assente o vuoto nella richiesta Gateway",
                    null,
                    null
            );
        }

        return new ByteArrayResource(request.getFileBytes()) {
            @Override
            public String getFilename() {
                return request.getFileName();
            }
        };
    }

    private boolean shouldMapAsTransportError(Throwable ex) {
        if (ex instanceof GatewayClientException) {
            return false;
        }
        if (ex instanceof GatewayResponseParsingException) {
            return false;
        }
        if (ex instanceof GatewayTransportException) {
            return false;
        }

        Throwable root = rootCause(ex);

        return ex instanceof WebClientRequestException
                || root instanceof WebClientRequestException
                || root instanceof TimeoutException
                || root instanceof SocketTimeoutException
                || root instanceof ConnectException
                || root instanceof UnknownHostException
                || root instanceof SSLException;
    }

    private GatewayTransportException toGatewayTransportException(Throwable ex) {
        String message = "Errore di trasporto verso Gateway FSE 2.0: " + ex.getMessage();
        return new GatewayTransportException(message, ex);
    }

    private Throwable rootCause(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}