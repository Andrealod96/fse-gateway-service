package it.thcs.fse.fsersaservice.api.dto.callback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Singolo evento di stato ricevuto nella notifica callback del Gateway FSE 2.0.
 *
 * Tipi di evento (eventType):
 *   VALIDATION       — esito della validazione CDA
 *   PUBLICATION      — esito della pubblicazione su EDS
 *   SEND_TO_INI      — esito dell'invio metadati a INI
 *   SEND_TO_UAR      — esito dell'invio alla UA-R
 *   UAR_FINAL_STATUS — esito finale dalla UA-R (evento terminale)
 *
 * Valori di stato (eventStatus):
 *   SUCCESS          — operazione completata con successo
 *   BLOCKING_ERROR   — errore bloccante, la transazione non proseguirà
 *   WARNING          — completato con avvisi non bloccanti
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GatewayWorkflowStatusEventDto {

    private String eventType;
    private String eventDate;
    private String eventStatus;
    private String issuer;
}