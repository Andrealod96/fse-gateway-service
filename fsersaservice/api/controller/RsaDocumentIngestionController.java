package it.thcs.fse.fsersaservice.api.controller;

import it.thcs.fse.fsersaservice.api.dto.request.RsaDocumentIngestionRequest;
import it.thcs.fse.fsersaservice.api.dto.response.RsaDocumentIngestionResponse;
import it.thcs.fse.fsersaservice.application.port.in.RsaDocumentIngestionUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rsa-documents")
public class RsaDocumentIngestionController {

    private final RsaDocumentIngestionUseCase rsaDocumentIngestionUseCase;

    public RsaDocumentIngestionController(RsaDocumentIngestionUseCase rsaDocumentIngestionUseCase) {
        this.rsaDocumentIngestionUseCase = rsaDocumentIngestionUseCase;
    }

    @PostMapping
    public ResponseEntity<RsaDocumentIngestionResponse> ingest(@Valid @RequestBody RsaDocumentIngestionRequest request) {
        RsaDocumentIngestionResponse response = rsaDocumentIngestionUseCase.ingest(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}