package it.thcs.fse.fsersaservice.infrastructure.gateway.exception;

public class GatewayTransportException extends RuntimeException {

    public GatewayTransportException(String message) {
        super(message);
    }

    public GatewayTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}