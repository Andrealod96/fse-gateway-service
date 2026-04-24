package it.thcs.fse.fsersaservice.application.service;

import it.thcs.fse.fsersaservice.api.dto.request.RsaDocumentIngestionRequest;
import it.thcs.fse.fsersaservice.api.dto.response.RsaDocumentIngestionResponse;
import it.thcs.fse.fsersaservice.application.mapper.RsaDocumentRequestMapper;
import it.thcs.fse.fsersaservice.application.port.in.RsaDocumentIngestionUseCase;
import it.thcs.fse.fsersaservice.config.properties.StorageProperties;
import it.thcs.fse.fsersaservice.domain.model.RsaDocument;
import it.thcs.fse.fsersaservice.infrastructure.cda.CdaDocumentXmlBuilder;
import it.thcs.fse.fsersaservice.infrastructure.pdf.RsaPdfRenderer;
import it.thcs.fse.fsersaservice.infrastructure.pdf.RsaPdfa3CdaEmbedder;
import it.thcs.fse.fsersaservice.infrastructure.pdf.RsaPdfa3Converter;
import it.thcs.fse.fsersaservice.infrastructure.signature.RsaPadesSigner;
import it.thcs.fse.fsersaservice.infrastructure.validation.schematron.CdaSchematronValidator;
import it.thcs.fse.fsersaservice.infrastructure.validation.terminology.CdaTerminologyValidator;
import it.thcs.fse.fsersaservice.infrastructure.validation.xsd.CdaXsdValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final StorageProperties storageProperties;

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
            StorageProperties storageProperties
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
        this.storageProperties = storageProperties;
    }

    @Override
    public RsaDocumentIngestionResponse ingest(RsaDocumentIngestionRequest request) {
        String internalRequestId = request.getSourceRequestId() != null && !request.getSourceRequestId().isBlank()
                ? request.getSourceRequestId()
                : UUID.randomUUID().toString();

        RsaDocument rsaDocument = rsaDocumentRequestMapper.toDomain(request);

        String cdaXml = cdaDocumentXmlBuilder.build(rsaDocument);
        Path cdaFile = writeCdaToDisk(internalRequestId, cdaXml);

        cdaXsdValidator.validate(cdaXml);
        cdaSchematronValidator.validate(cdaXml);
        cdaTerminologyValidator.validate(cdaXml);

        Path pdfFile = rsaPdfRenderer.render(internalRequestId, cdaXml);
        Path pdfaFile = rsaPdfa3Converter.convert(internalRequestId, pdfFile);
        Path pdfaEmbeddedFile = rsaPdfa3CdaEmbedder.embed(internalRequestId, pdfaFile, cdaXml);
        Path signedPdfFile = rsaPadesSigner.sign(internalRequestId, pdfaEmbeddedFile);

        log.info(
                "Richiesta RSA acquisita, mappata, trasformata in CDA, validata localmente, PDF base, PDF/A-3, embedding CDA e firma PAdES completati. requestId={}, sourceSystem={}, patientFiscalCode={}, cdaFile={}, pdfFile={}, pdfaFile={}, pdfaEmbeddedFile={}, signedPdfFile={}, xmlLength={}",
                internalRequestId,
                rsaDocument.getSourceSystem(),
                rsaDocument.getPatient().getFiscalCode(),
                cdaFile.toAbsolutePath(),
                pdfFile.toAbsolutePath(),
                pdfaFile.toAbsolutePath(),
                pdfaEmbeddedFile.toAbsolutePath(),
                signedPdfFile.toAbsolutePath(),
                cdaXml.length()
        );

        return new RsaDocumentIngestionResponse(
                internalRequestId,
                RsaDocumentIngestionResponse.Status.RECEIVED,
                "Richiesta RSA acquisita correttamente, CDA generato, validato localmente, PDF base, PDF/A-3, CDA embedded e PDF firmato prodotti",
                OffsetDateTime.now()
        );
    }

    private Path writeCdaToDisk(String requestId, String cdaXml) {
        try {
            Path cdaDir = Path.of(storageProperties.getCdaDir()).toAbsolutePath().normalize();
            Files.createDirectories(cdaDir);

            Path cdaFile = cdaDir.resolve(requestId + ".xml");
            Files.writeString(cdaFile, cdaXml, StandardCharsets.UTF_8);

            return cdaFile;
        } catch (IOException ex) {
            throw new IllegalStateException("Errore durante il salvataggio del CDA su disco", ex);
        }
    }
}