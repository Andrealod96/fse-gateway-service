package it.thcs.fse.fsersaservice.application.service;

import it.thcs.fse.fsersaservice.api.dto.request.RsaDocumentIngestionRequest;
import it.thcs.fse.fsersaservice.api.dto.response.RsaDocumentIngestionResponse;
import it.thcs.fse.fsersaservice.application.mapper.RsaDocumentRequestMapper;
import it.thcs.fse.fsersaservice.application.port.in.RsaDocumentIngestionUseCase;
import it.thcs.fse.fsersaservice.config.properties.GatewayProperties;
import it.thcs.fse.fsersaservice.config.properties.StorageProperties;
import it.thcs.fse.fsersaservice.domain.model.RsaDocument;
import it.thcs.fse.fsersaservice.infrastructure.cda.CdaDocumentXmlBuilder;
import it.thcs.fse.fsersaservice.infrastructure.gateway.client.FseGatewayClient;
import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.request.GatewayDocumentValidationRequest;
import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.response.GatewayValidationResponseDto;
import it.thcs.fse.fsersaservice.infrastructure.gateway.exception.GatewayClientException;
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
import it.thcs.fse.fsersaservice.infrastructure.gateway.exception.GatewayResponseParsingException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class RsaDocumentIngestionApplicationService implements RsaDocumentIngestionUseCase {

    private static final Logger log = LoggerFactory.getLogger(RsaDocumentIngestionApplicationService.class);

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

    @Override
    @Transactional
    public RsaDocumentIngestionResponse ingest(RsaDocumentIngestionRequest request) {

        // requestId interno: sempre UUID generato qui — non coincide mai con sourceRequestId del chiamante.
        // Separazione degli identificativi:
        //   requestId (interno) | sourceRequestId (PHP) | workflowInstanceId (Gateway) | ID documento CDA
        String internalRequestId = resolveRequestId();

        // 1. Mappa la request nel dominio
        RsaDocument rsaDocument = rsaDocumentRequestMapper.toDomain(request);

        // 2. Persisti il record iniziale con stato RECEIVED.
        //    sourceRequestId viene salvato nella sua colonna separata.
        RsaDocumentWorkflowEntity workflow = createAndSaveWorkflow(
                internalRequestId,
                request.getSourceRequestId(),
                rsaDocument.getSourceSystem(),
                rsaDocument.getPatient().getFiscalCode()
        );

        try {
            // 3. Genera CDA2 XML
            String cdaXml = cdaDocumentXmlBuilder.build(rsaDocument);
            Path cdaFile = writeCdaToDisk(internalRequestId, cdaXml);
            updateWorkflow(workflow, RsaWorkflowStatus.CDA_GENERATED, w -> w.setCdaFilePath(absPath(cdaFile)));

            // 4. Validazione locale CDA2 (XSD + Schematron RSA + Terminologia)
            cdaXsdValidator.validate(cdaXml);
            cdaSchematronValidator.validate(cdaXml);
            cdaTerminologyValidator.validate(cdaXml);
            updateWorkflow(workflow, RsaWorkflowStatus.CDA_VALIDATED, w -> {});

            // 5. Pipeline PDF: render → PDF/A-3 → embed CDA → firma PAdES
            Path pdfFile = rsaPdfRenderer.render(internalRequestId, cdaXml);
            updateWorkflow(workflow, RsaWorkflowStatus.PDF_RENDERED, w -> w.setPdfFilePath(absPath(pdfFile)));

            Path pdfaFile = rsaPdfa3Converter.convert(internalRequestId, pdfFile);
            updateWorkflow(workflow, RsaWorkflowStatus.PDFA_CREATED, w -> w.setPdfaFilePath(absPath(pdfaFile)));

            Path pdfaEmbeddedFile = rsaPdfa3CdaEmbedder.embed(internalRequestId, pdfaFile, cdaXml);
            updateWorkflow(workflow, RsaWorkflowStatus.CDA_EMBEDDED, w -> w.setPdfaEmbeddedFilePath(absPath(pdfaEmbeddedFile)));

            Path signedPdfFile = rsaPadesSigner.sign(internalRequestId, pdfaEmbeddedFile);
            updateWorkflow(workflow, RsaWorkflowStatus.SIGNED, w -> w.setSignedPdfFilePath(absPath(signedPdfFile)));

            log.info("Pipeline documentale completato: requestId={}, paziente={}, signedPdf={}",
                    internalRequestId,
                    maskFiscalCode(rsaDocument.getPatient().getFiscalCode()),
                    signedPdfFile.getFileName());

            // 6. Costruisci request Gateway e registra l'invio
            GatewayDocumentValidationRequest gatewayRequest =
                    gatewayDocumentRequestMapper.toValidationRequest(rsaDocument, signedPdfFile);

            updateWorkflow(workflow, RsaWorkflowStatus.GATEWAY_SUBMITTED, w -> {});

            // 7. Invia al Gateway FSE 2.0 — se risponde con errore HTTP lancia GatewayClientException
            GatewayValidationResponseDto gatewayResponse = submitToGateway(gatewayRequest);

            // 8. Risposta positiva: salva traceId, workflowInstanceId e stato GATEWAY_VALIDATED
            updateWorkflow(workflow, RsaWorkflowStatus.GATEWAY_VALIDATED, w -> {
                w.setTraceId(gatewayResponse.getTraceId());
                w.setWorkflowInstanceId(gatewayResponse.getWorkflowInstanceId());
                w.setGatewayValidationStatus("OK");
            });

            log.info("Gateway FSE 2.0 validazione OK: requestId={}, traceId={}, workflowInstanceId={}",
                    internalRequestId,
                    gatewayResponse.getTraceId(),
                    gatewayResponse.getWorkflowInstanceId());

            return new RsaDocumentIngestionResponse(
                    internalRequestId,
                    RsaDocumentIngestionResponse.Status.GATEWAY_VALIDATED,
                    "Documento RSA validato dal Gateway FSE 2.0. workflowInstanceId: "
                            + gatewayResponse.getWorkflowInstanceId(),
                    OffsetDateTime.now()
            );

        } catch (GatewayClientException ex) {
            log.error("Gateway FSE 2.0 ha rifiutato il documento: requestId={}, httpStatus={}, traceId={}, body={}",
                    internalRequestId, ex.getHttpStatus(), ex.getTraceId(), ex.getResponseBody());

            workflow.setGatewayValidationStatus("KO");
            workflow.setTraceId(ex.getTraceId());
            workflow.setWorkflowInstanceId(ex.getWorkflowInstanceId());
            workflow.setErrorMessage("Gateway HTTP " + ex.getHttpStatus() + ": " + truncate(ex.getResponseBody(), 1000));
            workflow.setWorkflowStatus(RsaWorkflowStatus.GATEWAY_REJECTED);
            workflowRepository.save(workflow);
            throw ex;

        } catch (GatewayTransportException ex) {
            log.error("Errore di trasporto verso Gateway FSE 2.0: requestId={}, causa={}",
                    internalRequestId, ex.getMessage());

            workflow.setErrorMessage("Errore di trasporto Gateway: " + truncate(ex.getMessage(), 1000));
            workflow.setWorkflowStatus(RsaWorkflowStatus.FAILED);
            workflowRepository.save(workflow);
            throw ex;

        } catch (GatewayResponseParsingException ex) {
            log.error("Risposta Gateway FSE 2.0 non parsabile o incoerente: requestId={}, httpStatus={}, body={}",
                    internalRequestId, ex.getHttpStatus(), ex.getResponseBody());

            workflow.setErrorMessage("Risposta Gateway non parsabile: " + truncate(ex.getMessage(), 1000));
            workflow.setWorkflowStatus(RsaWorkflowStatus.FAILED);
            workflowRepository.save(workflow);
            throw ex;

        } catch (Exception ex) {
            log.error("Errore nel pipeline RSA: requestId={}, causa={}", internalRequestId, ex.getMessage(), ex);

            workflow.setErrorMessage(truncate(ex.getMessage(), 1000));
            workflow.setWorkflowStatus(RsaWorkflowStatus.FAILED);
            workflowRepository.save(workflow);
            throw new IllegalStateException("Errore nel pipeline RSA per requestId=" + internalRequestId, ex);
        }
    }

    // ─── Persistenza ──────────────────────────────────────────────────────────

    private RsaDocumentWorkflowEntity createAndSaveWorkflow(
            String requestId,
            String sourceRequestId,
            String sourceSystem,
            String patientFiscalCode
    ) {
        RsaDocumentWorkflowEntity entity = new RsaDocumentWorkflowEntity(
                requestId, sourceRequestId, sourceSystem, patientFiscalCode,
                RsaWorkflowStatus.RECEIVED
        );
        return workflowRepository.save(entity);
    }

    @FunctionalInterface
    private interface WorkflowUpdater {
        void update(RsaDocumentWorkflowEntity entity);
    }

    private void updateWorkflow(
            RsaDocumentWorkflowEntity workflow,
            RsaWorkflowStatus status,
            WorkflowUpdater updater
    ) {
        workflow.setWorkflowStatus(status);
        updater.update(workflow);
        workflowRepository.save(workflow);
    }

    // ─── Chiamata Gateway ─────────────────────────────────────────────────────

    /**
     * Invia la request al Gateway in modo sincrono (block).
     * Il timeout è allineato con fse.gateway.response-timeout-seconds.
     * L'intero service è sincrono/bloccante (PDFBox, DSS, JPA) e il controller
     * esegue già su Schedulers.boundedElastic(), quindi block() è coerente.
     */
    private GatewayValidationResponseDto submitToGateway(GatewayDocumentValidationRequest request) {
        Duration timeout = Duration.ofSeconds(gatewayProperties.getResponseTimeoutSeconds());

        GatewayValidationResponseDto response = fseGatewayClient
                .validateDocument(request)
                .block(timeout);

        if (response == null) {
            throw new GatewayTransportException(
                    "Il Gateway FSE 2.0 ha restituito una risposta vuota", null);
        }

        return response;
    }

    // ─── Utility ──────────────────────────────────────────────────────────────

    /**
     * Genera sempre un UUID come requestId interno del microservizio.
     * Non usa mai sourceRequestId come requestId: gli identificativi restano separati.
     */
    private String resolveRequestId() {
        return UUID.randomUUID().toString();
    }

    private Path writeCdaToDisk(String requestId, String cdaXml) {
        try {
            Path cdaDir = Path.of(storageProperties.getCdaDir()).toAbsolutePath().normalize();
            Files.createDirectories(cdaDir);
            Path cdaFile = cdaDir.resolve(requestId + ".xml");
            Files.writeString(cdaFile, cdaXml, StandardCharsets.UTF_8);
            return cdaFile;
        } catch (IOException ex) {
            throw new IllegalStateException("Errore salvataggio CDA su disco", ex);
        }
    }

    private String absPath(Path path) {
        return path != null ? path.toAbsolutePath().normalize().toString() : null;
    }

    /**
     * Maschera il CF nei log — espone solo i primi 6 caratteri.
     * Es: RSSMRA80A01H501U → RSSMRA**********
     */
    private String maskFiscalCode(String cf) {
        if (cf == null || cf.length() < 6) return "******";
        return cf.substring(0, 6) + "**********";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) + "..." : value;
    }
}