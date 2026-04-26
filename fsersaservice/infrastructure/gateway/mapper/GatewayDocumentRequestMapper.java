package it.thcs.fse.fsersaservice.infrastructure.gateway.mapper;

import it.thcs.fse.fsersaservice.api.dto.request.RsaDocumentIngestionRequest;
import it.thcs.fse.fsersaservice.config.properties.JwtProperties;
import it.thcs.fse.fsersaservice.domain.model.RsaDocument;
import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.request.GatewayDocumentValidationRequest;
import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.request.GatewayRequestBodyDto;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class GatewayDocumentRequestMapper {

    private static final String HEALTH_DATA_FORMAT    = "CDA";
    private static final String MODE                  = "ATTACHMENT";
    private static final String ACTIVITY_VALIDATION   = "VALIDATION";
    private static final String RESOURCE_HL7_TYPE_RSA = "('11488-4^^2.16.840.1.113883.6.1')";
    private static final String ACTION_ID_CREATE      = "CREATE";

    private final JwtProperties jwtProperties;

    public GatewayDocumentRequestMapper(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    // ─── Validazione semplice ─────────────────────────────────────────────────

    /**
     * Costruisce la request per POST /v1/documents/validation.
     * Non include metadati XDS: non sono necessari su questo endpoint.
     */
    public GatewayDocumentValidationRequest toValidationRequest(
            RsaDocument rsaDocument,
            Path signedPdfFile
    ) {
        requireDocument(rsaDocument);

        GatewayRequestBodyDto requestBody = GatewayRequestBodyDto.builder()
                .healthDataFormat(HEALTH_DATA_FORMAT)
                .mode(MODE)
                .activity(ACTIVITY_VALIDATION)
                .build();

        return buildRequest(rsaDocument, signedPdfFile, requestBody, false);
    }

    // ─── Validate-and-create ──────────────────────────────────────────────────

    /**
     * Costruisce la request per POST /v1/documents/validate-and-create.
     *
     * Il parametro {@code identificativoDocReale} è l'ID del documento estratto
     * dal CDA generato, nel formato XDS {@code root^extension}.
     * Il Gateway verifica che corrisponda al ClinicalDocument/id nel PDF:
     * usare qualsiasi altro valore causerebbe un 400 "/msg/id-doc".
     *
     * Il campo {@code activity} NON va inserito su questo endpoint.
     * Il flag {@code includeAttachmentHash} è true per consentire la verifica
     * di integrità del CDA da parte del Gateway.
     *
     * @param rsaDocument            dominio del documento
     * @param signedPdfFile          PDF/A-3 firmato con CDA embedded
     * @param pubblicazione          metadati XDS forniti dal chiamante
     * @param identificativoDocReale ID reale estratto dal CDA (root^extension)
     */
    public GatewayDocumentValidationRequest toValidateAndCreateRequest(
            RsaDocument rsaDocument,
            Path signedPdfFile,
            RsaDocumentIngestionRequest.PubblicazioneDto pubblicazione,
            String identificativoDocReale
    ) {
        requireDocument(rsaDocument);
        requirePubblicazione(pubblicazione);

        if (!StringUtils.hasText(identificativoDocReale)) {
            throw new IllegalArgumentException(
                    "identificativoDocReale obbligatorio per validate-and-create");
        }

        GatewayRequestBodyDto requestBody = GatewayRequestBodyDto.builder()
                .healthDataFormat(HEALTH_DATA_FORMAT)
                .mode(MODE)
                // activity NON va impostato su validate-and-create
                // identificativoDoc usa il valore reale del CDA, non quello della request
                .identificativoDoc(identificativoDocReale)
                .identificativoRep(pubblicazione.getIdentificativoRep())
                .tipologiaStruttura(pubblicazione.getTipologiaStruttura())
                .tipoDocumentoLivAlto(pubblicazione.getTipoDocumentoLivAlto())
                .assettoOrganizzativo(pubblicazione.getAssettoOrganizzativo())
                .tipoAttivitaClinica(pubblicazione.getTipoAttivitaClinica())
                .identificativoSottomissione(pubblicazione.getIdentificativoSottomissione())
                .attiCliniciRegoleAccesso(nullIfEmpty(pubblicazione.getAttiCliniciRegoleAccesso()))
                .dataInizioPrestazione(pubblicazione.getDataInizioPrestazione())
                .dataFinePrestazione(pubblicazione.getDataFinePrestazione())
                .conservazioneANorma(pubblicazione.getConservazioneANorma())
                .descriptions(nullIfEmpty(pubblicazione.getDescriptions()))
                .administrativeRequest(nullIfEmpty(pubblicazione.getAdministrativeRequest()))
                .build();

        return buildRequest(rsaDocument, signedPdfFile, requestBody, true);
    }

    // ─── Builder comune ───────────────────────────────────────────────────────

    private GatewayDocumentValidationRequest buildRequest(
            RsaDocument rsaDocument,
            Path signedPdfFile,
            GatewayRequestBodyDto requestBody,
            boolean includeAttachmentHash
    ) {
        byte[] fileBytes = readFileBytes(signedPdfFile);
        String fileName  = signedPdfFile.getFileName().toString();

        String subjectRole = jwtProperties.getSubjectRole();
        if (!StringUtils.hasText(subjectRole)) {
            throw new IllegalStateException("fse.jwt.subject-role non configurato");
        }

        return GatewayDocumentValidationRequest.builder()
                .requestBody(requestBody)
                .fileBytes(fileBytes)
                .fileName(fileName)
                .fileContentType("application/pdf")
                .practitionerFiscalCode(rsaDocument.getAuthor().getFiscalCode())
                .patientFiscalCode(rsaDocument.getPatient().getFiscalCode())
                .subjectRole(subjectRole)
                .actionId(ACTION_ID_CREATE)
                .resourceHl7Type(RESOURCE_HL7_TYPE_RSA)
                .patientConsent(Boolean.TRUE)
                .includeAttachmentHash(includeAttachmentHash)
                .build();
    }

    // ─── Utility ─────────────────────────────────────────────────────────────

    private byte[] readFileBytes(Path file) {
        try {
            if (file == null || !Files.exists(file)) {
                throw new IllegalArgumentException("File PDF firmato non trovato: " + file);
            }
            return Files.readAllBytes(file);
        } catch (IOException ex) {
            throw new IllegalStateException("Impossibile leggere il file PDF firmato da " + file, ex);
        }
    }

    private void requireDocument(RsaDocument rsaDocument) {
        if (rsaDocument == null) throw new IllegalArgumentException("RsaDocument nullo");
    }

    private void requirePubblicazione(RsaDocumentIngestionRequest.PubblicazioneDto p) {
        if (p == null) throw new IllegalArgumentException(
                "PubblicazioneDto nullo: metadati XDS obbligatori per validate-and-create");
    }

    private <T> List<T> nullIfEmpty(List<T> list) {
        return (list == null || list.isEmpty()) ? null : list;
    }
}