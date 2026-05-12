package it.thcs.fse.fsersaservice.infrastructure.gateway.client;

import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.request.GatewayDocumentValidationRequest;
import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.response.GatewayTransactionInspectResponseDto;
import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.response.GatewayValidationResponseDto;
import it.thcs.fse.fsersaservice.infrastructure.gateway.jwt.GatewayStatusJwtContext;
import reactor.core.publisher.Mono;

public interface FseGatewayClient {

    /**
     * POST /v1/documents/validation
     * Valida il documento CDA2 in modo sincrono. Non pubblica.
     */
    Mono<GatewayValidationResponseDto> validateDocument(GatewayDocumentValidationRequest request);

    /**
     * POST /v1/documents/validate-and-create
     * Valida e pubblica contestualmente il documento CDA2.
     * La validazione è sincrona; l'invio a INI/EDS è asincrono.
     * Il request body include i metadati XDS obbligatori.
     */
    Mono<GatewayValidationResponseDto> validateAndCreate(GatewayDocumentValidationRequest request);

    /**
     * GET /v1/status/{workflowInstanceId}
     * Recupera lo stato di una transazione tramite workflowInstanceId.
     * Richiede entrambi i token JWT (Bearer + FSE-JWT-Signature).
     */
    Mono<GatewayTransactionInspectResponseDto> getStatusByWorkflowInstanceId(
            String workflowInstanceId,
            GatewayStatusJwtContext context
    );

    /**
     * GET /v1/status/search/{traceId}
     * Recupera lo stato di una transazione tramite traceId.
     * Usa solo il token Bearer.
     */
    Mono<GatewayTransactionInspectResponseDto> getStatusByTraceId(String traceId);
}