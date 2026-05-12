package it.thcs.fse.fsersaservice.application.port.in;

import it.thcs.fse.fsersaservice.api.dto.request.RsaDocumentIngestionRequest;
import it.thcs.fse.fsersaservice.api.dto.response.RsaDocumentIngestionResponse;

public interface RsaDocumentIngestionUseCase {

    RsaDocumentIngestionResponse ingest(RsaDocumentIngestionRequest request);
}