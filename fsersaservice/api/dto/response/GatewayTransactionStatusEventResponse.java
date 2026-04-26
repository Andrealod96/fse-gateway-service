package it.thcs.fse.fsersaservice.api.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GatewayTransactionStatusEventResponse {

    private final String eventType;
    private final String eventDate;
    private final String eventStatus;
    private final String message;
    private final String identificativoDocumento;
    private final String subject;
    private final String subjectRole;
    private final String tipoAttivita;
    private final String organizzazione;
    private final String workflowInstanceId;
    private final String traceId;
    private final String issuer;
    private final String expiringDate;
}