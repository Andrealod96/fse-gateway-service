package it.thcs.fse.fsersaservice.domain.exception;

import java.util.List;

public class SchematronValidationException extends RuntimeException {

    private final List<String> errors;
    private final List<String> warnings;

    public SchematronValidationException(String message, List<String> errors, List<String> warnings) {
        super(message);
        this.errors = List.copyOf(errors);
        this.warnings = List.copyOf(warnings);
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}