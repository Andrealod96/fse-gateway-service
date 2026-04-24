package it.thcs.fse.fsersaservice.infrastructure.cda;

import it.thcs.fse.fsersaservice.config.properties.CdaProperties;
import it.thcs.fse.fsersaservice.domain.model.RsaDocument;
import it.thcs.fse.fsersaservice.support.util.XmlEscaper;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class CdaStructuredBodyFragmentBuilder {

    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssZ");

    private final CdaProperties cdaProperties;

    public CdaStructuredBodyFragmentBuilder(CdaProperties cdaProperties) {
        this.cdaProperties = cdaProperties;
    }

    public String build(RsaDocument rsaDocument) {
        StringBuilder xml = new StringBuilder();

        xml.append("<component>");
        xml.append("<structuredBody moodCode=\"EVN\" classCode=\"DOCBODY\">");

        appendQuesitoDiagnostico(xml, rsaDocument.getClinicalContent().getQuesitoDiagnostico());
        appendStoriaClinica(xml, rsaDocument.getClinicalContent().getStoriaClinica());
        appendPrestazioni(xml, rsaDocument.getClinicalContent().getPrestazioni());
        appendConfrontoPrecedentiEsami(xml, rsaDocument.getClinicalContent().getConfrontoPrecedentiEsami());
        appendReferto(xml, rsaDocument.getClinicalContent().getRefertoText());
        appendDiagnosi(xml, rsaDocument.getClinicalContent().getDiagnosi());
        appendConclusioni(xml, rsaDocument.getClinicalContent().getConclusioni());
        appendSuggerimenti(xml, rsaDocument.getClinicalContent().getSuggerimentiMedicoPrescrittore());

        xml.append("</structuredBody>");
        xml.append("</component>");

        return xml.toString();
    }

    private void appendQuesitoDiagnostico(StringBuilder xml, RsaDocument.QuesitoDiagnostico quesito) {
        if (quesito == null) {
            return;
        }

        xml.append("<component typeCode=\"COMP\">");
        xml.append("<section ID=\"QUESITO_DIAGNOSTICO\">");
        xml.append("<code code=\"29299-5\" codeSystem=\"")
                .append(XmlEscaper.escapeAttribute(cdaProperties.getCodeSystemLoinc()))
                .append("\" codeSystemName=\"LOINC\" displayName=\"Motivo della visita\"/>");
        xml.append("<title> Quesito diagnostico </title>");
        xml.append("<text><paragraph>")
                .append(XmlEscaper.escapeText(quesito.getText()))
                .append("</paragraph></text>");
        xml.append("<entry>");
        xml.append("<observation classCode=\"OBS\" moodCode=\"EVN\">");
        xml.append("<code code=\"29298-7\" codeSystem=\"")
                .append(XmlEscaper.escapeAttribute(cdaProperties.getCodeSystemLoinc()))
                .append("\" codeSystemName=\"LOINC\" displayName=\"Motivo della visita\"/>");
        appendCodingValue(xml, quesito.getCoding());
        xml.append("</observation>");
        xml.append("</entry>");
        xml.append("</section>");
        xml.append("</component>");
    }

    private void appendStoriaClinica(StringBuilder xml, String storiaClinica) {
        if (!XmlEscaper.hasText(storiaClinica)) {
            return;
        }

        xml.append("<component typeCode=\"COMP\">");
        xml.append("<section ID=\"STORIA_CLINICA\">");
        xml.append("<code code=\"11329-0\" codeSystem=\"")
                .append(XmlEscaper.escapeAttribute(cdaProperties.getCodeSystemLoinc()))
                .append("\" codeSystemName=\"LOINC\" displayName=\"Storia Generale\"/>");
        xml.append("<title> Storia Clinica </title>");
        xml.append("<text><paragraph>")
                .append(XmlEscaper.escapeText(storiaClinica))
                .append("</paragraph></text>");
        xml.append("</section>");
        xml.append("</component>");
    }

    private void appendPrestazioni(StringBuilder xml, List<RsaDocument.Prestazione> prestazioni) {
        xml.append("<component typeCode=\"COMP\">");
        xml.append("<section ID=\"PRESTAZIONI\">");
        xml.append("<code code=\"62387-6\" codeSystem=\"")
                .append(XmlEscaper.escapeAttribute(cdaProperties.getCodeSystemLoinc()))
                .append("\" codeSystemName=\"LOINC\" displayName=\"Interventi\"/>");
        xml.append("<title> Prestazioni </title>");

        xml.append("<text><list>");
        int index = 1;
        for (RsaDocument.Prestazione prestazione : prestazioni) {
            xml.append("<item ID=\"Prestazione-").append(index).append("\">");
            xml.append("<content>");
            xml.append(XmlEscaper.escapeText(prestazione.getText()));
            xml.append(" - ");
            xml.append(XmlEscaper.escapeText(prestazione.getCoding().getCode()));
            xml.append(" - ");
            xml.append(XmlEscaper.escapeText(formatTs(prestazione.getPerformedAt())));
            xml.append("</content>");
            xml.append("</item>");
            index++;
        }
        xml.append("</list></text>");

        for (RsaDocument.Prestazione prestazione : prestazioni) {
            xml.append("<entry>");
            xml.append("<act classCode=\"ACT\" moodCode=\"EVN\">");
            appendCode(xml, prestazione.getCoding());
            xml.append("<effectiveTime value=\"")
                    .append(XmlEscaper.escapeAttribute(formatTs(prestazione.getPerformedAt())))
                    .append("\"/>");
            xml.append("<entryRelationship typeCode=\"COMP\">");
            xml.append("<procedure classCode=\"PROC\" moodCode=\"EVN\">");
            appendCode(xml, prestazione.getCoding());
            xml.append("<effectiveTime value=\"")
                    .append(XmlEscaper.escapeAttribute(formatTs(prestazione.getPerformedAt())))
                    .append("\"/>");
            xml.append("</procedure>");
            xml.append("</entryRelationship>");
            xml.append("</act>");
            xml.append("</entry>");
        }

        xml.append("</section>");
        xml.append("</component>");
    }

    private void appendConfrontoPrecedentiEsami(StringBuilder xml, String confronto) {
        if (!XmlEscaper.hasText(confronto)) {
            return;
        }

        xml.append("<component typeCode=\"COMP\">");
        xml.append("<section ID=\"CONFRONTO_PRECEDENTI_ESAMI_ESEGUITI\">");
        xml.append("<code code=\"93126-1\" codeSystem=\"")
                .append(XmlEscaper.escapeAttribute(cdaProperties.getCodeSystemLoinc()))
                .append("\" codeSystemName=\"LOINC\" displayName=\"Confronto con Precedenti Esami Eseguiti\"/>");
        xml.append("<title> Confronto con Precedenti Esami Eseguiti </title>");
        xml.append("<text><paragraph>")
                .append(XmlEscaper.escapeText(confronto))
                .append("</paragraph></text>");
        xml.append("</section>");
        xml.append("</component>");
    }

    private void appendReferto(StringBuilder xml, String refertoText) {
        xml.append("<component typeCode=\"COMP\">");
        xml.append("<section ID=\"REFERTO\">");
        xml.append("<code code=\"47045-0\" codeSystem=\"")
                .append(XmlEscaper.escapeAttribute(cdaProperties.getCodeSystemLoinc()))
                .append("\" codeSystemName=\"LOINC\" displayName=\"Referto\"/>");
        xml.append("<title>Referto</title>");
        xml.append("<text><paragraph>")
                .append(XmlEscaper.escapeText(refertoText))
                .append("</paragraph></text>");
        xml.append("</section>");
        xml.append("</component>");
    }

    private void appendDiagnosi(StringBuilder xml, List<RsaDocument.Diagnosis> diagnosi) {
        if (diagnosi == null || diagnosi.isEmpty()) {
            return;
        }

        xml.append("<component typeCode=\"COMP\">");
        xml.append("<section ID=\"DIAGNOSI\">");
        xml.append("<code code=\"29548-5\" codeSystem=\"")
                .append(XmlEscaper.escapeAttribute(cdaProperties.getCodeSystemLoinc()))
                .append("\" codeSystemName=\"LOINC\" displayName=\"Diagnosi\"/>");
        xml.append("<title> Diagnosi </title>");

        xml.append("<text><list>");
        int index = 1;
        for (RsaDocument.Diagnosis diagnosis : diagnosi) {
            xml.append("<item><content ID=\"DIAG")
                    .append(index)
                    .append("\">")
                    .append(XmlEscaper.escapeText(diagnosis.getText()))
                    .append("</content></item>");
            index++;
        }
        xml.append("</list></text>");

        for (RsaDocument.Diagnosis diagnosis : diagnosi) {
            xml.append("<entry>");
            xml.append("<observation classCode=\"OBS\" moodCode=\"EVN\">");
            xml.append("<code code=\"29308-4\" codeSystem=\"")
                    .append(XmlEscaper.escapeAttribute(cdaProperties.getCodeSystemLoinc()))
                    .append("\" codeSystemName=\"LOINC\" displayName=\"Diagnosi\"/>");
            appendCodingValue(xml, diagnosis.getCoding());
            xml.append("</observation>");
            xml.append("</entry>");
        }

        xml.append("</section>");
        xml.append("</component>");
    }

    private void appendConclusioni(StringBuilder xml, String conclusioni) {
        if (!XmlEscaper.hasText(conclusioni)) {
            return;
        }

        xml.append("<component typeCode=\"COMP\">");
        xml.append("<section ID=\"CONCLUSIONI\">");
        xml.append("<code code=\"55110-1\" codeSystem=\"")
                .append(XmlEscaper.escapeAttribute(cdaProperties.getCodeSystemLoinc()))
                .append("\" codeSystemName=\"LOINC\" displayName=\"Conclusioni\"/>");
        xml.append("<title> Conclusioni </title>");
        xml.append("<text><paragraph>")
                .append(XmlEscaper.escapeText(conclusioni))
                .append("</paragraph></text>");
        xml.append("</section>");
        xml.append("</component>");
    }

    private void appendSuggerimenti(StringBuilder xml, String suggerimenti) {
        if (!XmlEscaper.hasText(suggerimenti)) {
            return;
        }

        xml.append("<component typeCode=\"COMP\">");
        xml.append("<section ID=\"SUGGERIMENTI_MEDICO_PRESCRITTORE\">");
        xml.append("<code code=\"62385-0\" codeSystem=\"")
                .append(XmlEscaper.escapeAttribute(cdaProperties.getCodeSystemLoinc()))
                .append("\" codeSystemName=\"LOINC\" displayName=\"Raccomandazioni\"/>");
        xml.append("<title> Suggerimenti per il Medico Prescrittore </title>");
        xml.append("<text><paragraph>")
                .append(XmlEscaper.escapeText(suggerimenti))
                .append("</paragraph></text>");
        xml.append("</section>");
        xml.append("</component>");
    }

    private void appendCode(StringBuilder xml, RsaDocument.Coding coding) {
        xml.append("<code code=\"")
                .append(XmlEscaper.escapeAttribute(coding.getCode()))
                .append("\" codeSystem=\"")
                .append(XmlEscaper.escapeAttribute(coding.getCodeSystem()))
                .append("\" codeSystemName=\"")
                .append(XmlEscaper.escapeAttribute(coding.getCodeSystemName()))
                .append("\" displayName=\"")
                .append(XmlEscaper.escapeAttribute(coding.getDisplayName()))
                .append("\"/>");
    }

    private void appendCodingValue(StringBuilder xml, RsaDocument.Coding coding) {
        xml.append("<value xsi:type=\"CD\" code=\"")
                .append(XmlEscaper.escapeAttribute(coding.getCode()))
                .append("\" codeSystem=\"")
                .append(XmlEscaper.escapeAttribute(coding.getCodeSystem()))
                .append("\" codeSystemName=\"")
                .append(XmlEscaper.escapeAttribute(coding.getCodeSystemName()))
                .append("\" displayName=\"")
                .append(XmlEscaper.escapeAttribute(coding.getDisplayName()))
                .append("\"/>");
    }

    private String formatTs(OffsetDateTime value) {
        return TS_FORMATTER.format(value.withNano(0));
    }
}