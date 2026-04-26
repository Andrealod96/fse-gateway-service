package it.thcs.fse.fsersaservice.infrastructure.cda;

import it.thcs.fse.fsersaservice.domain.model.RsaDocument;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Component
public class CdaDocumentXmlBuilder {

    private static final ZoneId ROME_ZONE_ID = ZoneId.of("Europe/Rome");
    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssZ");
    private static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    private final CdaHeaderFragmentBuilder cdaHeaderFragmentBuilder;
    private final CdaPartiesAndContextFragmentBuilder cdaPartiesAndContextFragmentBuilder;
    private final CdaStructuredBodyFragmentBuilder cdaStructuredBodyFragmentBuilder;

    public CdaDocumentXmlBuilder(
            CdaHeaderFragmentBuilder cdaHeaderFragmentBuilder,
            CdaPartiesAndContextFragmentBuilder cdaPartiesAndContextFragmentBuilder,
            CdaStructuredBodyFragmentBuilder cdaStructuredBodyFragmentBuilder
    ) {
        this.cdaHeaderFragmentBuilder = cdaHeaderFragmentBuilder;
        this.cdaPartiesAndContextFragmentBuilder = cdaPartiesAndContextFragmentBuilder;
        this.cdaStructuredBodyFragmentBuilder = cdaStructuredBodyFragmentBuilder;
    }

    public String build(RsaDocument rsaDocument) {
        validateInput(rsaDocument);

        OffsetDateTime generationTime = OffsetDateTime.now(ROME_ZONE_ID).withNano(0);
        String generationTimeValue = TS_FORMATTER.format(generationTime);
        String documentIdExtension = buildDocumentIdExtension(rsaDocument, generationTimeValue);

        StringBuilder xml = new StringBuilder(16_384);
        xml.append(XML_DECLARATION);
        xml.append(cdaHeaderFragmentBuilder.build(rsaDocument, documentIdExtension, generationTimeValue));
        xml.append(cdaPartiesAndContextFragmentBuilder.build(rsaDocument, documentIdExtension, generationTimeValue));
        xml.append(cdaStructuredBodyFragmentBuilder.build(rsaDocument));
        xml.append("</ClinicalDocument>");

        return xml.toString();
    }

    private void validateInput(RsaDocument rsaDocument) {
        if (rsaDocument == null) {
            throw new IllegalArgumentException("RsaDocument obbligatorio");
        }
        if (rsaDocument.getPatient() == null) {
            throw new IllegalArgumentException("Patient obbligatorio per la generazione del CDA");
        }
        if (!StringUtils.hasText(rsaDocument.getPatient().getFiscalCode())) {
            throw new IllegalArgumentException("Patient.fiscalCode obbligatorio per la generazione del CDA");
        }
    }

    private String buildDocumentIdExtension(RsaDocument rsaDocument, String generationTimeValue) {
        String patientFiscalCode = normalizeFiscalCode(rsaDocument.getPatient().getFiscalCode());
        String timestampPart = generationTimeValue.substring(0, 14);
        String randomSuffix = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase(Locale.ROOT);

        return patientFiscalCode + "." + timestampPart + "." + randomSuffix;
    }

    private String normalizeFiscalCode(String fiscalCode) {
        return fiscalCode == null ? null : fiscalCode.trim().toUpperCase(Locale.ROOT);
    }
}