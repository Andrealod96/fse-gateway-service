package it.thcs.fse.fsersaservice.api.controller;

import it.thcs.fse.fsersaservice.api.dto.callback.GatewayWorkflowStatusCallbackRequest;
import it.thcs.fse.fsersaservice.api.dto.callback.GatewayWorkflowStatusEventDto;
import it.thcs.fse.fsersaservice.infrastructure.persistence.entity.RsaDocumentWorkflowEntity;
import it.thcs.fse.fsersaservice.infrastructure.persistence.enums.RsaWorkflowStatus;
import it.thcs.fse.fsersaservice.infrastructure.persistence.repository.RsaDocumentWorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Endpoint di callback invocato dal Gateway FSE 2.0 per notificare
 * l'esito delle operazioni asincrone (pubblicazione su INI/EDS/UA-R).
 *
 * Il Gateway chiama questo endpoint dopo che le operazioni avviate con
 * POST /v1/documents/validate-and-create sono completate.
 *
 * Il controller:
 *  1. Riceve la notifica con workflowInstanceId + lista eventi
 *  2. Recupera il workflow locale tramite workflowInstanceId
 *  3. Aggiorna gatewayPublicationStatus e workflowStatus nel DB
 *  4. Risponde sempre 200 (il Gateway NON riprova se riceve un 2xx)
 *
 * Nota: il path /v1/workflow/status deve essere ESCLUSO
 * dal filtro di autenticazione API key — il Gateway non invia la chiave.
 * Verifica SecurityConfig.
 */
@RestController
@RequestMapping(path = "/v1/workflow/status", produces = MediaType.APPLICATION_JSON_VALUE)
public class GatewayWorkflowStatusCallbackController {

    private static final Logger log =
            LoggerFactory.getLogger(GatewayWorkflowStatusCallbackController.class);

    // Tipi di evento che indicano la fine della transazione
    private static final String EVENT_UAR_FINAL  = "UAR_FINAL_STATUS";
    private static final String EVENT_SEND_INI   = "SEND_TO_INI";
    private static final String EVENT_PUBLICATION = "PUBLICATION";

    // Valori di stato evento
    private static final String STATUS_SUCCESS        = "SUCCESS";
    private static final String STATUS_BLOCKING_ERROR = "BLOCKING_ERROR";

    private final RsaDocumentWorkflowRepository workflowRepository;

