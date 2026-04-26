package it.thcs.fse.fsersaservice.infrastructure.gateway.exception;

public class GatewayClientException extends RuntimeException {

    private final int httpStatus;
    private final String traceId;
    private final String workflowInstanceId;
    private final String title;
    private final String detail;
    private final String responseBody;

    public GatewayClientException(
            String message,
            int httpStatus,
            String traceId,
            String workflowInstanceId,
            String title,
            String detail,
            String responseBody
    ) {
        super(message);
        this.httpStatus = httpStatus;
        this.traceId = traceId;
        this.workflowInstanceId = workflowInstanceId;
        this.title = title;
        this.detail = detail;
        this.responseBody = responseBody;
    }

    public GatewayClientException(
            String message,
            int httpStatus,
            String traceId,
            String workflowInstanceId,
            String title,
            String detail,
            String responseBody,
            Throwable cause
    ) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.traceId = traceId;
        this.workflowInstanceId = workflowInstanceId;
        this.title = title;
        this.detail = detail;
        this.responseBody = responseBody;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getWorkflowInstanceId() {
        return workflowInstanceId;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public String getResponseBody() {
        return responseBody;
    }
}