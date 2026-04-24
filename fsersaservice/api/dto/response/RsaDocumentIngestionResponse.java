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
        RECEIVED,
        REJECTED
    }
}