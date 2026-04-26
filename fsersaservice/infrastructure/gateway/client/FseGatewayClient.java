package it.thcs.fse.fsersaservice.infrastructure.gateway.client;

import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.request.GatewayDocumentValidationRequest;
import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.response.GatewayTransactionInspectResponseDto;
import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.response.GatewayValidationResponseDto;
import reactor.core.publisher.Mono;

public interface FseGatewayClient {

    Mono<GatewayValidationResponseDto> validateDocument(GatewayDocumentValidationRequest request);

    Mono<GatewayTransactionInspectResponseDto> getStatusByWorkflowInstanceId(String workflowInstanceId);

    Mono<GatewayTransactionInspectResponseDto> getStatusByTraceId(String traceId);
}