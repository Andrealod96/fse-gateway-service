package it.thcs.fse.fsersaservice.domain.exception;

import java.util.List;

public class CdaValidationException extends RuntimeException {

    private final List<String> errors;

    public CdaValidationException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}