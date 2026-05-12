package it.thcs.fse.fsersaservice.infrastructure.gateway.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GatewayProblemDetailsDto {

    @JsonProperty("traceID")
    private String traceId;

    @JsonProperty("spanID")
    private String spanId;

    private String type;
    private String title;
    private String detail;
    private Integer status;
    private String instance;
    private String workflowInstanceId;
    private String warning;

    public String getTraceId() {
        return traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public Integer getStatus() {
        return status;
    }

    public String getInstance() {
        return instance;
    }

    public String getWorkflowInstanceId() {
        return workflowInstanceId;
    }

    public String getWarning() {
        return warning;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public void setWorkflowInstanceId(String workflowInstanceId) {
        this.workflowInstanceId = workflowInstanceId;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }
}