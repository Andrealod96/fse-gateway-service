package it.thcs.fse.fsersaservice.infrastructure.cda;

import it.thcs.fse.fsersaservice.domain.model.RsaDocument;
import it.thcs.fse.fsersaservice.support.util.XmlEscaper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CdaStructuredBodyFragmentBuilder {

    public String build(RsaDocument document) {
        StringBuilder xml = new StringBuilder();

        xml.append("<component>");
        xml.append("<structuredBody moodCode=\"EVN\" classCode=\"DOCBODY\">");

        appendQuesitoDiagnostico(xml, document);
        appendStoriaClinica(xml, document);
        appendPrecedentiEsamiEseguiti(xml, document);
        appendEsameObiettivo(xml, document);
        appendPrestazioni(xml, document);
        appendConfrontoPrecedentiEsami(xml, document);
        appendReferto(xml, document);
        appendDiagnosi(xml, document);
        appendConclusioni(xml, document);
        appendSuggerimentiMedicoPrescrittore(xml, document);
        appendAccertamentiControlliConsigliati(xml, document);
        appendTerapiaFarmacologicaConsigliata(xml, document);

        xml.append("</structuredBody>");
        xml.append("</component>");

        return xml.toString();
    }

    private void appendQuesitoDiagnostico(StringBuilder xml, RsaDocument document) {
        RsaDocument.CodedText quesito = safeClinical(document).getQuesitoDiagnostico();
        if (quesito == null || !hasText(quesito.getText())) {
            return;
        }

        xml.append("<component typeCode=\"COMP\">");
        xml.append("<section ID=\"QUESITO_DIAGNOSTICO\">");
        xml.append("<code code=\"29299-5\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Motivo della visita\"/>");
        xml.append("<title> Quesito diagnostico </title>");
        xml.append("<text><paragraph> ").append(t(quesito.getText())).append(" </paragraph></text>");

        if (quesito.getCoding() != null) {
            xml.append("<entry>");
            xml.append("<observation classCode=\"OBS\" moodCode=\"EVN\">");
            xml.append("<code code=\"29298-7\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Motivo della visita\"/>");
            xml.append("<value xsi:type=\"CD\"");
            appendCodingAttributes(xml, quesito.getCoding());
            xml.append("></value>");
            xml.append("</observation>");
            xml.append("</entry>");
        }

        xml.append("</section>");
        xml.append("</component>");
    }

    private void appendStoriaClinica(StringBuilder xml, RsaDocument document) {
        RsaDocument.ClinicalContent clinical = safeClinical(document);
        RsaDocument.StoriaClinica storia = clinical.getStoriaClinicaDettaglio();

        boolean hasStructured = storia != null && hasStructuredStoriaClinicaContent(storia);
        boolean hasSimple = hasText(clinical.getStoriaClinica());

        if (!hasStructured && !hasSimple) {
            return;
        }

        xml.append("<component typeCode=\"COMP\">");
        xml.append("<section ID=\"STORIA_CLINICA\">");
        xml.append("<code code=\"11329-0\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Storia Generale\"/>");
        xml.append("<title> Storia Clinica </title>");

        xml.append("<text>");
        if (hasStructured) {
            xml.append("<table>");
            xml.append("<thead><tr>");
            xml.append("<th>Anamnesi Patologica remota</th>");
            xml.append("<th>Anamnesi Patologica prossima</th>");
            xml.append("<th>Anamnesi Patologica Fisiologica</th>");
            xml.append("<th>Anamnesi Familiare</th>");
            xml.append("<th>Allergie</th>");
            xml.append("</tr></thead>");
            xml.append("<tbody><tr>");
            xml.append("<td>").append(t(storia.getAnamnesiPatologicaRemotaText())).append("</td>");
            xml.append("<td>").append(t(storia.getAnamnesiPatologicaProssimaText())).append("</td>");
            xml.append("<td>").append(t(storia.getAnamnesiPatologicaFisiologicaText())).append("</td>");
            xml.append("<td>").append(t(joinFamilyHistoryNarrative(storia.getFamilyHistories()))).append("</td>");
            xml.append("<td>").append(t(joinAllergyNarrative(storia.getAllergies()))).append("</td>");
            xml.append("</tr></tbody>");
            xml.append("</table>");
        } else {
            xml.append("<paragraph> ").append(t(clinical.getStoriaClinica())).append(" </paragraph>");
        }
        xml.append("</text>");

        if (hasStructured && storia.getProblemCoding() != null) {
            xml.append("<entry>");
            xml.append("<observation moodCode=\"EVN\" classCode=\"OBS\">");
            xml.append("<code code=\"75326-9\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Problem\"/>");
            xml.append("<statusCode code=\"completed\"/>");

            if (hasText(storia.getProblemStartDateTime()) || hasText(storia.getProblemEndDateTime())) {
                xml.append("<effectiveTime>");
                if (hasText(storia.getProblemStartDateTime())) {
                    xml.append("<low value=\"").append(a(ts(storia.getProblemStartDateTime()))).append("\"/>");
                }
                if (hasText(storia.getProblemEndDateTime())) {
                    xml.append("<high value=\"").append(a(ts(storia.getProblemEndDateTime()))).append("\"/>");
                }
                xml.append("</effectiveTime>");
            }

            xml.append("<value xsi:type=\"CD\"");
            appendCodingAttributes(xml, storia.getProblemCoding());
            xml.append("/>");

            if (storia.getDecorsoClinicoCoding() != null) {
                xml.append("<entryRelationship typeCode=\"REFR\" inversionInd=\"false\">");
                xml.append("<observation classCode=\"OBS\" moodCode=\"EVN\">");
                xml.append("<code code=\"89261-2\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Decorso Clinico\"/>");
                if (hasText(storia.getDecorsoClinicoReferenceText())) {
                    xml.append("<text><reference value=\"#").append(a(storia.getDecorsoClinicoReferenceText())).append("\"/></text>");
                }
                xml.append("<statusCode code=\"completed\"/>");
                xml.append("<value xsi:type=\"CE\"");
                appendCodingAttributes(xml, storia.getDecorsoClinicoCoding());
                xml.append("/>");
                xml.append("</observation>");
                xml.append("</entryRelationship>");
            }

            if (storia.getStatoClinicoCoding() != null) {
                xml.append("<entryRelationship typeCode=\"REFR\" inversionInd=\"false\">");
                xml.append("<observation classCode=\"OBS\" moodCode=\"EVN\">");
                xml.append("<code code=\"33999-4\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Stato\"/>");
                if (hasText(storia.getStatoClinicoReferenceText())) {
                    xml.append("<text><reference value=\"#").append(a(storia.getStatoClinicoReferenceText())).append("\"/></text>");
                }
                xml.append("<statusCode code=\"completed\"/>");
                xml.append("<value xsi:type=\"CE\"");
                appendCodingAttributes(xml, storia.getStatoClinicoCoding());
                xml.append("/>");
                xml.append("</observation>");
                xml.append("</entryRelationship>");
            }

            xml.append("</observation>");
            xml.append("</entry>");
        }

        if (hasStructured) {
            appendFamilyHistoryEntries(xml, storia.getFamilyHistories());
            appendAllergieSubSection(xml, storia.getAllergies());
            appendTerapiaInAttoSubSection(xml, storia.getTerapiaFarmacologicaInAtto());
        }

        xml.append("</section>");
        xml.append("</component>");
    }

    private void appendFamilyHistoryEntries(StringBuilder xml, List<RsaDocument.FamilyHistory> familyHistories) {
        if (familyHistories == null || familyHistories.isEmpty()) {
            return;
        }

        for (RsaDocument.FamilyHistory fh : familyHistories) {
            if (fh == null || fh.getDiagnosisCoding() == null) {
                continue;
            }

            xml.append("<entry>");
            xml.append("<organizer moodCode=\"EVN\" classCode=\"CLUSTER\">");
            xml.append("<code code=\"10157-6\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Storia di malattie di membri familiari\"/>");
            xml.append("<statusCode code=\"completed\"/>");

            if (hasText(fh.getEffectiveTime())) {
                xml.append("<effectiveTime value=\"").append(a(ts(fh.getEffectiveTime()))).append("\"/>");
            }

            xml.append("<subject typeCode=\"SBJ\">");
            xml.append("<relatedSubject classCode=\"PRS\">");
            xml.append("<code code=\"").append(a(fh.getRelationshipCode())).append("\"");
            xml.append(" codeSystem=\"").append(a(defaultIfBlank(fh.getRelationshipCodeSystem(), "2.16.840.1.113883.5.111"))).append("\"");
            xml.append(" codeSystemName=\"").append(a(defaultIfBlank(fh.getRelationshipCodeSystemName(), "RoleCode"))).append("\"");
            xml.append(" displayName=\"").append(a(fh.getRelationshipDisplayName())).append("\"/>");
            xml.append("<subject>");
            xml.append("<administrativeGenderCode code=\"").append(a(fh.getSubjectGenderCode())).append("\"");
            xml.append(" codeSystem=\"2.16.840.1.113883.5.1\" codeSystemName=\"HL7 AdministrativeGender\" displayName=\"")
                    .append(a(resolveGenderDisplayName(fh.getSubjectGenderCode()))).append("\"/>");
            xml.append("</subject>");
            xml.append("</relatedSubject>");
            xml.append("</subject>");

            xml.append("<component>");
            xml.append("<observation moodCode=\"EVN\" classCode=\"OBS\">");
            xml.append("<code code=\"52797-8\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Diagnosi codice ICD\"/>");
            if (hasText(fh.getDiagnosisReferenceText())) {
                xml.append("<text><reference value=\"#").append(a(fh.getDiagnosisReferenceText())).append("\"/></text>");
            }
            xml.append("<statusCode code=\"completed\"/>");

            if (hasText(fh.getEffectiveTime())) {
                xml.append("<effectiveTime value=\"").append(a(ts(fh.getEffectiveTime()))).append("\"/>");
            }

            xml.append("<value xsi:type=\"CD\"");
            appendCodingAttributes(xml, fh.getDiagnosisCoding());
            xml.append("/>");

            if (fh.getAgeAtDiagnosisYears() != null) {
                xml.append("<entryRelationship typeCode=\"SUBJ\">");
                xml.append("<observation classCode=\"OBS\" moodCode=\"EVN\">");
                xml.append("<code code=\"35267-4\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Età diagnosi patologia\"/>");
                xml.append("<value xsi:type=\"PQ\" value=\"").append(fh.getAgeAtDiagnosisYears()).append("\" unit=\"a\"/>");
                xml.append("</observation>");
                xml.append("</entryRelationship>");
            }

            if (fh.getAgeAtDeathYears() != null) {
                xml.append("<entryRelationship typeCode=\"SUBJ\">");
                xml.append("<observation classCode=\"OBS\" moodCode=\"EVN\">");
                xml.append("<code code=\"39016-1\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Età decesso\"/>");
                xml.append("<value xsi:type=\"PQ\" value=\"").append(fh.getAgeAtDeathYears()).append("\" unit=\"a\"/>");
                xml.append("</observation>");
                xml.append("</entryRelationship>");
            }

            xml.append("</observation>");
            xml.append("</component>");
            xml.append("</organizer>");
            xml.append("</entry>");
        }
    }

    private void appendAllergieSubSection(StringBuilder xml, List<RsaDocument.Allergy> allergies) {
        List<RsaDocument.Allergy> valid = validAllergies(allergies);
        if (valid.isEmpty()) {
            return;
        }

        xml.append("<component typeCode=\"COMP\">");
        xml.append("<section ID=\"ALLERGIE\">");
        xml.append("<code code=\"48765-2\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Allergie e/o reazioni avverse\"/>");
        xml.append("<title> Allergie </title>");
        xml.append("<text><paragraph> ").append(t(joinAllergyNarrative(valid))).append(" </paragraph></text>");

        for (RsaDocument.Allergy allergy : valid) {
            xml.append("<entry>");
            xml.append("<act classCode=\"ACT\" moodCode=\"EVN\">");
            xml.append("<code nullFlavor=\"NA\"/>");
            xml.append("<statusCode code=\"active\"/>");

            if (hasText(allergy.getStartDateTime()) || hasText(allergy.getEndDateTime())) {
                xml.append("<effectiveTime>");
                if (hasText(allergy.getStartDateTime())) {
                    xml.append("<low value=\"").append(a(ts(allergy.getStartDateTime()))).append("\"/>");
                }
                if (hasText(allergy.getEndDateTime())) {
                    xml.append("<high value=\"").append(a(ts(allergy.getEndDateTime()))).append("\"/>");
                }
                xml.append("</effectiveTime>");
            }

            xml.append("<entryRelationship typeCode=\"SUBJ\">");
            xml.append("<observation classCode=\"OBS\" moodCode=\"EVN\">");
            xml.append("<code code=\"52473-6\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Allergia o causa della reazione\"/>");
            xml.append("<statusCode code=\"completed\"/>");

            if (hasText(allergy.getStartDateTime()) || hasText(allergy.getEndDateTime())) {
                xml.append("<effectiveTime>");
                if (hasText(allergy.getStartDateTime())) {
                    xml.append("<low value=\"").append(a(ts(allergy.getStartDateTime()))).append("\"/>");
                }
                if (hasText(allergy.getEndDateTime())) {
                    xml.append("<high value=\"").append(a(ts(allergy.getEndDateTime()))).append("\"/>");
                }
                xml.append("</effectiveTime>");
            }

            xml.append("<value xsi:type=\"CD\" code=\"ALG\" codeSystem=\"2.16.840.1.113883.5.4\" codeSystemName=\"ObservationIntoleranceType\" displayName=\"Allergy\"/>");

            if (allergy.getAgentCoding() != null) {
                xml.append("<participant typeCode=\"CSM\">");
                xml.append("<participantRole classCode=\"MANU\">");
                xml.append("<playingEntity classCode=\"MMAT\">");
                xml.append("<code");
                appendCodingAttributes(xml, allergy.getAgentCoding());
                xml.append("/>");
                xml.append("</playingEntity>");
                xml.append("</participantRole>");
                xml.append("</participant>");
            }

            if (allergy.getReactionCoding() != null) {
                xml.append("<entryRelationship typeCode=\"MFST\">");
                xml.append("<observation classCode=\"OBS\" moodCode=\"EVN\">");
                xml.append("<code code=\"75321-0\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Obiettività Clinica\"/>");
                xml.append("<statusCode code=\"completed\"/>");
                xml.append("<value xsi:type=\"CD\"");
                appendCodingAttributes(xml, allergy.getReactionCoding());
                xml.append("/>");
                xml.append("</observation>");
                xml.append("</entryRelationship>");
            }

            if (allergy.getCriticalityCoding() != null) {
                xml.append("<entryRelationship typeCode=\"SUBJ\">");
                xml.append("<observation classCode=\"OBS\" moodCode=\"EVN\">");
                xml.append("<code code=\"SEV\" codeSystem=\"2.16.840.1.113883.5.4\" codeSystemName=\"ActCode\" displayName=\"Criticality\"/>");
                xml.append("<statusCode code=\"completed\"/>");
                xml.append("<value xsi:type=\"CD\"");
                appendCodingAttributes(xml, allergy.getCriticalityCoding());
                xml.append("/>");
                xml.append("</observation>");
                xml.append("</entryRelationship>");
            }

            if (allergy.getStatusCoding() != null) {
                xml.append("<entryRelationship typeCode=\"REFR\">");
                xml.append("<observation classCode=\"OBS\" moodCode=\"EVN\">");
                xml.append("<code code=\"33999-4\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Stato\"/>");
                xml.append("<statusCode code=\"completed\"/>");
                xml.append("<value xsi:type=\"CD\"");
                appendCodingAttributes(xml, allergy.getStatusCoding());
                xml.append("/>");
                xml.append("</observation>");
                xml.append("</entryRelationship>");
            }

            if (hasText(allergy.getComment())) {
                xml.append("<entryRelationship typeCode=\"SUBJ\">");
                xml.append("<act classCode=\"ACT\" moodCode=\"EVN\">");
                xml.append("<code code=\"48767-8\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Annotation Comment\"/>");
                xml.append("<text>").append(t(allergy.getComment())).append("</text>");
                xml.append("<statusCode code=\"completed\"/>");
                xml.append("</act>");
                xml.append("</entryRelationship>");
            }

            xml.append("</observation>");
            xml.append("</entryRelationship>");
            xml.append("</act>");
            xml.append("</entry>");
        }

        xml.append("</section>");
        xml.append("</component>");
    }

    private void appendTerapiaInAttoSubSection(StringBuilder xml, List<RsaDocument.TerapiaFarmacologica> terapie) {
        List<RsaDocument.TerapiaFarmacologica> valid = validTherapies(terapie, false);
        if (valid.isEmpty()) {
            return;
        }

        xml.append("<component typeCode=\"COMP\">");
        xml.append("<section ID=\"TERAPIA_FARMACOLOGICA_IN_ATTO\">");
        xml.append("<code code=\"10160-0\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Uso di farmaci\"/>");
        xml.append("<title> Terapia farmacologica in atto </title>");
        xml.append("<text><list>");

        int i = 1;
        for (RsaDocument.TerapiaFarmacologica terapia : valid) {
            xml.append("<item><content ID=\"TF-").append(i++).append("\">")
                    .append(t(defaultIfBlank(terapia.getText(), terapia.getAicDisplayName())))
                    .append("</content></item>");
        }

        xml.append("</list></text>");

        for (RsaDocument.TerapiaFarmacologica terapia : valid) {
            xml.append("<entry>");
            xml.append("<substanceAdministration classCode=\"SBADM\" moodCode=\"EVN\">");
            xml.append("<consumable><manufacturedProduct><manufacturedMaterial>");
            xml.append("<code code=\"").append(a(terapia.getAicCode())).append("\"");
            xml.append(" codeSystem=\"2.16.840.1.113883.2.9.6.1.5\"");
            xml.append(" codeSystemName=\"AIC\"");
            xml.append(" displayName=\"").append(a(defaultIfBlank(terapia.getAicDisplayName(), terapia.getText()))).append("\"");

            if (hasText(terapia.getAtcCode())) {
                xml.append(">");
                xml.append("<translation code=\"").append(a(terapia.getAtcCode())).append("\"");
                xml.append(" codeSystem=\"2.16.840.1.113883.6.73\"");
                xml.append(" codeSystemName=\"WHO ATC\"");
                xml.append(" displayName=\"").append(a(terapia.getAtcDisplayName())).append("\"/>");
                xml.append("</code>");
            } else {
                xml.append("/>");
            }

            xml.append("</manufacturedMaterial></manufacturedProduct></consumable>");
            xml.append("</substanceAdministration>");
            xml.append("</entry>");
        }

        xml.append("</section>");
        xml.append("</component>");
    }

    private void appendPrecedentiEsamiEseguiti(StringBuilder xml, RsaDocument document) {
        List<RsaDocument.PrecedenteEsame> valid = validPreviousExams(safeClinical(document).getPrecedentiEsamiEseguiti());
        if (valid.isEmpty()) {
            return;
        }

        xml.append("<component typeCode=\"COMP\">");
        xml.append("<section ID=\"PRECEDENTI_ESAMI_ESEGUITI\">");
        xml.append("<code code=\"30954-2\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Test diagnostici o dati di laboratorio rilevanti\"/>");
        xml.append("<title>Precedenti Esami Eseguiti</title>");
        xml.append("<text><table><thead><tr><th>Precedente Esame Eseguito</th><th>Data Esame</th><th>Esito</th></tr></thead><tbody>");

        for (RsaDocument.PrecedenteEsame esame : valid) {
            xml.append("<tr>");
            xml.append("<td>").append(t(esame.getDescription())).append("</td>");
            xml.append("<td>").append(t(esame.getExecutionStartDateTime())).append("</td>");
            xml.append("<td>").append(t(esame.getOutcomeText())).append("</td>");
            xml.append("</tr>");
        }

        xml.append("</tbody></table></text>");

        for (RsaDocument.PrecedenteEsame esame : valid) {
            xml.append("<entry>");
            xml.append("<observation classCode=\"OBS\" moodCode=\"EVN\">");

            if (esame.getCoding() != null) {
                xml.append("<code");
                appendCodingAttributes(xml, esame.getCoding());
                xml.append("/>");
            } else {
                xml.append("<code code=\"90.54\" codeSystem=\"2.16.840.1.113883.6.103\" codeSystemName=\"ICD-9-CM\" displayName=\"Esame precedente\"/>");
            }

            xml.append("<effectiveTime>");
            if (hasText(esame.getExecutionStartDateTime())) {
                xml.append("<low value=\"").append(a(ts(esame.getExecutionStartDateTime()))).append("\"/>");
            }
            if (hasText(esame.getExecutionEndDateTime())) {
                xml.append("<high value=\"").append(a(ts(esame.getExecutionEndDateTime()))).append("\"/>");
            }
            xml.append("</effectiveTime>");

            xml.append("<value xsi:type=\"ST\">").append(t(esame.getOutcomeText())).append("</value>");
            xml.append("</observation>");
            xml.append("</entry>");
        }

        xml.append("</section>");
        xml.append("</component>");
    }

    private void appendEsameObiettivo(StringBuilder xml, RsaDocument document) {
        String value = safeClinical(document).getEsameObiettivo();
        if (!hasText(value)) {
            return;
        }

        xml.append("<component typeCode=\"COMP\">");
        xml.append("<section ID=\"ESAME_OBIETTIVO\">");
        xml.append("<code code=\"29545-1\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Osservazioni fisiche\"/>");
        xml.append("<title> Esame Obiettivo </title>");
        xml.append("<text><paragraph> ").append(t(value)).append(" </paragraph></text>");
        xml.append("</section>");
        xml.append("</component>");
    }

    private void appendPrestazioni(StringBuilder xml, RsaDocument document) {
        List<RsaDocument.Prestazione> valid = validPrestazioni(safeClinical(document).getPrestazioni());
        if (valid.isEmpty()) {
            return;
        }

        xml.append("<component typeCode=\"COMP\">");
        xml.append("<section ID=\"PRESTAZIONI\">");
        xml.append("<code code=\"62387-6\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Interventi\"/>");
        xml.append("<title> Prestazioni </title>");
        xml.append("<text>");
        xml.append("<paragraph><caption>Prestazioni Eseguite</caption></paragraph>");
        xml.append("<list>");

        int pIndex = 1;
        for (RsaDocument.Prestazione prestazione : valid) {
            String prestazioneId = "Prestazione-" + pIndex;
            xml.append("<item ID=\"").append(prestazioneId).append("\">");
            xml.append("<table><thead><tr><th>Prestazione ").append(pIndex).append("</th></tr></thead><tbody>");
            xml.append("<tr><th>Codice Prestazione</th><td>")
                    .append(t(prestazione.getCoding() != null ? prestazione.getCoding().getCode() : ""))
                    .append("</td></tr>");
            xml.append("<tr><th>Descrizione Prestazione eseguita</th><td>")
                    .append(t(defaultIfBlank(prestazione.getText(), prestazione.getCoding() != null ? prestazione.getCoding().getDisplayName() : "")))
                    .append("</td></tr>");
            xml.append("<tr><th>Data Prestazione eseguita</th><td>")
                    .append(t(ts(prestazione.getPerformedAt())))
                    .append("</td></tr>");
            xml.append("</tbody></table>");

            List<RsaDocument.ProceduraOperativa> procedure = validProcedure(prestazione.getProcedureOperative());
            if (!procedure.isEmpty()) {
                xml.append("<paragraph><caption>Procedure Operative Eseguite</caption></paragraph>");
                xml.append("<list>");
                int procIndex = 1;
                for (RsaDocument.ProceduraOperativa procedura : procedure) {
                    String procId = "Procedura-" + pIndex + "-" + procIndex;
                    xml.append("<item ID=\"").append(procId).append("\">");
                    xml.append("<table border=\"1\"><thead><tr><th colspan=\"2\">Procedura ").append(procIndex).append("</th></tr></thead><tbody>");
                    xml.append("<tr><th>Codice Prestazione Eseguita</th><td>")
                            .append(t(prestazione.getCoding() != null ? prestazione.getCoding().getCode() : ""))
                            .append("</td></tr>");
                    xml.append("<tr><th>Codice Procedura Operativa</th><td>")
                            .append(t(procedura.getCoding() != null ? procedura.getCoding().getCode() : ""))
                            .append("</td></tr>");
                    xml.append("<tr><th>Descrizione Procedura Operativa</th><td>")
                            .append(t(defaultIfBlank(procedura.getText(), procedura.getCoding() != null ? procedura.getCoding().getDisplayName() : "")))
                            .append("</td></tr>");
                    xml.append("<tr><th>Quantità</th><td>").append(t(procedura.getQuantityText())).append("</td></tr>");
                    xml.append("<tr><th>Modalità esecuzione procedura operativa</th><td>").append(t(procedura.getExecutionMode())).append("</td></tr>");
                    xml.append("<tr><th>Strumentazione utilizzata</th><td>").append(t(procedura.getInstrumentation())).append("</td></tr>");
                    xml.append("<tr><th>Parametri descrittivi della procedura</th><td>").append(t(procedura.getDescriptiveParameters())).append("</td></tr>");
                    xml.append("<tr><th>Note</th><td>").append(t(procedura.getNotes())).append("</td></tr>");
                    xml.append("</tbody></table>");
                    xml.append("</item>");
                    procIndex++;
                }
                xml.append("</list>");
            }

            xml.append("</item>");
            pIndex++;
        }

        xml.append("</list>");
        xml.append("</text>");

        pIndex = 1;
        for (RsaDocument.Prestazione prestazione : valid) {
            xml.append("<entry>");
            xml.append("<act classCode=\"ACT\" moodCode=\"EVN\">");

            if (prestazione.getCoding() != null) {
                xml.append("<code");
                appendCodingAttributes(xml, prestazione.getCoding());
                xml.append(">");
                xml.append("<originalText><reference value=\"#Prestazione-").append(pIndex).append("\"/></originalText>");
                xml.append("</code>");
            }

            if (hasText(prestazione.getPerformedAt())) {
                xml.append("<effectiveTime value=\"").append(a(ts(prestazione.getPerformedAt()))).append("\"/>");
            }

            for (RsaDocument.ProceduraOperativa procedura : validProcedure(prestazione.getProcedureOperative())) {
                xml.append("<entryRelationship typeCode=\"COMP\">");
                xml.append("<procedure classCode=\"PROC\" moodCode=\"EVN\">");
                if (procedura.getCoding() != null) {
                    xml.append("<code");
                    appendCodingAttributes(xml, procedura.getCoding());
                    xml.append("/>");
                }
                if (hasText(procedura.getPerformedAt())) {
                    xml.append("<effectiveTime value=\"").append(a(ts(procedura.getPerformedAt()))).append("\"/>");
                } else if (hasText(prestazione.getPerformedAt())) {
                    xml.append("<effectiveTime value=\"").append(a(ts(prestazione.getPerformedAt()))).append("\"/>");
                }
                xml.append("</procedure>");
                xml.append("</entryRelationship>");
            }

            xml.append("</act>");
            xml.append("</entry>");
            pIndex++;
        }

        xml.append("</section>");
        xml.append("</component>");
    }

    private void appendConfrontoPrecedentiEsami(StringBuilder xml, RsaDocument document) {
        String value = safeClinical(document).getConfrontoPrecedentiEsami();
        if (!hasText(value)) {
            return;
        }

        xml.append("<component typeCode=\"COMP\">");
        xml.append("<section ID=\"CONFRONTO_PRECEDENTI_ESAMI_ESEGUITI\">");
        xml.append("<code code=\"93126-1\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Confronto con Precedenti Esami Eseguiti\"/>");
        xml.append("<title> Confronto con Precedenti Esami Eseguiti </title>");
        xml.append("<text><paragraph> ").append(t(value)).append(" </paragraph></text>");
        xml.append("</section>");
        xml.append("</component>");
    }

    private void appendReferto(StringBuilder xml, RsaDocument document) {
        String value = safeClinical(document).getRefertoText();
        if (!hasText(value)) {
            return;
        }

        xml.append("<component typeCode=\"COMP\">");
        xml.append("<section ID=\"REFERTO\">");
        xml.append("<code code=\"47045-0\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Referto\"/>");
        xml.append("<title>Referto</title>");
        xml.append("<text><paragraph> ").append(t(value)).append(" </paragraph></text>");
        xml.append("</section>");
        xml.append("</component>");
    }

    private void appendDiagnosi(StringBuilder xml, RsaDocument document) {
        List<RsaDocument.CodedText> valid = validDiagnosi(safeClinical(document).getDiagnosi());
        if (valid.isEmpty()) {
            return;
        }

        xml.append("<component typeCode=\"COMP\">");
        xml.append("<section ID=\"DIAGNOSI\">");
        xml.append("<code code=\"29548-5\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Diagnosi\"/>");
        xml.append("<title> Diagnosi </title>");
        xml.append("<text><list>");

        int i = 1;
        for (RsaDocument.CodedText diagnosi : valid) {
            xml.append("<item><content ID=\"DIAG").append(i++).append("\">")
                    .append(t(diagnosi.getText()))
                    .append("</content></item>");
        }

        xml.append("</list></text>");

        for (RsaDocument.CodedText diagnosi : valid) {
            if (diagnosi.getCoding() == null) {
                continue;
            }
            xml.append("<entry>");
            xml.append("<observation classCode=\"OBS\" moodCode=\"EVN\">");
            xml.append("<code code=\"29308-4\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Diagnosi\"/>");
            xml.append("<value xsi:type=\"CD\"");
            appendCodingAttributes(xml, diagnosi.getCoding());
            xml.append("/>");
            xml.append("</observation>");
            xml.append("</entry>");
        }

        xml.append("</section>");
        xml.append("</component>");
    }

    private void appendConclusioni(StringBuilder xml, RsaDocument document) {
        String value = safeClinical(document).getConclusioni();
        if (!hasText(value)) {
            return;
        }

        xml.append("<component typeCode=\"COMP\">");
        xml.append("<section ID=\"CONCLUSIONI\">");
        xml.append("<code code=\"55110-1\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Conclusioni\"/>");
        xml.append("<title> Conclusioni </title>");
        xml.append("<text><paragraph> ").append(t(value)).append(" </paragraph></text>");
        xml.append("</section>");
        xml.append("</component>");
    }

    private void appendSuggerimentiMedicoPrescrittore(StringBuilder xml, RsaDocument document) {
        String value = safeClinical(document).getSuggerimentiMedicoPrescrittore();
        if (!hasText(value)) {
            return;
        }

        xml.append("<component typeCode=\"COMP\">");
        xml.append("<section ID=\"SUGGERIMENTI_MEDICO_PRESCRITTORE\">");
        xml.append("<code code=\"62385-0\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Raccomandazioni\"/>");
        xml.append("<title> Suggerimenti per il Medico Prescrittore </title>");
        xml.append("<text><paragraph> ").append(t(value)).append(" </paragraph></text>");
        xml.append("</section>");
        xml.append("</component>");
    }

    private void appendAccertamentiControlliConsigliati(StringBuilder xml, RsaDocument document) {
        List<RsaDocument.AccertamentoControllo> valid = validAccertamenti(safeClinical(document).getAccertamentiControlliConsigliati());
        if (valid.isEmpty()) {
            return;
        }

        xml.append("<component typeCode=\"COMP\">");
        xml.append("<section ID=\"ACCERTAMENTI_CONTROLLI_CONSIGLIATI\">");
        xml.append("<code code=\"80615-8\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Follow-up Consigliato\"/>");
        xml.append("<title> Accertamenti e Controlli Consigliati </title>");
        xml.append("<text><list>");

        int i = 1;
        for (RsaDocument.AccertamentoControllo acc : valid) {
            xml.append("<item><content ID=\"AC-").append(i++).append("\">")
                    .append(t(acc.getText()))
                    .append("</content></item>");
        }

        xml.append("</list></text>");

        for (RsaDocument.AccertamentoControllo acc : valid) {
            xml.append("<entry>");
            xml.append("<act classCode=\"ACT\" moodCode=\"PRP\">");
            if (acc.getCoding() != null) {
                xml.append("<code");
                appendCodingAttributes(xml, acc.getCoding());
                xml.append("/>");
            }
            xml.append("</act>");
            xml.append("</entry>");
        }

        xml.append("</section>");
        xml.append("</component>");
    }

    private void appendTerapiaFarmacologicaConsigliata(StringBuilder xml, RsaDocument document) {
        List<RsaDocument.TerapiaFarmacologica> valid = validTherapies(safeClinical(document).getTerapiaFarmacologicaConsigliata(), true);
        if (valid.isEmpty()) {
            return;
        }

        xml.append("<component typeCode=\"COMP\">");
        xml.append("<section ID=\"TERAPIA_FARMACOLOGICA_CONSIGLIATA\">");
        xml.append("<code code=\"93341-6\" codeSystem=\"2.16.840.1.113883.6.1\" codeSystemName=\"LOINC\" displayName=\"Farmaci Consigliati\"/>");
        xml.append("<title> Terapia farmacologica Consigliata </title>");
        xml.append("<text>");

        for (RsaDocument.TerapiaFarmacologica terapia : valid) {
            xml.append("<paragraph> ")
                    .append(t(defaultIfBlank(terapia.getText(), terapia.getAicDisplayName())))
                    .append(" </paragraph>");
        }

        xml.append("</text>");

        for (RsaDocument.TerapiaFarmacologica terapia : valid) {
            xml.append("<entry>");
            xml.append("<substanceAdministration classCode=\"SBADM\" moodCode=\"PRP\">");

            xml.append("<effectiveTime xsi:type=\"IVL_TS\">");
            xml.append("<low value=\"").append(a(ts(terapia.getStartDateTime()))).append("\"/>");
            xml.append("<high value=\"").append(a(ts(terapia.getEndDateTime()))).append("\"/>");
            xml.append("</effectiveTime>");

            if (terapia.getFrequencyHours() != null) {
                xml.append("<effectiveTime operator=\"A\" xsi:type=\"PIVL_TS\" institutionSpecified=\"true\">");
                xml.append("<period value=\"").append(terapia.getFrequencyHours()).append("\" unit=\"h\"/>");
                xml.append("</effectiveTime>");
            }

            if (hasText(terapia.getRouteCode())) {
                xml.append("<routeCode code=\"").append(a(terapia.getRouteCode())).append("\"");
                xml.append(" codeSystem=\"2.16.840.1.113883.5.112\"");
                xml.append(" codeSystemName=\"HL7 RouteOfAdministration\"");
                if (hasText(terapia.getRouteDisplayName())) {
                    xml.append(" displayName=\"").append(a(terapia.getRouteDisplayName())).append("\"");
                }
                xml.append("/>");
            }

            if (hasText(terapia.getDoseLowValue()) || hasText(terapia.getDoseHighValue())) {
                xml.append("<doseQuantity>");
                if (hasText(terapia.getDoseLowValue())) {
                    xml.append("<low value=\"").append(a(terapia.getDoseLowValue())).append("\"");
                    if (hasText(terapia.getDoseUnit())) {
                        xml.append(" unit=\"").append(a(terapia.getDoseUnit())).append("\"");
                    }
                    xml.append("/>");
                }
                if (hasText(terapia.getDoseHighValue())) {
                    xml.append("<high value=\"").append(a(terapia.getDoseHighValue())).append("\"");
                    if (hasText(terapia.getDoseUnit())) {
                        xml.append(" unit=\"").append(a(terapia.getDoseUnit())).append("\"");
                    }
                    xml.append("/>");
                }
                xml.append("</doseQuantity>");
            }

            if (hasText(terapia.getRateLowValue()) || hasText(terapia.getRateHighValue())) {
                xml.append("<rateQuantity>");
                if (hasText(terapia.getRateLowValue())) {
                    xml.append("<low value=\"").append(a(terapia.getRateLowValue())).append("\"");
                    if (hasText(terapia.getRateUnit())) {
                        xml.append(" unit=\"").append(a(terapia.getRateUnit())).append("\"");
                    }
                    xml.append("/>");
                }
                if (hasText(terapia.getRateHighValue())) {
                    xml.append("<high value=\"").append(a(terapia.getRateHighValue())).append("\"");
                    if (hasText(terapia.getRateUnit())) {
                        xml.append(" unit=\"").append(a(terapia.getRateUnit())).append("\"");
                    }
                    xml.append("/>");
                }
                xml.append("</rateQuantity>");
            }

            if (terapia.getAdministrationUnitCoding() != null) {
                xml.append("<administrationUnitCode");
                appendCodingAttributes(xml, terapia.getAdministrationUnitCoding());
                xml.append("/>");
            }

            xml.append("<consumable>");
            xml.append("<manufacturedProduct classCode=\"MANU\">");
            xml.append("<manufacturedMaterial>");
            xml.append("<code code=\"").append(a(terapia.getAicCode())).append("\"");
            xml.append(" codeSystem=\"2.16.840.1.113883.2.9.6.1.5\"");
            xml.append(" codeSystemName=\"AIC\"");
            xml.append(" displayName=\"").append(a(defaultIfBlank(terapia.getAicDisplayName(), terapia.getText()))).append("\"");

            if (hasText(terapia.getAtcCode())) {
                xml.append(">");
                xml.append("<translation code=\"").append(a(terapia.getAtcCode())).append("\"");
                xml.append(" codeSystem=\"2.16.840.1.113883.6.73\"");
                xml.append(" codeSystemName=\"ATC\"");
                if (hasText(terapia.getAtcDisplayName())) {
                    xml.append(" displayName=\"").append(a(terapia.getAtcDisplayName())).append("\"");
                }
                xml.append("/>");
                xml.append("</code>");
            } else {
                xml.append("/>");
            }

            xml.append("</manufacturedMaterial>");
            xml.append("</manufacturedProduct>");
            xml.append("</consumable>");

            if (terapia.getParticipantRef() != null) {
                appendTherapyParticipantRef(xml, terapia.getParticipantRef());
            }

            if (hasText(terapia.getGrammaturaValue())) {
                xml.append("<entryRelationship typeCode=\"COMP\">");
                xml.append("<observation classCode=\"OBS\" moodCode=\"EVN\">");
                xml.append("<code code=\"029480086\" codeSystem=\"2.16.840.1.113883.2.9.6.1.5\" codeSystemName=\"AIC\" displayName=\"Grammatura\"/>");
                xml.append("<value xsi:type=\"REAL\" value=\"").append(a(terapia.getGrammaturaValue())).append("\"/>");
                xml.append("</observation>");
                xml.append("</entryRelationship>");
            }

            if (hasText(terapia.getPackageQuantityValue())) {
                xml.append("<entryRelationship typeCode=\"COMP\">");
                xml.append("<observation classCode=\"OBS\" moodCode=\"EVN\">");
                xml.append("<code code=\"029480086\" codeSystem=\"2.16.840.1.113883.2.9.6.1.5\" codeSystemName=\"AIC\" displayName=\"Quantità nella Confezione\"/>");
                xml.append("<value xsi:type=\"REAL\" value=\"").append(a(terapia.getPackageQuantityValue())).append("\"/>");
                xml.append("</observation>");
                xml.append("</entryRelationship>");
            }

            if (terapia.getQuantity() != null) {
                xml.append("<entryRelationship typeCode=\"COMP\">");
                xml.append("<supply classCode=\"SPLY\" moodCode=\"RQO\">");
                xml.append("<quantity value=\"").append(terapia.getQuantity()).append("\"/>");
                xml.append("</supply>");
                xml.append("</entryRelationship>");
            }

            xml.append("</substanceAdministration>");
            xml.append("</entry>");
        }

        xml.append("</section>");
        xml.append("</component>");
    }

    private void appendTherapyParticipantRef(StringBuilder xml, RsaDocument.ParticipantRef participant) {
        xml.append("<participant typeCode=\"REF\">");
        if (hasText(participant.getEventTime())) {
            xml.append("<time value=\"").append(a(ts(participant.getEventTime()))).append("\"/>");
        }
        xml.append("<participantRole>");
        xml.append("<id root=\"2.16.840.1.113883.2.9.4.3.2\" extension=\"")
                .append(a(participant.getFiscalCode()))
                .append("\" assigningAuthorityName=\"MEF\"/>");
        xml.append("<playingEntity>");
        xml.append("<name>");
        xml.append("<family>").append(t(participant.getLastName())).append("</family>");
        xml.append("<given>").append(t(participant.getFirstName())).append("</given>");
        xml.append("</name>");
        xml.append("</playingEntity>");
        xml.append("</participantRole>");
        xml.append("</participant>");
    }

    private RsaDocument.ClinicalContent safeClinical(RsaDocument document) {
        return document != null && document.getClinicalContent() != null
                ? document.getClinicalContent()
                : new RsaDocument.ClinicalContent();
    }

    private List<RsaDocument.Allergy> validAllergies(List<RsaDocument.Allergy> allergies) {
        List<RsaDocument.Allergy> result = new ArrayList<>();
        if (allergies == null) {
            return result;
        }
        for (RsaDocument.Allergy a : allergies) {
            if (a != null) {
                result.add(a);
            }
        }
        return result;
    }

    private List<RsaDocument.PrecedenteEsame> validPreviousExams(List<RsaDocument.PrecedenteEsame> exams) {
        List<RsaDocument.PrecedenteEsame> result = new ArrayList<>();
        if (exams == null) {
            return result;
        }
        for (RsaDocument.PrecedenteEsame e : exams) {
            if (e != null && (hasText(e.getDescription()) || hasText(e.getOutcomeText()) || e.getCoding() != null)) {
                result.add(e);
            }
        }
        return result;
    }

    private List<RsaDocument.Prestazione> validPrestazioni(List<RsaDocument.Prestazione> prestazioni) {
        List<RsaDocument.Prestazione> result = new ArrayList<>();
        if (prestazioni == null) {
            return result;
        }
        for (RsaDocument.Prestazione p : prestazioni) {
            if (p != null && (hasText(p.getText()) || p.getCoding() != null)) {
                result.add(p);
            }
        }
        return result;
    }

    private List<RsaDocument.ProceduraOperativa> validProcedure(List<RsaDocument.ProceduraOperativa> procedure) {
        List<RsaDocument.ProceduraOperativa> result = new ArrayList<>();
        if (procedure == null) {
            return result;
        }
        for (RsaDocument.ProceduraOperativa p : procedure) {
            if (p != null && (hasText(p.getText()) || p.getCoding() != null)) {
                result.add(p);
            }
        }
        return result;
    }

    private List<RsaDocument.CodedText> validDiagnosi(List<RsaDocument.CodedText> diagnosi) {
        List<RsaDocument.CodedText> result = new ArrayList<>();
        if (diagnosi == null) {
            return result;
        }
        for (RsaDocument.CodedText d : diagnosi) {
            if (d != null && (hasText(d.getText()) || d.getCoding() != null)) {
                result.add(d);
            }
        }
        return result;
    }

    private List<RsaDocument.AccertamentoControllo> validAccertamenti(List<RsaDocument.AccertamentoControllo> accertamenti) {
        List<RsaDocument.AccertamentoControllo> result = new ArrayList<>();
        if (accertamenti == null) {
            return result;
        }
        for (RsaDocument.AccertamentoControllo a : accertamenti) {
            if (a != null && (hasText(a.getText()) || a.getCoding() != null)) {
                result.add(a);
            }
        }
        return result;
    }

    private List<RsaDocument.TerapiaFarmacologica> validTherapies(List<RsaDocument.TerapiaFarmacologica> terapie, boolean requirePeriod) {
        List<RsaDocument.TerapiaFarmacologica> result = new ArrayList<>();
        if (terapie == null) {
            return result;
        }
        for (RsaDocument.TerapiaFarmacologica t : terapie) {
            if (t == null) {
                continue;
            }
            boolean hasDrug = hasText(t.getAicCode()) || hasText(t.getText()) || hasText(t.getAicDisplayName());
            boolean hasPeriod = hasText(t.getStartDateTime()) && hasText(t.getEndDateTime());
            if (hasDrug && (!requirePeriod || hasPeriod)) {
                result.add(t);
            }
        }
        return result;
    }

    private boolean hasStructuredStoriaClinicaContent(RsaDocument.StoriaClinica storia) {
        return hasText(storia.getAnamnesiPatologicaRemotaText())
                || hasText(storia.getAnamnesiPatologicaProssimaText())
                || hasText(storia.getAnamnesiPatologicaFisiologicaText())
                || storia.getProblemCoding() != null
                || (storia.getFamilyHistories() != null && !storia.getFamilyHistories().isEmpty())
                || (storia.getAllergies() != null && !storia.getAllergies().isEmpty())
                || (storia.getTerapiaFarmacologicaInAtto() != null && !storia.getTerapiaFarmacologicaInAtto().isEmpty());
    }

    private String joinFamilyHistoryNarrative(List<RsaDocument.FamilyHistory> histories) {
        if (histories == null || histories.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (RsaDocument.FamilyHistory h : histories) {
            if (h == null) continue;
            if (out.length() > 0) out.append("; ");
            out.append(defaultIfBlank(h.getRelationshipDisplayName(), h.getRelationshipCode()))
                    .append(": ")
                    .append(h.getDiagnosisCoding() != null
                            ? defaultIfBlank(h.getDiagnosisCoding().getDisplayName(), h.getDiagnosisCoding().getCode())
                            : "");
        }
        return out.toString();
    }

    private String joinAllergyNarrative(List<RsaDocument.Allergy> allergies) {
        if (allergies == null || allergies.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (RsaDocument.Allergy a : allergies) {
            if (a == null) continue;
            if (out.length() > 0) out.append("; ");
            out.append(defaultIfBlank(
                    a.getNarrativeText(),
                    a.getAgentCoding() != null ? a.getAgentCoding().getDisplayName() : ""
            ));
        }
        return out.toString();
    }

    private void appendCodingAttributes(StringBuilder xml, RsaDocument.Coding coding) {
        xml.append(" code=\"").append(a(coding.getCode())).append("\"");
        xml.append(" codeSystem=\"").append(a(coding.getCodeSystem())).append("\"");
        if (hasText(coding.getCodeSystemName())) {
            xml.append(" codeSystemName=\"").append(a(coding.getCodeSystemName())).append("\"");
        }
        if (hasText(coding.getDisplayName())) {
            xml.append(" displayName=\"").append(a(coding.getDisplayName())).append("\"");
        }
    }

    private String resolveGenderDisplayName(String genderCode) {
        return switch (genderCode) {
            case "M" -> "MASCHIO";
            case "F" -> "FEMMINA";
            default -> "";
        };
    }

    private String ts(String value) {
        if (!hasText(value)) {
            return "";
        }
        return value.replace("-", "").replace(":", "").replace("T", "");
    }

    private boolean hasText(String value) {
        return XmlEscaper.hasText(value);
    }

    private String defaultIfBlank(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private String t(String value) {
        return XmlEscaper.escapeText(value);
    }

    private String a(String value) {
        return XmlEscaper.escapeAttribute(value);
    }
}