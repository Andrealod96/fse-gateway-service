package it.thcs.fse.fsersaservice.api.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class ApiErrorResponse {

    private final OffsetDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final List<String> details;
    private final String path;
}