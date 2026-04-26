package it.thcs.fse.fsersaservice.infrastructure.cda;

import it.thcs.fse.fsersaservice.config.properties.CdaProperties;
import it.thcs.fse.fsersaservice.config.properties.OidProperties;
import it.thcs.fse.fsersaservice.domain.model.RsaDocument;
import it.thcs.fse.fsersaservice.support.util.XmlEscaper;
import org.springframework.stereotype.Component;

@Component
public class CdaHeaderFragmentBuilder {

    private final CdaProperties cdaProperties;
    private final OidProperties oidProperties;

    public CdaHeaderFragmentBuilder(CdaProperties cdaProperties, OidProperties oidProperties) {
        this.cdaProperties = cdaProperties;
        this.oidProperties = oidProperties;
    }

    public String build(RsaDocument rsaDocument, String documentIdExtension, String generationTimeValue) {
        StringBuilder xml = new StringBuilder();

        xml.append("<ClinicalDocument xsi:schemaLocation=\"urn:hl7-org:v3 CDA.xsd\" ")
                .append("xmlns=\"urn:hl7-org:v3\" ")
                .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
                .append("xmlns:sdtc=\"urn:hl7-org:sdtc\">");

        xml.append("<realmCode code=\"")
                .append(XmlEscaper.escapeAttribute(cdaProperties.getRealmCode()))
                .append("\"/>");

        xml.append("<typeId root=\"")
                .append(XmlEscaper.escapeAttribute(cdaProperties.getTypeIdRoot()))
                .append("\" extension=\"")
                .append(XmlEscaper.escapeAttribute(cdaProperties.getTypeIdExtension()))
                .append("\"/>");

        xml.append("<templateId root=\"")
                .append(XmlEscaper.escapeAttribute(cdaProperties.getTemplateIdRoot()))
                .append("\" extension=\"")
                .append(XmlEscaper.escapeAttribute(cdaProperties.getTemplateIdExtension()))
                .append("\" assigningAuthorityName=\"HL7 Italia\"/>");

        xml.append("<id root=\"")
                .append(XmlEscaper.escapeAttribute(oidProperties.getPugliaCdaDocumentRoot()))
                .append("\" extension=\"")
                .append(XmlEscaper.escapeAttribute(documentIdExtension))
                .append("\" assigningAuthorityName=\"Regione Lazio\"/>");

        xml.append("<code code=\"")
                .append(XmlEscaper.escapeAttribute(cdaProperties.getDocumentCodeRsa()))
                .append("\" codeSystem=\"")
                .append(XmlEscaper.escapeAttribute(cdaProperties.getCodeSystemLoinc()))
                .append("\" codeSystemName=\"LOINC\" displayName=\"")
                .append(XmlEscaper.escapeAttribute("Nota di consulto"))
                .append("\">");
        xml.append("</code>");

        xml.append("<title>")
                .append(XmlEscaper.escapeText(cdaProperties.getDocumentTitle()))
                .append("</title>");

        xml.append("<sdtc:statusCode code=\"active\"/>");

        xml.append("<effectiveTime value=\"")
                .append(XmlEscaper.escapeAttribute(generationTimeValue))
                .append("\"/>");

        xml.append("<confidentialityCode code=\"N\" codeSystem=\"")
                .append(XmlEscaper.escapeAttribute(cdaProperties.getConfidentialityCodeSystem()))
                .append("\" codeSystemName=\"HL7 Confidentiality\" displayName=\"Normal\"/>");

        xml.append("<languageCode code=\"")
                .append(XmlEscaper.escapeAttribute(cdaProperties.getLanguageCode()))
                .append("\"/>");

        xml.append("<setId root=\"")
                .append(XmlEscaper.escapeAttribute(oidProperties.getPugliaCdaDocumentRoot()))
                .append("\" extension=\"")
                .append(XmlEscaper.escapeAttribute(documentIdExtension))
                .append("\" assigningAuthorityName=\"Regione Lazio\"/>");

        xml.append("<versionNumber value=\"1\"/>");

        return xml.toString();
    }
}