package it.thcs.fse.fsersaservice.application.service;

import it.thcs.fse.fsersaservice.api.dto.response.GatewayTransactionStatusEventResponse;
import it.thcs.fse.fsersaservice.api.dto.response.GatewayTransactionStatusResponse;
import it.thcs.fse.fsersaservice.application.port.out.GatewayStatusContextPort;
import it.thcs.fse.fsersaservice.infrastructure.gateway.client.FseGatewayClient;
import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.response.GatewayTransactionInspectResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Service
public class GatewayTransactionStatusApplicationService {

    private final FseGatewayClient fseGatewayClient;
    private final GatewayStatusContextPort gatewayStatusContextPort;

    public GatewayTransactionStatusApplicationService(
            FseGatewayClient fseGatewayClient,
            GatewayStatusContextPort gatewayStatusContextPort
    ) {
        this.fseGatewayClient = fseGatewayClient;
        this.gatewayStatusContextPort = gatewayStatusContextPort;
    }

    public Mono<GatewayTransactionStatusResponse> getByWorkflowInstanceId(String workflowInstanceId) {
        String normalizedWorkflowInstanceId = normalizeRequired(workflowInstanceId, "workflowInstanceId");

        return gatewayStatusContextPort.findByWorkflowInstanceId(normalizedWorkflowInstanceId)
                .flatMap(context -> fseGatewayClient.getStatusByWorkflowInstanceId(normalizedWorkflowInstanceId, context))
                .map(this::toResponse);
    }

    public Mono<GatewayTransactionStatusResponse> getByTraceId(String traceId) {
        String normalizedTraceId = normalizeRequired(traceId, "traceId");

        return fseGatewayClient.getStatusByTraceId(normalizedTraceId)
                .map(this::toResponse);
    }

    private String normalizeRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " mancante");
        }
        return value.trim();
    }

    private GatewayTransactionStatusResponse toResponse(GatewayTransactionInspectResponseDto dto) {
        return GatewayTransactionStatusResponse.builder()
                .traceId(dto.getTraceId())
                .spanId(dto.getSpanId())
                .transactionData(mapTransactionData(dto))
                .build();
    }

    private List<GatewayTransactionStatusEventResponse> mapTransactionData(GatewayTransactionInspectResponseDto dto) {
        if (dto.getTransactionData() == null) {
            return Collections.emptyList();
        }

        return dto.getTransactionData().stream()
                .map(item -> GatewayTransactionStatusEventResponse.builder()
                        .eventType(item.getEventType())
                        .eventDate(item.getEventDate())
                        .eventStatus(item.getEventStatus())
                        .message(item.getMessage())
                        .identificativoDocumento(item.getIdentificativoDocumento())
                        .subject(item.getSubject())
                        .subjectRole(item.getSubjectRole())
                        .tipoAttivita(item.getTipoAttivita())
                        .organizzazione(item.getOrganizzazione())
                        .workflowInstanceId(item.getWorkflowInstanceId())
                        .traceId(item.getTraceId())
                        .issuer(item.getIssuer())
                        .expiringDate(item.getExpiringDate())
                        .build())
                .toList();
    }
}
