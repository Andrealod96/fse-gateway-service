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
                @Index(name = "idx_rsa_document_workflow_source_request_id", columnList = "source_request_id"),
                @Index(name = "idx_rsa_document_workflow_patient_fiscal_code", columnList = "patient_fiscal_code"),
                @Index(name = "idx_rsa_document_workflow_status", columnList = "ingestion_status"),
                @Index(name = "idx_rsa_document_workflow_workflow_instance_id", columnList = "workflow_instance_id"),
                @Index(name = "idx_rsa_document_workflow_trace_id", columnList = "trace_id")
        }
)
public class RsaDocumentWorkflowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── Identificativi ───────────────────────────────────────────────────────

    /** ID tecnico interno del microservizio. Diverso dal ClinicalDocument/id e dal workflowInstanceId. */
    @Column(name = "request_id", nullable = false, length = 100)
    private String requestId;

    /** ID richiesta proveniente dal sistema sorgente (PHP), se presente. */
    @Column(name = "source_request_id", length = 100)
    private String sourceRequestId;

    @Column(name = "source_system", nullable = false, length = 100)
    private String sourceSystem;

    @Column(name = "patient_fiscal_code", nullable = false, length = 16)
    private String patientFiscalCode;

    // ─── Stato workflow ───────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "ingestion_status", nullable = false, length = 32)
    private RsaWorkflowStatus workflowStatus;

    // ─── Path file generati ───────────────────────────────────────────────────

    @Column(name = "cda_file_path", length = 1000)
    private String cdaFilePath;

    @Column(name = "pdf_file_path", length = 1000)
    private String pdfFilePath;

    @Column(name = "pdfa_file_path", length = 1000)
    private String pdfaFilePath;

    @Column(name = "pdfa_embedded_file_path", length = 1000)
    private String pdfaEmbeddedFilePath;

    @Column(name = "signed_pdf_file_path", length = 1000)
    private String signedPdfFilePath;

    // ─── Esiti Gateway FSE 2.0 ────────────────────────────────────────────────

    /**
     * traceID restituito dal Gateway nella risposta di validazione.
     * Serve per il supporto Sogei in caso di problemi.
     */
    @Column(name = "trace_id", length = 200)
    private String traceId;

    /**
     * workflowInstanceId restituito dal Gateway nella risposta di validazione.
     * Necessario per la successiva chiamata di pubblicazione.
     * Non coincide con requestId né con l'id del ClinicalDocument.
     */
    @Column(name = "workflow_instance_id", length = 500)
    private String workflowInstanceId;

    /**
     * Esito della validazione restituito dal Gateway.
     * Valori tipici: OK, KO.
     */
    @Column(name = "gateway_validation_status", length = 50)
    private String gatewayValidationStatus;

    /**
     * Esito della pubblicazione restituito dal Gateway.
     * Null fino a quando non viene eseguita la pubblicazione.
     */
    @Column(name = "gateway_publication_status", length = 50)
    private String gatewayPublicationStatus;

    // ─── Errore ───────────────────────────────────────────────────────────────

    /** Messaggio di errore tecnico. Popolato solo quando workflowStatus = FAILED. */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // ─── Audit ────────────────────────────────────────────────────────────────

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ─── Costruttori ──────────────────────────────────────────────────────────

    protected RsaDocumentWorkflowEntity() {
        // for JPA
    }

    public RsaDocumentWorkflowEntity(
            String requestId,
            String sourceRequestId,
            String sourceSystem,
            String patientFiscalCode,
            RsaWorkflowStatus workflowStatus
    ) {
        this.requestId = requestId;
        this.sourceRequestId = sourceRequestId;
        this.sourceSystem = sourceSystem;
        this.patientFiscalCode = patientFiscalCode;
        this.workflowStatus = workflowStatus;
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

    // ─── Getter ───────────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public String getRequestId() { return requestId; }
    public String getSourceRequestId() { return sourceRequestId; }
    public String getSourceSystem() { return sourceSystem; }
    public String getPatientFiscalCode() { return patientFiscalCode; }
    public RsaWorkflowStatus getWorkflowStatus() { return workflowStatus; }
    public String getCdaFilePath() { return cdaFilePath; }
    public String getPdfFilePath() { return pdfFilePath; }
    public String getPdfaFilePath() { return pdfaFilePath; }
    public String getPdfaEmbeddedFilePath() { return pdfaEmbeddedFilePath; }
    public String getSignedPdfFilePath() { return signedPdfFilePath; }
    public String getTraceId() { return traceId; }
    public String getWorkflowInstanceId() { return workflowInstanceId; }
    public String getGatewayValidationStatus() { return gatewayValidationStatus; }
    public String getGatewayPublicationStatus() { return gatewayPublicationStatus; }
    public String getErrorMessage() { return errorMessage; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    // ─── Setter ───────────────────────────────────────────────────────────────

    public void setWorkflowStatus(RsaWorkflowStatus workflowStatus) { this.workflowStatus = workflowStatus; }
    public void setCdaFilePath(String cdaFilePath) { this.cdaFilePath = cdaFilePath; }
    public void setPdfFilePath(String pdfFilePath) { this.pdfFilePath = pdfFilePath; }
    public void setPdfaFilePath(String pdfaFilePath) { this.pdfaFilePath = pdfaFilePath; }
    public void setPdfaEmbeddedFilePath(String pdfaEmbeddedFilePath) { this.pdfaEmbeddedFilePath = pdfaEmbeddedFilePath; }
    public void setSignedPdfFilePath(String signedPdfFilePath) { this.signedPdfFilePath = signedPdfFilePath; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public void setWorkflowInstanceId(String workflowInstanceId) { this.workflowInstanceId = workflowInstanceId; }
    public void setGatewayValidationStatus(String gatewayValidationStatus) { this.gatewayValidationStatus = gatewayValidationStatus; }
    public void setGatewayPublicationStatus(String gatewayPublicationStatus) { this.gatewayPublicationStatus = gatewayPublicationStatus; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}