package it.thcs.fse.fsersaservice.infrastructure.gateway.exception;

public class GatewayClientException extends RuntimeException {

    private final int httpStatus;
    private final String responseBody;
    private final String traceId;

    public GatewayClientException(String message, int httpStatus, String responseBody, String traceId) {
        super(message);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
        this.traceId = traceId;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getTraceId() {
        return traceId;
    }
}