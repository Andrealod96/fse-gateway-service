package it.thcs.fse.fsersaservice.api.error;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.ServerWebExchange;
import it.thcs.fse.fsersaservice.domain.exception.CdaValidationException;
import it.thcs.fse.fsersaservice.domain.exception.SchematronValidationException;
import it.thcs.fse.fsersaservice.domain.exception.TerminologyValidationException;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            WebExchangeBindException ex,
            ServerWebExchange exchange
    ) {
        List<String> details = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .toList();

        return ResponseEntity.badRequest().body(
                new ApiErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "Richiesta non valida",
                        details,
                        exchange.getRequest().getPath().value()
                )
        );
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ApiErrorResponse> handleInputException(
            ServerWebInputException ex,
            ServerWebExchange exchange
    ) {
        return ResponseEntity.badRequest().body(
                new ApiErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "Payload JSON non valido o non deserializzabile",
                        List.of(ex.getReason() != null ? ex.getReason() : ex.getMessage()),
                        exchange.getRequest().getPath().value()
                )
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            ServerWebExchange exchange
    ) {
        List<String> details = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();

        return ResponseEntity.badRequest().body(
                new ApiErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "Violazione dei vincoli di validazione",
                        details,
                        exchange.getRequest().getPath().value()
                )
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex,
            ServerWebExchange exchange
    ) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                        "Errore interno del microservizio",
                        List.of(
                                ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName(),
                                ex.getCause() != null && ex.getCause().getMessage() != null
                                        ? ex.getCause().getMessage()
                                        : "Nessuna causa disponibile"
                        ),
                        exchange.getRequest().getPath().value()
                )
        );
    }
    @ExceptionHandler(CdaValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleCdaValidationException(
            CdaValidationException ex,
            ServerWebExchange exchange
    ) {
        return ResponseEntity.unprocessableEntity().body(
                new ApiErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.UNPROCESSABLE_ENTITY.value(),
                        HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase(),
                        "CDA non conforme allo schema XSD",
                        ex.getErrors(),
                        exchange.getRequest().getPath().value()
                )
        );
    }
    @ExceptionHandler(SchematronValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleSchematronValidationException(
            SchematronValidationException ex,
            ServerWebExchange exchange
    ) {
        List<String> details = new java.util.ArrayList<>(ex.getErrors());
        if (!ex.getWarnings().isEmpty()) {
            details.addAll(
                    ex.getWarnings().stream()
                            .map(w -> "WARNING: " + w)
                            .toList()
            );
        }

        return ResponseEntity.unprocessableEntity().body(
                new ApiErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.UNPROCESSABLE_ENTITY.value(),
                        HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase(),
                        "CDA non conforme allo Schematron RSA ufficiale",
                        details,
                        exchange.getRequest().getPath().value()
                )
        );
    }
    @ExceptionHandler(TerminologyValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleTerminologyValidationException(
            TerminologyValidationException ex,
            ServerWebExchange exchange
    ) {
        List<String> details = new java.util.ArrayList<>(ex.getErrors());
        if (!ex.getWarnings().isEmpty()) {
            details.addAll(
                    ex.getWarnings().stream()
                            .map(w -> "WARNING: " + w)
                            .toList()
            );
        }

        return ResponseEntity.unprocessableEntity().body(
                new ApiErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.UNPROCESSABLE_ENTITY.value(),
                        HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase(),
                        "CDA non conforme ai cataloghi terminologici caricati",
                        details,
                        exchange.getRequest().getPath().value()
                )
        );
    }
}