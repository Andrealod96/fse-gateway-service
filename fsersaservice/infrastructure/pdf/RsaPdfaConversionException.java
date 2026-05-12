package it.thcs.fse.fsersaservice.infrastructure.pdf;

public class RsaPdfaConversionException extends RuntimeException {

    public RsaPdfaConversionException(String message) {
        super(message);
    }

    public RsaPdfaConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}