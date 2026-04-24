package it.thcs.fse.fsersaservice.infrastructure.gateway.exception;

public class GatewayResponseParsingException extends RuntimeException {

    public GatewayResponseParsingException(String message) {
        super(message);
    }

    public GatewayResponseParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}