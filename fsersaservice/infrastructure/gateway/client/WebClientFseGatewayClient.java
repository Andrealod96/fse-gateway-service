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
import it.thcs.fse.fsersaservice.infrastructure.gateway.jwt.GatewayStatusJwtContext;
import it.thcs.fse.fsersaservice.infrastructure.gateway.jwt.JwtGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
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
            @Qualifier("gatewayWebClient") WebClient fseGatewayWebClient,
            GatewayProperties gatewayProperties,
            JwtGenerator jwtGenerator,
            ObjectMapper objectMapper
    ) {
        this.fseGatewayWebClient = fseGatewayWebClient;
        this.gatewayProperties = gatewayProperties;
        this.jwtGenerator = jwtGenerator;
        this.objectMapper = objectMapper;
    }

    // ─── Validazione ──────────────────────────────────────────────────────────

    @Override
    public Mono<GatewayValidationResponseDto> validateDocument(GatewayDocumentValidationRequest request) {
        return jwtGenerator.generateGatewayHeaders(request)
                .flatMap(headers -> doPostDocument(
                        request, headers,
                        gatewayProperties.getValidationPath(),
                        "validazione documento"
                ))
                .timeout(Duration.ofSeconds(gatewayProperties.getResponseTimeoutSeconds()))
                .onErrorMap(this::shouldMapAsTransportError, this::toGatewayTransportException);
    }

    // ─── Validate-and-create ──────────────────────────────────────────────────

    @Override
    public Mono<GatewayValidationResponseDto> validateAndCreate(GatewayDocumentValidationRequest request) {
        return jwtGenerator.generateGatewayHeaders(request)
                .flatMap(headers -> doPostDocument(
                        request, headers,
                        gatewayProperties.getValidateAndCreatePath(),
                        "validate-and-create"
                ))
                .timeout(Duration.ofSeconds(gatewayProperties.getResponseTimeoutSeconds()))
                .onErrorMap(this::shouldMapAsTransportError, this::toGatewayTransportException);
    }

    // ─── Status per workflowInstanceId ────────────────────────────────────────

    @Override
    public Mono<GatewayTransactionInspectResponseDto> getStatusByWorkflowInstanceId(
            String workflowInstanceId,
            GatewayStatusJwtContext context
    ) {
        if (!StringUtils.hasText(workflowInstanceId)) {
            return Mono.error(new GatewayResponseParsingException(
                    "workflowInstanceId mancante per interrogazione stato", null, null));
        }
        if (context == null) {
            return Mono.error(new GatewayResponseParsingException(
                    "GatewayStatusJwtContext nullo per interrogazione stato workflowInstanceId", null, null));
        }

        return jwtGenerator.generateGatewayHeadersForWorkflowStatus(context)
                .flatMap(headers -> doGetStatusByWorkflowInstanceId(workflowInstanceId.trim(), headers))
                .timeout(Duration.ofSeconds(gatewayProperties.getResponseTimeoutSeconds()))
                .onErrorMap(this::shouldMapAsTransportError, this::toGatewayTransportException);
    }

    // ─── Status per traceId ───────────────────────────────────────────────────

    @Override
    public Mono<GatewayTransactionInspectResponseDto> getStatusByTraceId(String traceId) {
        if (!StringUtils.hasText(traceId)) {
            return Mono.error(new GatewayResponseParsingException(
                    "traceId mancante per interrogazione stato", null, null));
        }

        return jwtGenerator.generateAuthenticationBearerToken()
                .flatMap(authToken -> doGetStatusByTraceId(traceId.trim(), authToken))
                .timeout(Duration.ofSeconds(gatewayProperties.getResponseTimeoutSeconds()))
                .onErrorMap(this::shouldMapAsTransportError, this::toGatewayTransportException);
    }

    // ─── HTTP helpers ─────────────────────────────────────────────────────────

    /**
     * Metodo condiviso per validation e validate-and-create — stessa struttura multipart,
     * cambia solo il path e il contenuto del requestBody (già impostato nel request).
     */
    private Mono<GatewayValidationResponseDto> doPostDocument(
            GatewayDocumentValidationRequest request,
            GatewayJwtHeaders jwtHeaders,
            String path,
            String operationLabel
    ) {
        validateGatewayJwtHeaders(jwtHeaders, operationLabel);

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("requestBody", serializeRequestBody(request), MediaType.APPLICATION_JSON);
        builder.part("file", pdfResource(request), MediaType.APPLICATION_PDF);

        log.info("Invio richiesta Gateway: path={}, operazione={}, paziente={}",
                path, operationLabel, maskCf(request.getPatientFiscalCode()));

        return fseGatewayWebClient.post()
                .uri(path)
                .accept(MediaType.APPLICATION_JSON)
                .headers(h -> {
                    h.setBearerAuth(jwtHeaders.authorizationBearerToken());
                    h.set(HEADER_FSE_JWT_SIGNATURE, jwtHeaders.fseJwtSignatureToken());
                })
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(builder.build())
                .exchangeToMono(response -> handleTypedResponse(
                        response, path, this::parseValidationSuccessResponse));
    }

    private Mono<GatewayTransactionInspectResponseDto> doGetStatusByWorkflowInstanceId(
            String workflowInstanceId,
            GatewayJwtHeaders jwtHeaders
    ) {
        validateGatewayJwtHeaders(jwtHeaders, "workflow status");

        log.info("Interrogazione stato workflow: workflowInstanceId={}", workflowInstanceId);

        return fseGatewayWebClient.get()
                .uri(gatewayProperties.getStatusByWorkflowPath(), workflowInstanceId)
                .accept(MediaType.APPLICATION_JSON)
                .headers(h -> {
                    h.setBearerAuth(jwtHeaders.authorizationBearerToken());
                    h.set(HEADER_FSE_JWT_SIGNATURE, jwtHeaders.fseJwtSignatureToken());
                })
                .exchangeToMono(response -> handleTypedResponse(
                        response,
                        gatewayProperties.getStatusByWorkflowPath(),
                        this::parseTransactionInspectSuccessResponse));
    }

    private Mono<GatewayTransactionInspectResponseDto> doGetStatusByTraceId(
            String traceId,
            String bearerToken
    ) {
        if (!StringUtils.hasText(bearerToken)) {
            return Mono.error(new GatewayResponseParsingException(
                    "Authorization Bearer JWT assente per interrogazione stato traceId", null, null));
        }

        log.info("Interrogazione stato traceId: traceId={}", traceId);

        return fseGatewayWebClient.get()
                .uri(gatewayProperties.getStatusByTracePath(), traceId)
                .accept(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(bearerToken))
                .exchangeToMono(response -> handleTypedResponse(
                        response,
                        gatewayProperties.getStatusByTracePath(),
                        this::parseTransactionInspectSuccessResponse));
    }

    // ─── Response parsing ─────────────────────────────────────────────────────

    private <T> Mono<T> handleTypedResponse(
            ClientResponse response,
            String endpointPath,
            BiFunction<HttpStatusCode, String, T> successParser
    ) {
        HttpStatusCode status = response.statusCode();
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> {
                    if (status.is2xxSuccessful()) {
                        return Mono.fromCallable(() -> successParser.apply(status, body));
                    }
                    return Mono.error(toGatewayClientException(endpointPath, status, body));
                });
    }

    private GatewayValidationResponseDto parseValidationSuccessResponse(
            HttpStatusCode status, String body) {
        GatewayValidationResponseDto dto = parseRequiredBody(status, body, GatewayValidationResponseDto.class);

        if (!StringUtils.hasText(dto.getTraceId())) {
            throw new GatewayResponseParsingException(
                    "Gateway FSE 2.0 ha restituito risposta 2xx senza traceID", status.value(), body);
        }
        if (!StringUtils.hasText(dto.getWorkflowInstanceId())) {
            throw new GatewayResponseParsingException(
                    "Gateway FSE 2.0 ha restituito risposta 2xx senza workflowInstanceId", status.value(), body);
        }
        return dto;
    }

    private GatewayTransactionInspectResponseDto parseTransactionInspectSuccessResponse(
            HttpStatusCode status, String body) {
        GatewayTransactionInspectResponseDto dto =
                parseRequiredBody(status, body, GatewayTransactionInspectResponseDto.class);

        if (!StringUtils.hasText(dto.getTraceId())) {
            throw new GatewayResponseParsingException(
                    "Gateway FSE 2.0 ha restituito risposta status 2xx senza traceID", status.value(), body);
        }
        return dto;
    }

    private <T> T parseRequiredBody(HttpStatusCode status, String body, Class<T> type) {
        if (!StringUtils.hasText(body)) {
            throw new GatewayResponseParsingException(
                    "Gateway FSE 2.0 ha restituito body vuoto", status.value(), null);
        }
        try {
            T dto = objectMapper.readValue(body, type);
            if (dto == null) {
                throw new GatewayResponseParsingException(
                        "Gateway FSE 2.0 ha restituito DTO nullo", status.value(), body);
            }
            return dto;
        } catch (Exception ex) {
            throw new GatewayResponseParsingException(
                    "Gateway FSE 2.0 ha restituito body non parsabile", status.value(), body, ex);
        }
    }

    private GatewayClientException toGatewayClientException(
            String path, HttpStatusCode status, String body) {
        GatewayProblemDetailsDto problem = tryParseProblem(body);
        String traceId    = problem != null ? problem.getTraceId()            : null;
        String workflowId = problem != null ? problem.getWorkflowInstanceId() : null;
        String title      = problem != null ? problem.getTitle()              : null;
        String detail     = problem != null ? problem.getDetail()             : null;
        String message = "Errore Gateway " + path + ". HTTP " + status.value()
                + " — " + (StringUtils.hasText(body) ? body : "(no body)");

        log.error("Gateway FSE 2.0 errore: path={}, status={}, traceId={}, detail={}",
                path, status.value(), traceId, detail);

        return new GatewayClientException(message, status.value(), traceId, workflowId, title, detail, body);
    }

    private GatewayProblemDetailsDto tryParseProblem(String body) {
        if (!StringUtils.hasText(body)) return null;
        try {
            return objectMapper.readValue(body, GatewayProblemDetailsDto.class);
        } catch (Exception ex) {
            return null;
        }
    }

    // ─── Utility ─────────────────────────────────────────────────────────────

    private String serializeRequestBody(GatewayDocumentValidationRequest request) {
        try {
            return objectMapper.writeValueAsString(request.getRequestBody());
        } catch (JsonProcessingException ex) {
            throw new GatewayResponseParsingException(
                    "Impossibile serializzare requestBody Gateway", null, null, ex);
        }
    }

    private ByteArrayResource pdfResource(GatewayDocumentValidationRequest request) {
        if (request == null || request.getFileBytes() == null || request.getFileBytes().length == 0) {
            throw new GatewayResponseParsingException(
                    "File PDF assente o vuoto nella richiesta Gateway", null, null);
        }
        return new ByteArrayResource(request.getFileBytes()) {
            @Override
            public String getFilename() {
                return request.getFileName();
            }
        };
    }

    private void validateGatewayJwtHeaders(GatewayJwtHeaders h, String op) {
        if (h == null || !StringUtils.hasText(h.authorizationBearerToken())) {
            throw new GatewayResponseParsingException(
                    "Authorization Bearer JWT assente per operazione: " + op, null, null);
        }
        if (!StringUtils.hasText(h.fseJwtSignatureToken())) {
            throw new GatewayResponseParsingException(
                    "FSE-JWT-Signature assente per operazione: " + op, null, null);
        }
    }

    private boolean shouldMapAsTransportError(Throwable ex) {
        if (ex instanceof GatewayClientException
                || ex instanceof GatewayResponseParsingException
                || ex instanceof GatewayTransportException) return false;
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
        return new GatewayTransportException(
                "Errore di trasporto verso Gateway FSE 2.0: " + ex.getMessage(), ex);
    }

    private Throwable rootCause(Throwable ex) {
        Throwable c = ex;
        while (c.getCause() != null && c.getCause() != c) c = c.getCause();
        return c;
    }

    private String maskCf(String cf) {
        if (cf == null || cf.length() < 6) return "******";
        return cf.substring(0, 6) + "**********";
    }
}