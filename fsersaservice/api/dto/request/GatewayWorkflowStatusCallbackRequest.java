package it.thcs.fse.fsersaservice.api.dto.callback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Payload ricevuto dal Gateway FSE 2.0 sulla callback
 * POST /v1/workflow/status
 *
 * Il Gateway invia questa notifica al sistema produttore dopo che
 * le operazioni asincrone (pubblicazione INI/EDS) sono completate.
 * La lista {@code events} contiene tutti gli eventi della transazione
 * in ordine cronologico.
 *
 * Esempio payload success:
 * <pre>
 * {
 *   "workflowInstanceId": "2.16.840.1.113883...^^^^urn:ihe:iti:xdw:2013:workflowInstanceId",
 *   "events": [
 *     { "eventType": "VALIDATION",   "eventStatus": "SUCCESS", ... },
 *     { "eventType": "PUBLICATION",  "eventStatus": "SUCCESS", ... },
 *     { "eventType": "SEND_TO_INI",  "eventStatus": "SUCCESS", ... },
 *     { "eventType": "SEND_TO_UAR",  "eventStatus": "SUCCESS", ... },
 *     { "eventType": "UAR_FINAL_STATUS", "eventStatus": "SUCCESS", ... }
 *   ]
 * }
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GatewayWorkflowStatusCallbackRequest {

    private String workflowInstanceId;
    private List<GatewayWorkflowStatusEventDto> events = new ArrayList<>();
}