    public GatewayWorkflowStatusCallbackController(
            RsaDocumentWorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public Mono<ResponseEntity<Map<String, Object>>> receiveWorkflowStatus(
            @RequestBody GatewayWorkflowStatusCallbackRequest callbackRequest
    ) {
        return Mono.fromCallable(() -> processCallback(callbackRequest))
                .subscribeOn(Schedulers.boundedElastic())
                .map(accepted -> ResponseEntity.ok()
                        .<Map<String, Object>>body(Map.of(
                                "received", true,
                                "workflowInstanceId",
                                accepted ? callbackRequest.getWorkflowInstanceId() : "unknown"
                        )));
    }

    // ─── Logica di elaborazione ───────────────────────────────────────────────

    private boolean processCallback(GatewayWorkflowStatusCallbackRequest req) {
        String workflowInstanceId = req.getWorkflowInstanceId();
        List<GatewayWorkflowStatusEventDto> events = req.getEvents();

        log.info("Callback Gateway ricevuta: workflowInstanceId={}, eventi={}",
                workflowInstanceId,
                events != null ? events.size() : 0);

        if (!StringUtils.hasText(workflowInstanceId)) {
            log.warn("Callback Gateway ricevuta senza workflowInstanceId — ignorata");
            return false;
        }

        if (events == null || events.isEmpty()) {
            log.warn("Callback Gateway ricevuta senza eventi: workflowInstanceId={}", workflowInstanceId);
            return true; // rispondiamo 200 comunque
        }

        // Logga tutti gli eventi ricevuti
        events.forEach(e -> log.info(
                "  Evento Gateway: type={}, status={}, date={}, issuer={}",
                e.getEventType(), e.getEventStatus(), e.getEventDate(), e.getIssuer()));

        // Trova il workflow nel DB
        Optional<RsaDocumentWorkflowEntity> workflowOpt =
                workflowRepository.findByWorkflowInstanceId(workflowInstanceId);

        if (workflowOpt.isEmpty()) {
            // Potrebbe essere una pubblicazione su un workflow non iniziato da questo microservizio
            // oppure un workflowInstanceId non ancora persistito. Rispondiamo 200 comunque.
            log.warn("Workflow non trovato per workflowInstanceId={} — callback ignorata",
                    workflowInstanceId);
            return false;
        }

        RsaDocumentWorkflowEntity workflow = workflowOpt.get();
        updateWorkflowFromEvents(workflow, events);
        workflowRepository.save(workflow);

        log.info("Workflow aggiornato da callback: workflowInstanceId={}, nuovoStato={}",
                workflowInstanceId, workflow.getWorkflowStatus());

        return true;
    }

    /**
     * Determina il nuovo stato del workflow analizzando la lista di eventi.
     *
     * Priorità di lettura (dal più significativo):
     *  1. UAR_FINAL_STATUS — è l'evento terminale, ha precedenza su tutto
     *  2. SEND_TO_INI      — se ha BLOCKING_ERROR, la pubblicazione è fallita
     *  3. PUBLICATION      — esito della pubblicazione su EDS
     *
     * Se uno qualsiasi degli eventi ha BLOCKING_ERROR → FAILED
     * Se UAR_FINAL_STATUS = SUCCESS → PUBLISHED
     * Altrimenti, prendiamo l'ultimo evento e deriviamo lo stato.
     */
    private void updateWorkflowFromEvents(
            RsaDocumentWorkflowEntity workflow,
            List<GatewayWorkflowStatusEventDto> events
    ) {
        // Controlla se c'è un BLOCKING_ERROR in qualsiasi evento
        boolean hasBlockingError = events.stream()
                .anyMatch(e -> STATUS_BLOCKING_ERROR.equalsIgnoreCase(e.getEventStatus()));

        if (hasBlockingError) {
            GatewayWorkflowStatusEventDto errorEvent = events.stream()
                    .filter(e -> STATUS_BLOCKING_ERROR.equalsIgnoreCase(e.getEventStatus()))
                    .findFirst()
                    .orElse(null);

            String errorType = errorEvent != null ? errorEvent.getEventType() : "UNKNOWN";
            log.error("BLOCKING_ERROR nella callback: workflowInstanceId={}, eventType={}",
                    workflow.getWorkflowInstanceId(), errorType);

            workflow.setGatewayPublicationStatus("BLOCKING_ERROR:" + errorType);
            workflow.setWorkflowStatus(RsaWorkflowStatus.FAILED);
            return;
        }

        // Cerca UAR_FINAL_STATUS — evento terminale di successo
        Optional<GatewayWorkflowStatusEventDto> uarFinal = events.stream()
                .filter(e -> EVENT_UAR_FINAL.equalsIgnoreCase(e.getEventType()))
                .findFirst();

        if (uarFinal.isPresent()) {
            if (STATUS_SUCCESS.equalsIgnoreCase(uarFinal.get().getEventStatus())) {
                workflow.setGatewayPublicationStatus("SUCCESS");
                workflow.setWorkflowStatus(RsaWorkflowStatus.PUBLISHED);
            } else {
                workflow.setGatewayPublicationStatus("ERROR:" + uarFinal.get().getEventStatus());
                workflow.setWorkflowStatus(RsaWorkflowStatus.FAILED);
            }
            return;
        }

        // Nessun UAR_FINAL ancora — prendi l'ultimo evento cronologicamente
        events.stream()
                .filter(e -> e.getEventDate() != null && !e.getEventDate().isBlank())
                .max(Comparator.comparing(GatewayWorkflowStatusEventDto::getEventDate))
                .ifPresent(lastEvent -> {
                    String summary = lastEvent.getEventType() + ":" + lastEvent.getEventStatus();
                    workflow.setGatewayPublicationStatus(summary);

                    // Se l'ultimo evento è PUBLICATION SUCCESS, aggiorniamo lo stato
                    if (EVENT_PUBLICATION.equalsIgnoreCase(lastEvent.getEventType())
                            && STATUS_SUCCESS.equalsIgnoreCase(lastEvent.getEventStatus())) {
                        workflow.setWorkflowStatus(RsaWorkflowStatus.PUBLISHED);
                    }
                    // Altrimenti lasciamo lo stato corrente — arriveranno altri eventi
                });
    }
}