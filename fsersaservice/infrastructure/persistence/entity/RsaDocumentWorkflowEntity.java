package it.thcs.fse.fsersaservice.infrastructure.persistence.entity;

import it.thcs.fse.fsersaservice.infrastructure.persistence.enums.RsaWorkflowStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "rsa_document_workflow",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_rsa_document_workflow_request_id", columnNames = "request_id")
        },
        indexes = {
                @Index(name = "idx_rsa_document_workflow_request_id", columnList = "request_id"),
                @Index(name = "idx_rsa_document_workflow_source_request_id", columnList = "source_request_id"),
                @Index(name = "idx_rsa_document_workflow_patient_fiscal_code", columnList = "patient_fiscal_code"),
                @Index(name = "idx_rsa_document_workflow_status", columnList = "workflow_status")
        }
)
public class RsaDocumentWorkflowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID tecnico interno del microservizio.
     * Non è né il ClinicalDocument/id né il workflowInstanceId del Gateway.
     */
    @Column(name = "request_id", nullable = false, length = 100)
    private String requestId;

    /**
     * ID richiesta proveniente dal sistema sorgente (PHP), se presente.
     */
    @Column(name = "source_request_id", length = 100)
    private String sourceRequestId;

    @Column(name = "source_system", nullable = false, length = 100)
    private String sourceSystem;

    @Column(name = "patient_fiscal_code", nullable = false, length = 16)
    private String patientFiscalCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "workflow_status", nullable = false, length = 50)
    private RsaWorkflowStatus workflowStatus;

    @Column(name = "cda_file_path", length = 1000)
    private String cdaFilePath;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected RsaDocumentWorkflowEntity() {
        // for JPA
    }

    public RsaDocumentWorkflowEntity(
            String requestId,
            String sourceRequestId,
            String sourceSystem,
            String patientFiscalCode,
            RsaWorkflowStatus workflowStatus,
            String cdaFilePath
    ) {
        this.requestId = requestId;
        this.sourceRequestId = sourceRequestId;
        this.sourceSystem = sourceSystem;
        this.patientFiscalCode = patientFiscalCode;
        this.workflowStatus = workflowStatus;
        this.cdaFilePath = cdaFilePath;
    }

    @PrePersist
    void onPrePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;

        if (this.workflowStatus == null) {
            this.workflowStatus = RsaWorkflowStatus.RECEIVED;
        }
    }

    @PreUpdate
    void onPreUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getSourceRequestId() {
        return sourceRequestId;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public String getPatientFiscalCode() {
        return patientFiscalCode;
    }

    public RsaWorkflowStatus getWorkflowStatus() {
        return workflowStatus;
    }

    public String getCdaFilePath() {
        return cdaFilePath;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setSourceRequestId(String sourceRequestId) {
        this.sourceRequestId = sourceRequestId;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public void setPatientFiscalCode(String patientFiscalCode) {
        this.patientFiscalCode = patientFiscalCode;
    }

    public void setWorkflowStatus(RsaWorkflowStatus workflowStatus) {
        this.workflowStatus = workflowStatus;
    }

    public void setCdaFilePath(String cdaFilePath) {
        this.cdaFilePath = cdaFilePath;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}