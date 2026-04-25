package it.thcs.fse.fsersaservice.infrastructure.cda;

import it.thcs.fse.fsersaservice.config.properties.OidProperties;
import it.thcs.fse.fsersaservice.domain.model.RsaDocument;
import it.thcs.fse.fsersaservice.support.util.XmlEscaper;
import org.springframework.stereotype.Component;

@Component
public class CdaPartiesAndContextFragmentBuilder {

    private static final String NATIONAL_ORGANIZATION_ROOT = "2.16.840.1.113883.2.9.4.1.2";
    private static final String NATIONAL_REGION_ROOT = "2.16.840.1.113883.2.9.4.1.1";
    private static final String FACILITY_ROOT = "2.16.840.1.113883.2.9.4.1.6";
    private static final String NRE_ROOT = "2.16.840.1.113883.2.9.4.3.9";

    private final OidProperties oidProperties;

    public CdaPartiesAndContextFragmentBuilder(OidProperties oidProperties) {
        this.oidProperties = oidProperties;
    }

    public String build(RsaDocument rsaDocument, String documentIdExtension, String generationTimeValue) {
        StringBuilder xml = new StringBuilder();

        xml.append(buildRecordTarget(rsaDocument.getPatient()));
        xml.append(buildAuthor(rsaDocument.getAuthor(), generationTimeValue));
        xml.append(buildDataEnterer(rsaDocument.getDataEnterer(), generationTimeValue));
        xml.append(buildCustodian(rsaDocument.getCustodian()));
        xml.append(buildLegalAuthenticator(rsaDocument.getLegalAuthenticator(), generationTimeValue));
        xml.append(buildParticipantRef(rsaDocument.getParticipantRef(), generationTimeValue));
        xml.append(buildInFulfillmentOf(rsaDocument.getOrder()));
        xml.append(buildDocumentationOf(rsaDocument.getServiceEvent()));
        xml.append(buildComponentOf(rsaDocument.getEncounter(), rsaDocument.getCustodian()));

        return xml.toString();
    }

