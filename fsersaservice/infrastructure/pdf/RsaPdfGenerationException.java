package it.thcs.fse.fsersaservice.infrastructure.pdf;

public class RsaPdfGenerationException extends RuntimeException {

    public RsaPdfGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public RsaPdfGenerationException(String message) {
        super(message);
    }
}