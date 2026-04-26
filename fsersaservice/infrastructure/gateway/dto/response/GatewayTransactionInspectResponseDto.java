package it.thcs.fse.fsersaservice.infrastructure.gateway.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GatewayTransactionInspectResponseDto {

    @JsonProperty("traceID")
    private String traceId;

    @JsonProperty("spanID")
    private String spanId;

    private List<GatewayTransactionEventDto> transactionData = new ArrayList<>();
}