    private String buildRecordTarget(RsaDocument.Person patient) {
        if (patient == null) {
            return "";
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<recordTarget>");
        xml.append("<patientRole>");

        appendFiscalCodeId(xml, patient.getFiscalCode());

        if (patient.getAddress() != null) {
            xml.append("<addr use=\"H\">");
            appendAddress(xml, patient.getAddress(), true);
            xml.append("</addr>");
        }

        appendPatientTelecom(xml, patient.getContact());

        xml.append("<patient>");
        xml.append("<name>");
        xml.append("<family>").append(XmlEscaper.escapeText(patient.getLastName())).append("</family>");
        xml.append("<given>").append(XmlEscaper.escapeText(patient.getFirstName())).append("</given>");
        xml.append("</name>");

        if (XmlEscaper.hasText(patient.getGenderCode())) {
            xml.append("<administrativeGenderCode code=\"")
                    .append(XmlEscaper.escapeAttribute(patient.getGenderCode()))
                    .append("\" codeSystem=\"2.16.840.1.113883.5.1\" codeSystemName=\"HL7 AdministrativeGender\" displayName=\"")
                    .append(XmlEscaper.escapeAttribute(resolveGenderDisplayName(patient.getGenderCode())))
                    .append("\"/>");
        }

        if (XmlEscaper.hasText(patient.getBirthDate())) {
            xml.append("<birthTime value=\"")
                    .append(XmlEscaper.escapeAttribute(formatDate(patient.getBirthDate())))
                    .append("\"/>");
        }

        if (patient.getBirthPlace() != null) {
            xml.append("<birthplace><place><addr>");
            appendPlace(xml, patient.getBirthPlace());
            xml.append("</addr></place></birthplace>");
        }

        xml.append("</patient>");
        xml.append("</patientRole>");
        xml.append("</recordTarget>");
        return xml.toString();
    }

    private String buildAuthor(RsaDocument.Professional author, String defaultTime) {
        if (author == null) {
            return "";
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<author>");
        xml.append("<time value=\"").append(XmlEscaper.escapeAttribute(defaultTime)).append("\"/>");
        xml.append("<assignedAuthor classCode=\"ASSIGNED\">");

        appendFiscalCodeId(xml, author.getFiscalCode());

        if (author.getAddress() != null) {
            xml.append("<addr use=\"HP\">");
            appendAddress(xml, author.getAddress(), true);
            xml.append("</addr>");
        }

        appendAuthorTelecom(xml, author.getContact());

        xml.append("<assignedPerson>");
        appendPersonName(xml, author.getFirstName(), author.getLastName(), author.getPrefix());
        xml.append("</assignedPerson>");

        xml.append("</assignedAuthor>");
        xml.append("</author>");
        return xml.toString();
    }

    private String buildDataEnterer(RsaDocument.Professional dataEnterer, String defaultTime) {
        if (dataEnterer == null) {
            return "";
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<dataEnterer typeCode=\"ENT\">");
        xml.append("<time value=\"").append(XmlEscaper.escapeAttribute(defaultTime)).append("\"/>");
        xml.append("<assignedEntity>");

        appendFiscalCodeId(xml, dataEnterer.getFiscalCode());

        if (dataEnterer.getAddress() != null) {
            xml.append("<addr>");
            appendAddress(xml, dataEnterer.getAddress(), false);
            xml.append("</addr>");
        }

        appendHpMcTelecom(xml, dataEnterer.getContact());

        xml.append("<assignedPerson>");
        appendPersonName(xml, dataEnterer.getFirstName(), dataEnterer.getLastName(), null);
        xml.append("</assignedPerson>");

        xml.append("</assignedEntity>");
        xml.append("</dataEnterer>");
        return xml.toString();
    }

    private String buildCustodian(RsaDocument.Custodian custodian) {
        if (custodian == null) {
            return "";
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<custodian>");
        xml.append("<assignedCustodian>");
        xml.append("<representedCustodianOrganization>");

        xml.append("<id root=\"")
                .append(NATIONAL_ORGANIZATION_ROOT)
                .append("\" extension=\"")
                .append(XmlEscaper.escapeAttribute(custodian.getCode()))
                .append("\" assigningAuthorityName=\"Ministero della Salute\"/>");

        xml.append("<name>").append(XmlEscaper.escapeText(custodian.getName())).append("</name>");

        if (XmlEscaper.hasText(custodian.getPhone())) {
            xml.append("<telecom use=\"WP\" value=\"tel:")
                    .append(XmlEscaper.escapeAttribute(custodian.getPhone()))
                    .append("\"/>");
        }

        if (custodian.getAddress() != null) {
            xml.append("<addr>");
            appendAddress(xml, custodian.getAddress(), false);
            xml.append("</addr>");
        }

        xml.append("</representedCustodianOrganization>");
        xml.append("</assignedCustodian>");
        xml.append("</custodian>");
        return xml.toString();
    }

    private String buildLegalAuthenticator(RsaDocument.Professional legalAuthenticator, String defaultTime) {
        if (legalAuthenticator == null) {
            return "";
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<legalAuthenticator>");
        xml.append("<time value=\"").append(XmlEscaper.escapeAttribute(defaultTime)).append("\"/>");
        xml.append("<signatureCode code=\"S\"/>");
        xml.append("<assignedEntity>");

        appendFiscalCodeId(xml, legalAuthenticator.getFiscalCode());

        if (legalAuthenticator.getAddress() != null) {
            xml.append("<addr>");
            appendAddress(xml, legalAuthenticator.getAddress(), false);
            xml.append("</addr>");
        }

        appendHpMcTelecom(xml, legalAuthenticator.getContact());

        xml.append("<assignedPerson>");
        appendPersonName(xml, legalAuthenticator.getFirstName(), legalAuthenticator.getLastName(), legalAuthenticator.getPrefix());
        xml.append("</assignedPerson>");

        xml.append("</assignedEntity>");
        xml.append("</legalAuthenticator>");
        return xml.toString();
    }

    private String buildParticipantRef(RsaDocument.ParticipantRef participantRef, String defaultTime) {
        if (participantRef == null) {
            return "";
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<participant typeCode=\"REF\">");
        xml.append("<time value=\"")
                .append(XmlEscaper.escapeAttribute(
                        XmlEscaper.hasText(participantRef.getEventTime()) ? formatTs(participantRef.getEventTime()) : defaultTime
                ))
                .append("\"/>");
        xml.append("<associatedEntity classCode=\"PROV\">");

        appendFiscalCodeId(xml, participantRef.getFiscalCode());

        if (participantRef.getOccupationCoding() != null) {
            xml.append("<code");
            appendCodingAttributes(xml, participantRef.getOccupationCoding());
            xml.append("/>");
        }

        if (participantRef.getAddress() != null) {
            xml.append("<addr>");
            appendAddress(xml, participantRef.getAddress(), true);
            xml.append("</addr>");
        }

        if (participantRef.getContact() != null && XmlEscaper.hasText(participantRef.getContact().getEmail())) {
            xml.append("<telecom use=\"HP\" value=\"mailto:")
                    .append(XmlEscaper.escapeAttribute(participantRef.getContact().getEmail()))
                    .append("\"/>");
        }

        xml.append("<associatedPerson>");
        appendPersonName(xml, participantRef.getFirstName(), participantRef.getLastName(), participantRef.getPrefix());
        xml.append("</associatedPerson>");

        xml.append("</associatedEntity>");
        xml.append("</participant>");
        return xml.toString();
    }

    private String buildInFulfillmentOf(RsaDocument.Order order) {
        if (order == null) {
            return "";
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<inFulfillmentOf>");
        xml.append("<order classCode=\"ACT\" moodCode=\"RQO\">");
        xml.append("<id root=\"")
                .append(NRE_ROOT)
                .append("\" extension=\"")
                .append(XmlEscaper.escapeAttribute(order.getNre()))
                .append("\" assigningAuthorityName=\"MEF\"/>");
        xml.append("<priorityCode code=\"")
                .append(XmlEscaper.escapeAttribute(order.getPriorityCode()))
                .append("\" codeSystem=\"2.16.840.1.113883.5.7\" codeSystemName=\"HL7 ActPriority\" displayName=\"")
                .append(XmlEscaper.escapeAttribute(resolvePriorityDisplayName(order.getPriorityCode())))
                .append("\"/>");
        xml.append("</order>");
        xml.append("</inFulfillmentOf>");
        return xml.toString();
    }

    private String buildDocumentationOf(RsaDocument.ServiceEvent serviceEvent) {
        if (serviceEvent == null) {
            return "";
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<documentationOf>");
        xml.append("<serviceEvent>");
        xml.append("<code code=\"")
                .append(XmlEscaper.escapeAttribute(serviceEvent.getEventCode()))
                .append("\" codeSystem=\"2.16.840.1.113883.2.9.5.1.4\" codeSystemName=\"ActCode\" displayName=\"")
                .append(XmlEscaper.escapeAttribute(resolveServiceEventDisplayName(serviceEvent.getEventCode())))
                .append("\"/>");
        if (XmlEscaper.hasText(serviceEvent.getEffectiveTime())) {
            xml.append("<effectiveTime value=\"")
                    .append(XmlEscaper.escapeAttribute(formatTs(serviceEvent.getEffectiveTime())))
                    .append("\"/>");
        }
        xml.append("</serviceEvent>");
        xml.append("</documentationOf>");
        return xml.toString();
    }

    private String buildComponentOf(RsaDocument.Encounter encounter, RsaDocument.Custodian custodian) {
        if (encounter == null) {
            return "";
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<componentOf>");
        xml.append("<encompassingEncounter>");

        xml.append("<id root=\"")
                .append(XmlEscaper.escapeAttribute(oidProperties.getPugliaRoot()))
                .append("\" extension=\"")
                .append(XmlEscaper.escapeAttribute(encounter.getEncounterId()))
                .append("\" assigningAuthorityName=\"MEF\"/>");

        xml.append("<effectiveTime>");
        if (XmlEscaper.hasText(encounter.getStartDateTime())) {
            xml.append("<low value=\"")
                    .append(XmlEscaper.escapeAttribute(formatTs(encounter.getStartDateTime())))
                    .append("\"/>");
        }
        if (XmlEscaper.hasText(encounter.getEndDateTime())) {
            xml.append("<high value=\"")
                    .append(XmlEscaper.escapeAttribute(formatTs(encounter.getEndDateTime())))
                    .append("\"/>");
        }
        xml.append("</effectiveTime>");

        xml.append("<location>");
        xml.append("<healthCareFacility>");

        xml.append("<id root=\"")
                .append(FACILITY_ROOT)
                .append("\" extension=\"")
                .append(XmlEscaper.escapeAttribute(encounter.getFacilityCode()))
                .append("\" assigningAuthorityName=\"Ministero della Salute\"/>");

        xml.append("<location>");
        xml.append("<name>").append(XmlEscaper.escapeText(encounter.getFacilityName())).append("</name>");
        if (encounter.getFacilityAddress() != null) {
            xml.append("<addr>");
            appendAddress(xml, encounter.getFacilityAddress(), true);
            xml.append("</addr>");
        }
        xml.append("</location>");

        if (custodian != null) {
            xml.append("<serviceProviderOrganization>");
            xml.append("<id root=\"")
                    .append(NATIONAL_ORGANIZATION_ROOT)
                    .append("\" extension=\"")
                    .append(XmlEscaper.escapeAttribute(custodian.getCode()))
                    .append("\" assigningAuthorityName=\"Ministero della Salute\"/>");
            xml.append("<asOrganizationPartOf>");
            xml.append("<id root=\"")
                    .append(NATIONAL_REGION_ROOT)
                    .append("\" extension=\"")
                    .append(XmlEscaper.escapeAttribute(parentOrganizationCode(custodian.getCode())))
                    .append("\" assigningAuthorityName=\"Ministero della Salute\"/>");
            xml.append("</asOrganizationPartOf>");
            xml.append("</serviceProviderOrganization>");
        }

        xml.append("</healthCareFacility>");
        xml.append("</location>");
        xml.append("</encompassingEncounter>");
        xml.append("</componentOf>");
        return xml.toString();
    }

    private void appendFiscalCodeId(StringBuilder xml, String fiscalCode) {
        xml.append("<id root=\"")
                .append(XmlEscaper.escapeAttribute(oidProperties.getCodiceFiscaleRoot()))
                .append("\" extension=\"")
                .append(XmlEscaper.escapeAttribute(fiscalCode))
                .append("\" assigningAuthorityName=\"MEF\"/>");
    }

    private void appendPersonName(StringBuilder xml, String firstName, String lastName, String prefix) {
        xml.append("<name>");
        xml.append("<family>").append(XmlEscaper.escapeText(lastName)).append("</family>");
        xml.append("<given>").append(XmlEscaper.escapeText(firstName)).append("</given>");
        if (XmlEscaper.hasText(prefix)) {
            xml.append("<prefix>").append(XmlEscaper.escapeText(prefix)).append("</prefix>");
        }
        xml.append("</name>");
    }

    private void appendAuthorTelecom(StringBuilder xml, RsaDocument.Contact contact) {
        if (contact == null) {
            return;
        }
        if (XmlEscaper.hasText(contact.getEmail())) {
            xml.append("<telecom use=\"HP\" value=\"mailto:")
                    .append(XmlEscaper.escapeAttribute(contact.getEmail()))
                    .append("\"/>");
            xml.append("<telecom use=\"WP\" value=\"mailto:")
                    .append(XmlEscaper.escapeAttribute(contact.getEmail()))
                    .append("\"/>");
        }
        if (XmlEscaper.hasText(contact.getMobile())) {
            xml.append("<telecom use=\"MC\" value=\"tel:")
                    .append(XmlEscaper.escapeAttribute(contact.getMobile()))
                    .append("\"/>");
        }
    }

    private void appendHpMcTelecom(StringBuilder xml, RsaDocument.Contact contact) {
        if (contact == null) {
            return;
        }
        if (XmlEscaper.hasText(contact.getEmail())) {
            xml.append("<telecom use=\"HP\" value=\"mailto:")
                    .append(XmlEscaper.escapeAttribute(contact.getEmail()))
                    .append("\"/>");
        }
        if (XmlEscaper.hasText(contact.getMobile())) {
            xml.append("<telecom use=\"MC\" value=\"tel:")
                    .append(XmlEscaper.escapeAttribute(contact.getMobile()))
                    .append("\"/>");
        }
    }

    private void appendPatientTelecom(StringBuilder xml, RsaDocument.Contact contact) {
        if (contact == null) {
            return;
        }
        if (XmlEscaper.hasText(contact.getEmail())) {
            xml.append("<telecom use=\"HP\" value=\"mailto:")
                    .append(XmlEscaper.escapeAttribute(contact.getEmail()))
                    .append("\"/>");
        }
        if (XmlEscaper.hasText(contact.getMobile())) {
            xml.append("<telecom use=\"MC\" value=\"tel:")
                    .append(XmlEscaper.escapeAttribute(contact.getMobile()))
                    .append("\"/>");
        }
    }

    private void appendAddress(StringBuilder xml, RsaDocument.Address address, boolean includePostalCode) {
        xml.append("<country>").append(XmlEscaper.escapeText(address.getCountryCode())).append("</country>");
        xml.append("<state>").append(XmlEscaper.escapeText(address.getRegionCode())).append("</state>");
        xml.append("<county>").append(XmlEscaper.escapeText(address.getProvinceCode())).append("</county>");
        xml.append("<city>").append(XmlEscaper.escapeText(address.getCity())).append("</city>");
        xml.append("<censusTract>").append(XmlEscaper.escapeText(address.getIstatCityCode())).append("</censusTract>");
        if (includePostalCode && XmlEscaper.hasText(address.getPostalCode())) {
            xml.append("<postalCode>").append(XmlEscaper.escapeText(address.getPostalCode())).append("</postalCode>");
        }
        xml.append("<streetAddressLine>").append(XmlEscaper.escapeText(address.getStreetAddress())).append("</streetAddressLine>");
    }

    private void appendPlace(StringBuilder xml, RsaDocument.Place place) {
        xml.append("<country>").append(XmlEscaper.escapeText(place.getCountryCode())).append("</country>");
        xml.append("<state>").append(XmlEscaper.escapeText(place.getRegionCode())).append("</state>");
        xml.append("<county>").append(XmlEscaper.escapeText(place.getProvinceCode())).append("</county>");
        xml.append("<city>").append(XmlEscaper.escapeText(place.getCity())).append("</city>");
        xml.append("<censusTract>").append(XmlEscaper.escapeText(place.getIstatCityCode())).append("</censusTract>");
    }

    private void appendCodingAttributes(StringBuilder xml, RsaDocument.Coding coding) {
        xml.append(" code=\"").append(XmlEscaper.escapeAttribute(coding.getCode())).append("\"");
        xml.append(" codeSystem=\"").append(XmlEscaper.escapeAttribute(coding.getCodeSystem())).append("\"");
        if (XmlEscaper.hasText(coding.getCodeSystemName())) {
            xml.append(" codeSystemName=\"").append(XmlEscaper.escapeAttribute(coding.getCodeSystemName())).append("\"");
        }
        if (XmlEscaper.hasText(coding.getDisplayName())) {
            xml.append(" displayName=\"").append(XmlEscaper.escapeAttribute(coding.getDisplayName())).append("\"");
        }
    }

    private String parentOrganizationCode(String custodianCode) {
        if (!XmlEscaper.hasText(custodianCode)) {
            return "";
        }
        int idx = custodianCode.indexOf('.');
        return idx > 0 ? custodianCode.substring(0, idx) : custodianCode;
    }

    private String formatTs(String value) {
        return value.replace("-", "").replace(":", "").replace("T", "");
    }

    private String formatDate(String value) {
        return value.replace("-", "");
    }

    private String resolveGenderDisplayName(String genderCode) {
        return switch (genderCode) {
            case "M" -> "MASCHIO";
            case "F" -> "FEMMINA";
            default -> "";
        };
    }

    private String resolvePriorityDisplayName(String priorityCode) {
        return switch (priorityCode) {
            case "R" -> "Normale";
            case "UR" -> "Urgente";
            default -> "";
        };
    }

    private String resolveServiceEventDisplayName(String eventCode) {
        return switch (eventCode) {
            case "PROG" -> "Accesso Programmato";
            case "DIR" -> "Accesso Diretto";
            default -> "";
        };
    }
}