package it.thcs.fse.fsersaservice.application.port.out;

import it.thcs.fse.fsersaservice.infrastructure.gateway.jwt.GatewayStatusJwtContext;
import reactor.core.publisher.Mono;

public interface GatewayStatusContextPort {

    Mono<GatewayStatusJwtContext> findByWorkflowInstanceId(String workflowInstanceId);

    Mono<GatewayStatusJwtContext> findByTraceId(String traceId);
}