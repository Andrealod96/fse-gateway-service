package it.thcs.fse.fsersaservice.api.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GatewayTransactionStatusResponse {

    private final String traceId;
    private final String spanId;
    private final List<GatewayTransactionStatusEventResponse> transactionData;
}