package it.thcs.fse.fsersaservice.infrastructure.persistence.adapter;

import it.thcs.fse.fsersaservice.application.port.out.GatewayStatusContextPort;
import it.thcs.fse.fsersaservice.config.properties.JwtProperties;
import it.thcs.fse.fsersaservice.infrastructure.gateway.jwt.GatewayStatusJwtContext;
import it.thcs.fse.fsersaservice.infrastructure.persistence.entity.RsaDocumentWorkflowEntity;
import it.thcs.fse.fsersaservice.infrastructure.persistence.repository.RsaDocumentWorkflowRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class JpaGatewayStatusContextAdapter implements GatewayStatusContextPort {

    private static final String DEFAULT_WORKFLOW_ACTION_ID = "CREATE";
    private static final String DEFAULT_RSA_RESOURCE_HL7_TYPE = "('11488-4^^2.16.840.1.113883.6.1')";

    private final RsaDocumentWorkflowRepository workflowRepository;
    private final JwtProperties jwtProperties;

    public JpaGatewayStatusContextAdapter(
            RsaDocumentWorkflowRepository workflowRepository,
            JwtProperties jwtProperties
    ) {
        this.workflowRepository = workflowRepository;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public Mono<GatewayStatusJwtContext> findByWorkflowInstanceId(String workflowInstanceId) {
        return Mono.fromCallable(() -> workflowRepository.findByWorkflowInstanceId(workflowInstanceId)
                        .map(this::toContext)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Workflow non trovato per workflowInstanceId: " + workflowInstanceId
                        )))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<GatewayStatusJwtContext> findByTraceId(String traceId) {
        return Mono.fromCallable(() -> workflowRepository.findByTraceId(traceId)
                        .map(this::toContext)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Workflow non trovato per traceId: " + traceId
                        )))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private GatewayStatusJwtContext toContext(RsaDocumentWorkflowEntity entity) {
        String patientFiscalCode = requireText(entity.getPatientFiscalCode(), "patientFiscalCode");

        return new GatewayStatusJwtContext(
                jwtProperties.getSubjectFiscalCode(),
                patientFiscalCode,
                jwtProperties.getSubjectRole(),
                jwtProperties.getPurposeOfUse(),
                jwtProperties.getSubjectOrganization(),
                jwtProperties.getSubjectOrganizationId(),
                jwtProperties.getSubjectApplicationId(),
                jwtProperties.getSubjectApplicationVendor(),
                jwtProperties.getSubjectApplicationVersion(),
                jwtProperties.getLocality(),
                Boolean.TRUE,
                DEFAULT_WORKFLOW_ACTION_ID,
                firstNonBlank(jwtProperties.getWorkflowStatusResourceHl7Type(), DEFAULT_RSA_RESOURCE_HL7_TYPE),
                jwtProperties.getUseSubjectAsAuthor()
        );
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " mancante nel workflow locale");
        }
        return value.trim();
    }

    private String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        if (StringUtils.hasText(second)) {
            return second.trim();
        }
        return null;
    }
}
