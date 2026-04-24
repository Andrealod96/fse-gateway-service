package it.thcs.fse.fsersaservice.infrastructure.cda;

import it.thcs.fse.fsersaservice.domain.model.RsaDocument;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class CdaDocumentXmlBuilder {

    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssZ");

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
        OffsetDateTime generationTime = OffsetDateTime.now(ZoneId.of("Europe/Rome")).withNano(0);
        String generationTimeValue = TS_FORMATTER.format(generationTime);
        String documentIdExtension = buildDocumentIdExtension(rsaDocument, generationTimeValue);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append(cdaHeaderFragmentBuilder.build(rsaDocument, documentIdExtension, generationTimeValue));
        xml.append(cdaPartiesAndContextFragmentBuilder.build(rsaDocument, documentIdExtension, generationTimeValue));
        xml.append(cdaStructuredBodyFragmentBuilder.build(rsaDocument));
        xml.append("</ClinicalDocument>");

        return xml.toString();
    }

    private String buildDocumentIdExtension(RsaDocument rsaDocument, String generationTimeValue) {
        String patientFiscalCode = rsaDocument.getPatient().getFiscalCode();
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return patientFiscalCode + "." + generationTimeValue.substring(0, 14) + "." + randomSuffix;
    }
}