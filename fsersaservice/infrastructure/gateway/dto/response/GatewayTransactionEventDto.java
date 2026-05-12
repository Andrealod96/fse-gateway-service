package it.thcs.fse.fsersaservice.infrastructure.gateway.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GatewayTransactionEventDto {

    private String eventType;
    private String eventDate;
    private String eventStatus;
    private String message;

    private String identificativoDocumento;
    private String subject;
    private String subjectRole;
    private String tipoAttivita;
    private String organizzazione;

    private String workflowInstanceId;
    private String traceId;
    private String issuer;
    private String expiringDate;
}