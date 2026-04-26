package it.thcs.fse.fsersaservice.api.controller;

import it.thcs.fse.fsersaservice.api.dto.request.RsaDocumentIngestionRequest;
import it.thcs.fse.fsersaservice.api.dto.response.RsaDocumentIngestionResponse;
import it.thcs.fse.fsersaservice.application.port.in.RsaDocumentIngestionUseCase;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping(path = "/api/v1/rsa-documents", produces = MediaType.APPLICATION_JSON_VALUE)
public class RsaDocumentIngestionController {

    private final RsaDocumentIngestionUseCase rsaDocumentIngestionUseCase;

    public RsaDocumentIngestionController(RsaDocumentIngestionUseCase rsaDocumentIngestionUseCase) {
        this.rsaDocumentIngestionUseCase = rsaDocumentIngestionUseCase;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<RsaDocumentIngestionResponse> ingest(@Valid @RequestBody RsaDocumentIngestionRequest request) {
        return Mono.fromCallable(() -> rsaDocumentIngestionUseCase.ingest(request))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
