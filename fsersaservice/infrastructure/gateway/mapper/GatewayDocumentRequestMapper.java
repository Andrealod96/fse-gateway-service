package it.thcs.fse.fsersaservice.infrastructure.gateway.mapper;

import it.thcs.fse.fsersaservice.config.properties.JwtProperties;
import it.thcs.fse.fsersaservice.domain.model.RsaDocument;
import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.request.GatewayDocumentValidationRequest;
import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.request.GatewayRequestBodyDto;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class GatewayDocumentRequestMapper {

    private static final String HEALTH_DATA_FORMAT = "CDA";
    private static final String MODE = "ATTACHMENT";
    private static final String ACTIVITY_VALIDATION = "VALIDATION";
    private static final String ACTION_ID_CREATE = "CREATE";

    /**
     * RSA = Referto di Specialistica Ambulatoriale
     * LOINC 11488-4 come da tracciato CDA RSA ufficiale.
     */
    private static final String RESOURCE_HL7_TYPE_RSA = "('11488-4^^2.16.840.1.113883.6.1')";

    private final JwtProperties jwtProperties;

    public GatewayDocumentRequestMapper(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public GatewayDocumentValidationRequest toValidationRequest(
            RsaDocument rsaDocument,
            Path signedPdfFile
    ) {
        if (rsaDocument == null) {
            throw new IllegalArgumentException("RsaDocument nullo");
        }

        byte[] fileBytes = readFileBytes(signedPdfFile);
        String fileName = signedPdfFile.getFileName().toString();

        GatewayRequestBodyDto requestBody = GatewayRequestBodyDto.builder()
                .healthDataFormat(HEALTH_DATA_FORMAT)
                .mode(MODE)
                .activity(ACTIVITY_VALIDATION)
                .build();

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
                .includeAttachmentHash(false)
                .build();
    }

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
}