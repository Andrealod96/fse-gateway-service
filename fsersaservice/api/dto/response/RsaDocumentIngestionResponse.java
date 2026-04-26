package it.thcs.fse.fsersaservice.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Risposta del microservizio RSA dopo il flusso di ingestion.
 *
 * I campi {@code traceId} e {@code workflowInstanceId} sono obbligatori
 * per il report-checklist.xlsx di accreditamento FSE 2.0:
 * ogni test case deve riportare timestamp, traceID e workflowInstanceID.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RsaDocumentIngestionResponse {

    /** ID interno del microservizio (UUID generato, diverso dal workflowInstanceId Gateway). */
    private final String requestId;

    /** Stato del workflow al termine dell'elaborazione. */
    private final Status status;

    /** Messaggio human-readable. */
    private final String message;

    /** Timestamp UTC di ricezione della richiesta. */
    private final OffsetDateTime receivedAt;

    // ─── Campi Gateway FSE 2.0 (per accreditamento) ───────────────────────────

    /**
     * traceID restituito dal Gateway FSE 2.0.
     * Obbligatorio nel report-checklist.xlsx per ogni test case.
     * Null se l'elaborazione fallisce prima di raggiungere il Gateway.
     */
    private final String traceId;

    /**
     * workflowInstanceId restituito dal Gateway FSE 2.0.
     * Obbligatorio nel report-checklist.xlsx per ogni test case.
     * Necessario per la successiva chiamata di pubblicazione.
     * Null se l'elaborazione fallisce prima di raggiungere il Gateway.
     */
    private final String workflowInstanceId;

    /**
     * Eventuale warning semantico/terminologico restituito dal Gateway.
     * Il Gateway restituisce warning ma considera comunque la validazione positiva.
     * Null se non ci sono warning.
     */
    private final String gatewayWarning;

    // ─── Enum di stato ────────────────────────────────────────────────────────

    public enum Status {
        /** Richiesta ricevuta e registrata — usato solo se la pipeline non è ancora completata. */
        RECEIVED,
        /** Validazione completata con successo sul Gateway FSE 2.0. */
        GATEWAY_VALIDATED,
        /** Validazione completata con warning semantici ma accettata dal Gateway FSE 2.0. */
        GATEWAY_VALIDATED_WITH_WARNINGS,
        /** Richiesta rifiutata (errore locale o gateway). */
        REJECTED
    }
}