package it.thcs.fse.fsersaservice.infrastructure.gateway.exception;

public class GatewayResponseParsingException extends RuntimeException {

    private final Integer httpStatus;
    private final String responseBody;

    public GatewayResponseParsingException(String message, Integer httpStatus, String responseBody) {
        super(message);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
    }

    public GatewayResponseParsingException(String message, Integer httpStatus, String responseBody, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }
}