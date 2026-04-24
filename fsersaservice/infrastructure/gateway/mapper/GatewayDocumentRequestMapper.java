package it.thcs.fse.fsersaservice.infrastructure.gateway.mapper;

import it.thcs.fse.fsersaservice.config.properties.GatewayProperties;
import it.thcs.fse.fsersaservice.domain.model.RsaDocument;
import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.request.GatewayDocumentValidationRequest;
import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.request.GatewayRequestBodyDto;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Mapper che costruisce la {@link GatewayDocumentValidationRequest} da inviare al Gateway FSE 2.0.
 *
 * Responsabilità:
 * - leggere il file PDF firmato dal disco
 * - valorizzare il requestBody con i campi obbligatori per /v1/documents/validation
 * - valorizzare i metadati JWT (CF professionista, CF paziente, ruolo, ecc.)
 *
 * Nota sul requestBody per /v1/documents/validation:
 * La documentazione ufficiale (it-fse-support/doc/integrazione-gateway/README.md)
 * richiede solo healthDataFormat, mode, activity per la validazione pura.
 * I metadati XDS (identificativoDoc, tipoDocumentoLivAlto, ecc.) sono necessari
 * solo per /v1/documents e /v1/documents/validate-and-create.
 */
@Component
public class GatewayDocumentRequestMapper {

    private static final String HEALTH_DATA_FORMAT = "CDA";
    private static final String MODE = "ATTACHMENT";
    private static final String ACTIVITY_VALIDATION = "VALIDATION";

    /**
     * resource_hl7_type per RSA — Referto Specialistica Ambulatoriale.
     * Formato: ('LOINC_CODE^^OID_LOINC')
     */
    private static final String RESOURCE_HL7_TYPE_RSA = "('11488-4^^2.16.840.1.113883.6.1')";

    private final GatewayProperties gatewayProperties;

    public GatewayDocumentRequestMapper(GatewayProperties gatewayProperties) {
        this.gatewayProperties = gatewayProperties;
    }

    /**
     * Costruisce la request di validazione per il Gateway FSE 2.0.
     *
     * @param rsaDocument   dominio del documento RSA, contiene i dati clinici e anagrafici
     * @param signedPdfFile path del PDF/A-3 firmato PAdES da inviare
     * @return request completa per {@code FseGatewayClient.validateDocument()}
     */
    public GatewayDocumentValidationRequest toValidationRequest(
            RsaDocument rsaDocument,
            Path signedPdfFile
    ) {
        byte[] fileBytes = readFileBytes(signedPdfFile);
        String fileName = signedPdfFile.getFileName().toString();

        GatewayRequestBodyDto requestBody = GatewayRequestBodyDto.builder()
                .healthDataFormat(HEALTH_DATA_FORMAT)
                .mode(MODE)
                .activity(ACTIVITY_VALIDATION)
                .build();

        return GatewayDocumentValidationRequest.builder()
                .requestBody(requestBody)
                .fileBytes(fileBytes)
                .fileName(fileName)
                // CF professionista: l'autore del documento clinico
                .practitionerFiscalCode(rsaDocument.getAuthor().getFiscalCode())
                // CF paziente: il soggetto del documento
                .patientFiscalCode(rsaDocument.getPatient().getFiscalCode())
                // Ruolo del professionista, configurabile via fse.gateway.default-subject-role
                .subjectRole(gatewayProperties.getDefaultSubjectRole())
                // resource_hl7_type fisso per RSA
                .resourceHl7Type(RESOURCE_HL7_TYPE_RSA)
                // Per validazione: CREATE
                .actionId("CREATE")
                // Consenso paziente: true per default
                .patientConsent(Boolean.TRUE)
                // attachment_hash non necessario per /documents/validation
                .includeAttachmentHash(false)
                .build();
    }

    private byte[] readFileBytes(Path file) {
        try {
            if (file == null || !Files.exists(file)) {
                throw new IllegalArgumentException(
                        "File PDF firmato non trovato: " + file);
            }
            return Files.readAllBytes(file);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Impossibile leggere il file PDF firmato da " + file, ex);
        }
    }
}