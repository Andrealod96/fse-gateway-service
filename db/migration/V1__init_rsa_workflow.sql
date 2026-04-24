CREATE TABLE rsa_document_workflow
(
    id                         BIGINT       NOT NULL AUTO_INCREMENT,
    request_id                 VARCHAR(100) NOT NULL,
    source_request_id          VARCHAR(100) NULL,
    source_system              VARCHAR(100) NOT NULL,
    patient_fiscal_code        VARCHAR(16)  NOT NULL,

    ingestion_status           VARCHAR(32)  NOT NULL,

    cda_file_path              VARCHAR(1000) NULL,
    pdf_file_path              VARCHAR(1000) NULL,
    pdfa_file_path             VARCHAR(1000) NULL,
    pdfa_embedded_file_path    VARCHAR(1000) NULL,
    signed_pdf_file_path       VARCHAR(1000) NULL,

    trace_id                   VARCHAR(200) NULL,
    workflow_instance_id       VARCHAR(500) NULL,
    gateway_validation_status  VARCHAR(50) NULL,
    gateway_publication_status VARCHAR(50) NULL,

    error_message              TEXT NULL,

    created_at                 TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                 TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    CONSTRAINT uk_rsa_document_workflow_request_id UNIQUE (request_id),

    KEY                        idx_rsa_document_workflow_source_request_id (source_request_id),
    KEY                        idx_rsa_document_workflow_patient_fiscal_code (patient_fiscal_code),
    KEY                        idx_rsa_document_workflow_status (ingestion_status),
    KEY                        idx_rsa_document_workflow_workflow_instance_id (workflow_instance_id),
    KEY                        idx_rsa_document_workflow_trace_id (trace_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;