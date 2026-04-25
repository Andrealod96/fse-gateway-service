package it.thcs.fse.fsersaservice.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class RsaDocumentIngestionResponse {

    private final String requestId;
    private final Status status;
    private final String message;
    private final OffsetDateTime receivedAt;

    public enum Status {
        /** Richiesta ricevuta e registrata — usato solo se la pipeline non è ancora completata. */
        RECEIVED,
        /** Validazione completata con successo sul Gateway FSE 2.0. */
        GATEWAY_VALIDATED,
        /** Richiesta rifiutata (errore locale o gateway). */
        REJECTED
    }
}