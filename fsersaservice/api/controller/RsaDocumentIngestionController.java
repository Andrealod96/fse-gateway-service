package it.thcs.fse.fsersaservice.api.controller;

import it.thcs.fse.fsersaservice.api.dto.request.RsaDocumentIngestionRequest;
import it.thcs.fse.fsersaservice.api.dto.response.RsaDocumentIngestionResponse;
import it.thcs.fse.fsersaservice.application.port.in.RsaDocumentIngestionUseCase;
import it.thcs.fse.fsersaservice.application.port.in.RsaDocumentPublishUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Entry point REST per i documenti RSA (Referto di Specialistica Ambulatoriale).
 *
 * Endpoint esposti:
 *
 *   POST /api/v1/rsa-documents
 *     Flusso di sola validazione → Gateway /v1/documents/validation
 *     Risposta 201 in caso di successo (activity=VALIDATION).
 *
 *   POST /api/v1/rsa-documents/publish
 *     Flusso validate-and-create → Gateway /v1/documents/validate-and-create
 *     Richiede il campo "pubblicazione" con i metadati XDS obbligatori.
 *     Risposta 202 Accepted: la validazione è sincrona,
 *     la pubblicazione su INI/EDS è asincrona.
 */
@RestController
@RequestMapping(path = "/api/v1/rsa-documents", produces = MediaType.APPLICATION_JSON_VALUE)
public class RsaDocumentIngestionController {

    private final RsaDocumentIngestionUseCase rsaDocumentIngestionUseCase;
    private final RsaDocumentPublishUseCase rsaDocumentPublishUseCase;

    public RsaDocumentIngestionController(
            RsaDocumentIngestionUseCase rsaDocumentIngestionUseCase,
            RsaDocumentPublishUseCase rsaDocumentPublishUseCase
    ) {
        this.rsaDocumentIngestionUseCase = rsaDocumentIngestionUseCase;
        this.rsaDocumentPublishUseCase = rsaDocumentPublishUseCase;
    }

    // ─── Validazione semplice ─────────────────────────────────────────────────

    /**
     * Riceve i dati clinici RSA, esegue il pipeline completo
     * (CDA2 → validazione locale → PDF/A-3 → firma PAdES → Gateway validazione)
     * e restituisce traceId e workflowInstanceId.
     *
     * HTTP 201 allineato con la specifica Ministero (activity=VALIDATION → 201).
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<RsaDocumentIngestionResponse>> ingest(
            @Valid @RequestBody RsaDocumentIngestionRequest request
    ) {
        return Mono.fromCallable(() -> rsaDocumentIngestionUseCase.ingest(request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(response));
    }

    // ─── Validate-and-create (pubblicazione contestuale) ─────────────────────

    /**
     * Riceve i dati clinici RSA + metadati XDS, esegue il pipeline completo
     * (CDA2 → validazione locale → PDF/A-3 → firma PAdES → Gateway validate-and-create)
     * e restituisce la presa in carico con traceId e workflowInstanceId.
     *
     * Il campo "pubblicazione" nella request è obbligatorio e deve contenere:
     * tipologiaStruttura, identificativoDoc, identificativoRep,
     * tipoDocumentoLivAlto, assettoOrganizzativo, tipoAttivitaClinica,
     * identificativoSottomissione.
     *
     * HTTP 202 Accepted: la validazione è sincrona, la pubblicazione
     * su INI/EDS è asincrona. Il Gateway notificherà l'esito finale
     * tramite callback POST /v1/workflow/status.
     */
    @PostMapping(path = "/publish", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<RsaDocumentIngestionResponse>> publish(
            @Valid @RequestBody RsaDocumentIngestionRequest request
    ) {
        return Mono.fromCallable(() -> rsaDocumentPublishUseCase.publish(request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> ResponseEntity
                        .status(HttpStatus.ACCEPTED)
                        .body(response));
    }
}