package it.thcs.fse.fsersaservice.infrastructure.gateway.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GatewayValidationResponseDto {

    @JsonProperty("traceID")
    private String traceId;

    @JsonProperty("spanID")
    private String spanId;

    private String workflowInstanceId;

    private String warning;
}