package it.thcs.fse.fsersaservice.infrastructure.signature;

public class RsaSignatureException extends RuntimeException {

    public RsaSignatureException(String message) {
        super(message);
    }

    public RsaSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}