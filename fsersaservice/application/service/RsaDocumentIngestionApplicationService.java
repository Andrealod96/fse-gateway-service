package it.thcs.fse.fsersaservice.application.service;

import it.thcs.fse.fsersaservice.api.dto.request.RsaDocumentIngestionRequest;
import it.thcs.fse.fsersaservice.api.dto.response.RsaDocumentIngestionResponse;
import it.thcs.fse.fsersaservice.application.mapper.RsaDocumentRequestMapper;
import it.thcs.fse.fsersaservice.application.port.in.RsaDocumentIngestionUseCase;
import it.thcs.fse.fsersaservice.application.port.in.RsaDocumentPublishUseCase;
import it.thcs.fse.fsersaservice.config.properties.GatewayProperties;
import it.thcs.fse.fsersaservice.config.properties.StorageProperties;
import it.thcs.fse.fsersaservice.domain.exception.CdaValidationException;
import it.thcs.fse.fsersaservice.domain.exception.SchematronValidationException;
import it.thcs.fse.fsersaservice.domain.exception.TerminologyValidationException;
import it.thcs.fse.fsersaservice.domain.model.RsaDocument;
import it.thcs.fse.fsersaservice.infrastructure.cda.CdaDocumentXmlBuilder;
import it.thcs.fse.fsersaservice.infrastructure.cda.CdaGenerationResult;
import it.thcs.fse.fsersaservice.infrastructure.gateway.client.FseGatewayClient;
import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.request.GatewayDocumentValidationRequest;
import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.response.GatewayValidationResponseDto;
import it.thcs.fse.fsersaservice.infrastructure.gateway.exception.GatewayClientException;
import it.thcs.fse.fsersaservice.infrastructure.gateway.exception.GatewayResponseParsingException;
import it.thcs.fse.fsersaservice.infrastructure.gateway.exception.GatewayTransportException;
import it.thcs.fse.fsersaservice.infrastructure.gateway.mapper.GatewayDocumentRequestMapper;
import it.thcs.fse.fsersaservice.infrastructure.pdf.RsaPdfRenderer;
import it.thcs.fse.fsersaservice.infrastructure.pdf.RsaPdfa3CdaEmbedder;
import it.thcs.fse.fsersaservice.infrastructure.pdf.RsaPdfa3Converter;
import it.thcs.fse.fsersaservice.infrastructure.persistence.entity.RsaDocumentWorkflowEntity;
import it.thcs.fse.fsersaservice.infrastructure.persistence.enums.RsaWorkflowStatus;
import it.thcs.fse.fsersaservice.infrastructure.persistence.repository.RsaDocumentWorkflowRepository;
import it.thcs.fse.fsersaservice.infrastructure.signature.RsaPadesSigner;
import it.thcs.fse.fsersaservice.infrastructure.validation.schematron.CdaSchematronValidator;
import it.thcs.fse.fsersaservice.infrastructure.validation.terminology.CdaTerminologyValidator;
import it.thcs.fse.fsersaservice.infrastructure.validation.xsd.CdaXsdValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class RsaDocumentIngestionApplicationService
        implements RsaDocumentIngestionUseCase, RsaDocumentPublishUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(RsaDocumentIngestionApplicationService.class);

    private final RsaDocumentRequestMapper rsaDocumentRequestMapper;
    private final CdaDocumentXmlBuilder cdaDocumentXmlBuilder;
    private final CdaXsdValidator cdaXsdValidator;
    private final CdaSchematronValidator cdaSchematronValidator;
    private final CdaTerminologyValidator cdaTerminologyValidator;
    private final RsaPdfRenderer rsaPdfRenderer;
    private final RsaPdfa3Converter rsaPdfa3Converter;
    private final RsaPdfa3CdaEmbedder rsaPdfa3CdaEmbedder;
    private final RsaPadesSigner rsaPadesSigner;
    private final GatewayDocumentRequestMapper gatewayDocumentRequestMapper;
    private final FseGatewayClient fseGatewayClient;
    private final RsaDocumentWorkflowRepository workflowRepository;
    private final StorageProperties storageProperties;
    private final GatewayProperties gatewayProperties;

    public RsaDocumentIngestionApplicationService(
            RsaDocumentRequestMapper rsaDocumentRequestMapper,
            CdaDocumentXmlBuilder cdaDocumentXmlBuilder,
            CdaXsdValidator cdaXsdValidator,
            CdaSchematronValidator cdaSchematronValidator,
            CdaTerminologyValidator cdaTerminologyValidator,
            RsaPdfRenderer rsaPdfRenderer,
            RsaPdfa3Converter rsaPdfa3Converter,
            RsaPdfa3CdaEmbedder rsaPdfa3CdaEmbedder,
            RsaPadesSigner rsaPadesSigner,
            GatewayDocumentRequestMapper gatewayDocumentRequestMapper,
            FseGatewayClient fseGatewayClient,
            RsaDocumentWorkflowRepository workflowRepository,
            StorageProperties storageProperties,
            GatewayProperties gatewayProperties
    ) {
        this.rsaDocumentRequestMapper = rsaDocumentRequestMapper;
        this.cdaDocumentXmlBuilder = cdaDocumentXmlBuilder;
        this.cdaXsdValidator = cdaXsdValidator;
        this.cdaSchematronValidator = cdaSchematronValidator;
        this.cdaTerminologyValidator = cdaTerminologyValidator;
        this.rsaPdfRenderer = rsaPdfRenderer;
        this.rsaPdfa3Converter = rsaPdfa3Converter;
        this.rsaPdfa3CdaEmbedder = rsaPdfa3CdaEmbedder;
        this.rsaPadesSigner = rsaPadesSigner;
        this.gatewayDocumentRequestMapper = gatewayDocumentRequestMapper;
        this.fseGatewayClient = fseGatewayClient;
        this.workflowRepository = workflowRepository;
        this.storageProperties = storageProperties;
        this.gatewayProperties = gatewayProperties;
    }

    // ─── Validazione semplice ─────────────────────────────────────────────────

    @Override
    @Transactional
    public RsaDocumentIngestionResponse ingest(RsaDocumentIngestionRequest request) {
        String requestId = UUID.randomUUID().toString();
        RsaDocument rsaDocument = rsaDocumentRequestMapper.toDomain(request);
        RsaDocumentWorkflowEntity workflow = createAndSaveWorkflow(
                requestId, request.getSourceRequestId(),
                rsaDocument.getSourceSystem(), rsaDocument.getPatient().getFiscalCode());

        try {
            PipelineResult pipeline = runDocumentPipeline(requestId, rsaDocument, workflow);

            GatewayDocumentValidationRequest gatewayRequest =
                    gatewayDocumentRequestMapper.toValidationRequest(rsaDocument, pipeline.signedPdf());
            updateWorkflow(workflow, RsaWorkflowStatus.GATEWAY_SUBMITTED, w -> {});

            GatewayValidationResponseDto response = callGateway(
                    fseGatewayClient.validateDocument(gatewayRequest));

            return buildSuccessResponse(requestId, workflow, response,
                    "Documento RSA validato dal Gateway FSE 2.0.");

        } catch (Exception ex) {
            return handleException(requestId, workflow, ex);
        }
    }

    // ─── Validate-and-create ──────────────────────────────────────────────────

    @Override
    @Transactional
    public RsaDocumentIngestionResponse publish(RsaDocumentIngestionRequest request) {
        if (request.getPubblicazione() == null) {
            throw new IllegalArgumentException(
                    "Il campo 'pubblicazione' con i metadati XDS è obbligatorio per il flusso publish.");
        }

        String requestId = UUID.randomUUID().toString();
        RsaDocument rsaDocument = rsaDocumentRequestMapper.toDomain(request);
        RsaDocumentWorkflowEntity workflow = createAndSaveWorkflow(
                requestId, request.getSourceRequestId(),
                rsaDocument.getSourceSystem(), rsaDocument.getPatient().getFiscalCode());

        try {
            PipelineResult pipeline = runDocumentPipeline(requestId, rsaDocument, workflow);

            // L'identificativoDoc DEVE corrispondere al ClinicalDocument/id del CDA generato.
            // Lo estraiamo direttamente dal risultato della build — non usiamo quello della request.
            String identificativoDocReale = pipeline.cdaResult().identificativoDocXds();

            log.info("validate-and-create: requestId={}, identificativoDoc={}",
                    requestId, identificativoDocReale);

            GatewayDocumentValidationRequest gatewayRequest =
                    gatewayDocumentRequestMapper.toValidateAndCreateRequest(
                            rsaDocument,
                            pipeline.signedPdf(),
                            request.getPubblicazione(),
                            identificativoDocReale
                    );
            updateWorkflow(workflow, RsaWorkflowStatus.GATEWAY_SUBMITTED, w -> {});

            GatewayValidationResponseDto response = callGateway(
                    fseGatewayClient.validateAndCreate(gatewayRequest));

            log.info("Documento pubblicato su FSE 2.0: requestId={}, traceId={}, workflowInstanceId={}",
                    requestId, response.getTraceId(), response.getWorkflowInstanceId());

            return buildSuccessResponse(requestId, workflow, response,
                    "Documento RSA validato e inviato al Gateway FSE 2.0 per la pubblicazione.");

        } catch (Exception ex) {
            return handleException(requestId, workflow, ex);
        }
    }

    // ─── Pipeline documentale ─────────────────────────────────────────────────

    /**
     * Esegue l'intero pipeline documentale: CDA2 → validazione → PDF/A-3 → firma PAdES.
     * Restituisce il path del PDF firmato E il risultato della generazione CDA
     * (necessario per estrarre l'identificativoDoc nel flusso validate-and-create).
     */
    private PipelineResult runDocumentPipeline(
            String requestId,
            RsaDocument rsaDocument,
            RsaDocumentWorkflowEntity workflow
    ) {
        // 1. Genera CDA2 — usa buildWithId per avere anche gli identificativi
        CdaGenerationResult cdaResult = cdaDocumentXmlBuilder.buildWithId(rsaDocument);
        String cdaXml = cdaResult.cdaXml();

        Path cdaFile = writeCdaToDisk(requestId, cdaXml);
        updateWorkflow(workflow, RsaWorkflowStatus.CDA_GENERATED, w -> w.setCdaFilePath(absPath(cdaFile)));

        // 2. Validazione locale
        cdaXsdValidator.validate(cdaXml);
        cdaSchematronValidator.validate(cdaXml);
        cdaTerminologyValidator.validate(cdaXml);
        updateWorkflow(workflow, RsaWorkflowStatus.CDA_VALIDATED, w -> {});

        // 3. Pipeline PDF
        Path pdfFile = rsaPdfRenderer.render(requestId, cdaXml);
        updateWorkflow(workflow, RsaWorkflowStatus.PDF_RENDERED, w -> w.setPdfFilePath(absPath(pdfFile)));

        Path pdfaFile = rsaPdfa3Converter.convert(requestId, pdfFile);
        updateWorkflow(workflow, RsaWorkflowStatus.PDFA_CREATED, w -> w.setPdfaFilePath(absPath(pdfaFile)));

        Path pdfaEmbedded = rsaPdfa3CdaEmbedder.embed(requestId, pdfaFile, cdaXml);
        updateWorkflow(workflow, RsaWorkflowStatus.CDA_EMBEDDED, w -> w.setPdfaEmbeddedFilePath(absPath(pdfaEmbedded)));

        Path signedPdf = rsaPadesSigner.sign(requestId, pdfaEmbedded);
        updateWorkflow(workflow, RsaWorkflowStatus.SIGNED, w -> w.setSignedPdfFilePath(absPath(signedPdf)));

        log.info("Pipeline documentale completato: requestId={}, paziente={}, docId={}^{}",
                requestId,
                maskFiscalCode(rsaDocument.getPatient().getFiscalCode()),
                cdaResult.documentIdRoot(),
                cdaResult.documentIdExtension());

        return new PipelineResult(signedPdf, cdaResult);
    }

    /** Risultato del pipeline: PDF firmato + metadati CDA generati. */
    private record PipelineResult(Path signedPdf, CdaGenerationResult cdaResult) {}

    // ─── Chiamata Gateway ─────────────────────────────────────────────────────

    private GatewayValidationResponseDto callGateway(
            reactor.core.publisher.Mono<GatewayValidationResponseDto> mono) {
        Duration timeout = Duration.ofSeconds(gatewayProperties.getResponseTimeoutSeconds());
        GatewayValidationResponseDto response = mono.block(timeout);
        if (response == null) {
            throw new GatewayTransportException("Gateway FSE 2.0 ha restituito risposta vuota", null);
        }
        return response;
    }

    // ─── Response builder ─────────────────────────────────────────────────────

    private RsaDocumentIngestionResponse buildSuccessResponse(
            String requestId,
            RsaDocumentWorkflowEntity workflow,
            GatewayValidationResponseDto gatewayResponse,
            String baseMessage
    ) {
        boolean hasWarning = StringUtils.hasText(gatewayResponse.getWarning());

        updateWorkflow(workflow, RsaWorkflowStatus.GATEWAY_VALIDATED, w -> {
            w.setTraceId(gatewayResponse.getTraceId());
            w.setWorkflowInstanceId(gatewayResponse.getWorkflowInstanceId());
            w.setGatewayValidationStatus("OK");
        });

        RsaDocumentIngestionResponse.Status status = hasWarning
                ? RsaDocumentIngestionResponse.Status.GATEWAY_VALIDATED_WITH_WARNINGS
                : RsaDocumentIngestionResponse.Status.GATEWAY_VALIDATED;

        String message = hasWarning
                ? baseMessage + " Warning: " + gatewayResponse.getWarning()
                : baseMessage;

        return RsaDocumentIngestionResponse.builder()
                .requestId(requestId)
                .status(status)
                .message(message)
                .receivedAt(OffsetDateTime.now())
                .traceId(gatewayResponse.getTraceId())
                .workflowInstanceId(gatewayResponse.getWorkflowInstanceId())
                .gatewayWarning(gatewayResponse.getWarning())
                .build();
    }

    // ─── Gestione eccezioni ───────────────────────────────────────────────────

    private RsaDocumentIngestionResponse handleException(
            String requestId, RsaDocumentWorkflowEntity workflow, Exception ex) {
        if (ex instanceof SchematronValidationException
                || ex instanceof CdaValidationException
                || ex instanceof TerminologyValidationException) {
            log.warn("Validazione CDA fallita: requestId={}, causa={}", requestId, ex.getMessage());
            workflow.setErrorMessage(truncate(ex.getMessage(), 1000));
            workflow.setWorkflowStatus(RsaWorkflowStatus.FAILED);
            workflowRepository.save(workflow);
            throw (RuntimeException) ex;
        }
        if (ex instanceof GatewayClientException gce) {
            log.error("Gateway rifiutato: requestId={}, httpStatus={}, traceId={}",
                    requestId, gce.getHttpStatus(), gce.getTraceId());
            workflow.setTraceId(gce.getTraceId());
            workflow.setWorkflowInstanceId(gce.getWorkflowInstanceId());
            workflow.setErrorMessage(truncate(ex.getMessage(), 1000));
            workflow.setWorkflowStatus(RsaWorkflowStatus.GATEWAY_REJECTED);
            workflowRepository.save(workflow);
            throw gce;
        }
        if (ex instanceof GatewayTransportException || ex instanceof GatewayResponseParsingException) {
            log.error("Errore comunicazione Gateway: requestId={}, causa={}", requestId, ex.getMessage());
            workflow.setErrorMessage(truncate(ex.getMessage(), 1000));
            workflow.setWorkflowStatus(RsaWorkflowStatus.FAILED);
            workflowRepository.save(workflow);
            throw (RuntimeException) ex;
        }
        log.error("Errore pipeline RSA: requestId={}", requestId, ex);
        workflow.setErrorMessage(truncate(ex.getMessage(), 1000));
        workflow.setWorkflowStatus(RsaWorkflowStatus.FAILED);
        workflowRepository.save(workflow);
        throw new IllegalStateException("Errore nel pipeline RSA per requestId=" + requestId, ex);
    }

    // ─── Persistenza ──────────────────────────────────────────────────────────

    private RsaDocumentWorkflowEntity createAndSaveWorkflow(
            String requestId, String sourceRequestId,
            String sourceSystem, String patientFiscalCode) {
        RsaDocumentWorkflowEntity entity = new RsaDocumentWorkflowEntity(
                requestId, sourceRequestId, sourceSystem, patientFiscalCode,
                RsaWorkflowStatus.RECEIVED);
        return workflowRepository.save(entity);
    }

    @FunctionalInterface
    private interface WorkflowUpdater { void update(RsaDocumentWorkflowEntity e); }

    private void updateWorkflow(
            RsaDocumentWorkflowEntity workflow, RsaWorkflowStatus status, WorkflowUpdater updater) {
        workflow.setWorkflowStatus(status);
        updater.update(workflow);
        workflowRepository.save(workflow);
    }

    // ─── Utility ──────────────────────────────────────────────────────────────

    private Path writeCdaToDisk(String requestId, String cdaXml) {
        try {
            Path dir = Path.of(storageProperties.getCdaDir()).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path file = dir.resolve(requestId + ".xml");
            Files.writeString(file, cdaXml, StandardCharsets.UTF_8);
            return file;
        } catch (IOException ex) {
            throw new IllegalStateException("Errore salvataggio CDA su disco", ex);
        }
    }

    private String absPath(Path path) {
        return path != null ? path.toAbsolutePath().normalize().toString() : null;
    }

    private String maskFiscalCode(String cf) {
        if (cf == null || cf.length() < 6) return "******";
        return cf.substring(0, 6) + "**********";
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) + "..." : value;
    }
}