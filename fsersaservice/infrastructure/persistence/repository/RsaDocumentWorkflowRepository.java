package it.thcs.fse.fsersaservice.infrastructure.persistence.repository;

import it.thcs.fse.fsersaservice.infrastructure.persistence.entity.RsaDocumentWorkflowEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RsaDocumentWorkflowRepository extends JpaRepository<RsaDocumentWorkflowEntity, Long> {

    Optional<RsaDocumentWorkflowEntity> findByRequestId(String requestId);

    Optional<RsaDocumentWorkflowEntity> findBySourceRequestId(String sourceRequestId);

    Optional<RsaDocumentWorkflowEntity> findByWorkflowInstanceId(String workflowInstanceId);
    Optional<RsaDocumentWorkflowEntity> findByTraceId(String traceId);

    boolean existsByRequestId(String requestId);
}