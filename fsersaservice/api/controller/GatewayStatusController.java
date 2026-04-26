package it.thcs.fse.fsersaservice.api.controller;

import it.thcs.fse.fsersaservice.api.dto.response.GatewayTransactionStatusResponse;
import it.thcs.fse.fsersaservice.application.service.GatewayTransactionStatusApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/gateway")
public class GatewayStatusController {

    private final GatewayTransactionStatusApplicationService applicationService;

    public GatewayStatusController(GatewayTransactionStatusApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("/status/{workflowInstanceId}")
    public Mono<GatewayTransactionStatusResponse> getByWorkflowInstanceId(
            @PathVariable String workflowInstanceId
    ) {
        return applicationService.getByWorkflowInstanceId(workflowInstanceId);
    }

    @GetMapping("/status/search/{traceId}")
    public Mono<GatewayTransactionStatusResponse> getByTraceId(
            @PathVariable String traceId
    ) {
        return applicationService.getByTraceId(traceId);
    }
}