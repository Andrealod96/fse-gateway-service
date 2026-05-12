package it.thcs.fse.fsersaservice.application.port.in;

import it.thcs.fse.fsersaservice.api.dto.request.RsaDocumentIngestionRequest;
import it.thcs.fse.fsersaservice.api.dto.response.RsaDocumentIngestionResponse;

public interface RsaDocumentPublishUseCase {

    /**
     * Esegue il pipeline completo di generazione, validazione e pubblicazione
     * contestuale (validate-and-create) verso il Gateway FSE 2.0.
     *
     * La request deve contenere il campo {@code pubblicazione} con i metadati
     * XDS obbligatori per l'indicizzazione su INI/EDS.
     *
     * @param request dati clinici + metadati XDS
     * @return response con traceId e workflowInstanceId del Gateway
     */
    RsaDocumentIngestionResponse publish(RsaDocumentIngestionRequest request);
}