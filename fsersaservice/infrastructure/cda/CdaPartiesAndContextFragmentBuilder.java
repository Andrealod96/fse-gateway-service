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
    private static final String ADMINISTRATIVE_GENDER_CODE_SYSTEM = "2.16.840.1.113883.5.1";
    private static final String ADMINISTRATIVE_GENDER_CODE_SYSTEM_NAME = "HL7 AdministrativeGender";
    private static final String ACT_PRIORITY_CODE_SYSTEM = "2.16.840.1.113883.5.7";
    private static final String ACT_PRIORITY_CODE_SYSTEM_NAME = "HL7 ActPriority";
    private static final String SERVICE_EVENT_CODE_SYSTEM = "2.16.840.1.113883.2.9.5.1.4";
    private static final String SERVICE_EVENT_CODE_SYSTEM_NAME = "ActCode";

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
        appendAddressElement(xml, "H", patient.getAddress(), true);
        appendPatientTelecom(xml, patient.getContact());

        xml.append("<patient>");
        appendPersonName(xml, patient.getFirstName(), patient.getLastName(), null);
        appendAdministrativeGenderCode(xml, patient.getGenderCode());
        appendBirthTime(xml, patient.getBirthDate());
        appendBirthPlace(xml, patient.getBirthPlace());
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
        appendTime(xml, defaultTime);
        xml.append("<assignedAuthor classCode=\"ASSIGNED\">");

        appendFiscalCodeId(xml, author.getFiscalCode());
        appendAddressElement(xml, "HP", author.getAddress(), true);
        appendAuthorTelecom(xml, author.getContact());
        appendAssignedPerson(xml, author.getFirstName(), author.getLastName(), author.getPrefix());

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
        appendTime(xml, defaultTime);
        xml.append("<assignedEntity>");

        appendFiscalCodeId(xml, dataEnterer.getFiscalCode());
        appendAddressElement(xml, null, dataEnterer.getAddress(), false);
        appendHpMcTelecom(xml, dataEnterer.getContact());
        appendAssignedPerson(xml, dataEnterer.getFirstName(), dataEnterer.getLastName(), null);

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

        if (XmlEscaper.hasText(custodian.getCode())) {
            xml.append("<id root=\"")
                    .append(NATIONAL_ORGANIZATION_ROOT)
                    .append("\" extension=\"")
                    .append(XmlEscaper.escapeAttribute(custodian.getCode()))
                    .append("\" assigningAuthorityName=\"Ministero della Salute\"/>");
        }

        appendTextElementIfPresent(xml, "name", custodian.getName());

        if (XmlEscaper.hasText(custodian.getPhone())) {
            xml.append("<telecom use=\"WP\" value=\"tel:")
                    .append(XmlEscaper.escapeAttribute(custodian.getPhone()))
                    .append("\"/>");
        }

        appendAddressElement(xml, null, custodian.getAddress(), false);

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
        appendTime(xml, defaultTime);
        xml.append("<signatureCode code=\"")
                .append(XmlEscaper.escapeAttribute(
                        XmlEscaper.hasText(legalAuthenticator.getSignatureCode())
                                ? legalAuthenticator.getSignatureCode()
                                : "S"
                ))
                .append("\"/>");
        xml.append("<assignedEntity>");

        appendFiscalCodeId(xml, legalAuthenticator.getFiscalCode());
        appendAddressElement(xml, null, legalAuthenticator.getAddress(), false);
        appendHpMcTelecom(xml, legalAuthenticator.getContact());
        appendAssignedPerson(xml, legalAuthenticator.getFirstName(), legalAuthenticator.getLastName(), legalAuthenticator.getPrefix());

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

        if (hasAnyCodingValue(participantRef.getOccupationCoding())) {
            xml.append("<code");
            appendCodingAttributes(xml, participantRef.getOccupationCoding());
            xml.append("/>");
        }

        appendAddressElement(xml, null, participantRef.getAddress(), true);

        if (participantRef.getContact() != null && XmlEscaper.hasText(participantRef.getContact().getEmail())) {
            xml.append("<telecom use=\"HP\" value=\"mailto:")
                    .append(XmlEscaper.escapeAttribute(participantRef.getContact().getEmail()))
                    .append("\"/>");
        }

        appendAssociatedPerson(xml, participantRef.getFirstName(), participantRef.getLastName(), participantRef.getPrefix());

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

        if (XmlEscaper.hasText(order.getNre())) {
            xml.append("<id root=\"")
                    .append(NRE_ROOT)
                    .append("\" extension=\"")
                    .append(XmlEscaper.escapeAttribute(order.getNre()))
                    .append("\" assigningAuthorityName=\"MEF\"/>");
        }

        appendPriorityCode(xml, order.getPriorityCode());

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

        appendServiceEventCode(xml, serviceEvent.getEventCode());

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

        if (XmlEscaper.hasText(encounter.getEncounterId())) {
            xml.append("<id root=\"")
                    .append(XmlEscaper.escapeAttribute(oidProperties.getPugliaRoot()))
                    .append("\" extension=\"")
                    .append(XmlEscaper.escapeAttribute(encounter.getEncounterId()))
                    .append("\" assigningAuthorityName=\"MEF\"/>");
        }

        if (XmlEscaper.hasText(encounter.getStartDateTime()) || XmlEscaper.hasText(encounter.getEndDateTime())) {
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
        }

        xml.append("<location>");
        xml.append("<healthCareFacility>");

        if (XmlEscaper.hasText(encounter.getFacilityCode())) {
            xml.append("<id root=\"")
                    .append(FACILITY_ROOT)
                    .append("\" extension=\"")
                    .append(XmlEscaper.escapeAttribute(encounter.getFacilityCode()))
                    .append("\" assigningAuthorityName=\"Ministero della Salute\"/>");
        }

        xml.append("<location>");
        appendTextElementIfPresent(xml, "name", encounter.getFacilityName());
        appendAddressElement(xml, null, encounter.getFacilityAddress(), true);
        xml.append("</location>");

        appendServiceProviderOrganization(xml, custodian);

        xml.append("</healthCareFacility>");
        xml.append("</location>");
        xml.append("</encompassingEncounter>");
        xml.append("</componentOf>");
        return xml.toString();
    }

    private void appendAdministrativeGenderCode(StringBuilder xml, String genderCode) {
        if (!XmlEscaper.hasText(genderCode)) {
            return;
        }

        xml.append("<administrativeGenderCode");
        appendAttributeIfPresent(xml, "code", genderCode);
        appendAttributeIfPresent(xml, "codeSystem", ADMINISTRATIVE_GENDER_CODE_SYSTEM);
        appendAttributeIfPresent(xml, "codeSystemName", ADMINISTRATIVE_GENDER_CODE_SYSTEM_NAME);
        appendAttributeIfPresent(xml, "displayName", resolveGenderDisplayName(genderCode));
        xml.append("/>");
    }

    private void appendPriorityCode(StringBuilder xml, String priorityCode) {
        if (!XmlEscaper.hasText(priorityCode)) {
            return;
        }

        xml.append("<priorityCode");
        appendAttributeIfPresent(xml, "code", priorityCode);
        appendAttributeIfPresent(xml, "codeSystem", ACT_PRIORITY_CODE_SYSTEM);
        appendAttributeIfPresent(xml, "codeSystemName", ACT_PRIORITY_CODE_SYSTEM_NAME);
        appendAttributeIfPresent(xml, "displayName", resolvePriorityDisplayName(priorityCode));
        xml.append("/>");
    }

    private void appendServiceEventCode(StringBuilder xml, String eventCode) {
        if (!XmlEscaper.hasText(eventCode)) {
            return;
        }

        xml.append("<code");
        appendAttributeIfPresent(xml, "code", eventCode);
        appendAttributeIfPresent(xml, "codeSystem", SERVICE_EVENT_CODE_SYSTEM);
        appendAttributeIfPresent(xml, "codeSystemName", SERVICE_EVENT_CODE_SYSTEM_NAME);
        appendAttributeIfPresent(xml, "displayName", resolveServiceEventDisplayName(eventCode));
        xml.append("/>");
    }

    private void appendBirthTime(StringBuilder xml, String birthDate) {
        if (!XmlEscaper.hasText(birthDate)) {
            return;
        }

        xml.append("<birthTime value=\"")
                .append(XmlEscaper.escapeAttribute(formatDate(birthDate)))
                .append("\"/>");
    }

    private void appendBirthPlace(StringBuilder xml, RsaDocument.Place birthPlace) {
        if (!hasAnyPlaceValue(birthPlace)) {
            return;
        }

        xml.append("<birthplace><place><addr>");
        appendPlace(xml, birthPlace);
        xml.append("</addr></place></birthplace>");
    }

    private void appendFiscalCodeId(StringBuilder xml, String fiscalCode) {
        if (!XmlEscaper.hasText(fiscalCode)) {
            return;
        }

        xml.append("<id root=\"")
                .append(XmlEscaper.escapeAttribute(oidProperties.getCodiceFiscaleRoot()))
                .append("\" extension=\"")
                .append(XmlEscaper.escapeAttribute(fiscalCode))
                .append("\" assigningAuthorityName=\"MEF\"/>");
    }

    private void appendAssignedPerson(StringBuilder xml, String firstName, String lastName, String prefix) {
        if (!hasAnyNameValue(firstName, lastName, prefix)) {
            return;
        }

        xml.append("<assignedPerson>");
        appendPersonName(xml, firstName, lastName, prefix);
        xml.append("</assignedPerson>");
    }

    private void appendAssociatedPerson(StringBuilder xml, String firstName, String lastName, String prefix) {
        if (!hasAnyNameValue(firstName, lastName, prefix)) {
            return;
        }

        xml.append("<associatedPerson>");
        appendPersonName(xml, firstName, lastName, prefix);
        xml.append("</associatedPerson>");
    }

    private void appendPersonName(StringBuilder xml, String firstName, String lastName, String prefix) {
        if (!hasAnyNameValue(firstName, lastName, prefix)) {
            return;
        }

        xml.append("<name>");
        appendTextElementIfPresent(xml, "family", lastName);
        appendTextElementIfPresent(xml, "given", firstName);
        appendTextElementIfPresent(xml, "prefix", prefix);
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

    private void appendAddressElement(StringBuilder xml, String use, RsaDocument.Address address, boolean includePostalCode) {
        if (!hasAnyAddressValue(address)) {
            return;
        }

        xml.append("<addr");
        if (XmlEscaper.hasText(use)) {
            xml.append(" use=\"").append(XmlEscaper.escapeAttribute(use)).append("\"");
        }
        xml.append(">");
        appendAddress(xml, address, includePostalCode);
        xml.append("</addr>");
    }

    private void appendAddress(StringBuilder xml, RsaDocument.Address address, boolean includePostalCode) {
        if (address == null) {
            return;
        }

        appendTextElementIfPresent(xml, "country", address.getCountryCode());
        appendTextElementIfPresent(xml, "state", address.getRegionCode());
        appendTextElementIfPresent(xml, "county", address.getProvinceCode());
        appendTextElementIfPresent(xml, "city", address.getCity());
        appendTextElementIfPresent(xml, "censusTract", address.getIstatCityCode());

        if (includePostalCode) {
            appendTextElementIfPresent(xml, "postalCode", address.getPostalCode());
        }

        appendTextElementIfPresent(xml, "streetAddressLine", address.getStreetAddress());
    }

    private void appendPlace(StringBuilder xml, RsaDocument.Place place) {
        if (place == null) {
            return;
        }

        appendTextElementIfPresent(xml, "country", place.getCountryCode());
        appendTextElementIfPresent(xml, "state", place.getRegionCode());
        appendTextElementIfPresent(xml, "county", place.getProvinceCode());
        appendTextElementIfPresent(xml, "city", place.getCity());
        appendTextElementIfPresent(xml, "censusTract", place.getIstatCityCode());
    }

    private void appendServiceProviderOrganization(StringBuilder xml, RsaDocument.Custodian custodian) {
        if (custodian == null || !XmlEscaper.hasText(custodian.getCode())) {
            return;
        }

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

    private void appendTime(StringBuilder xml, String timeValue) {
        if (!XmlEscaper.hasText(timeValue)) {
            return;
        }

        xml.append("<time value=\"")
                .append(XmlEscaper.escapeAttribute(timeValue))
                .append("\"/>");
    }

    private void appendTextElementIfPresent(StringBuilder xml, String elementName, String value) {
        if (!XmlEscaper.hasText(value)) {
            return;
        }

        xml.append("<")
                .append(elementName)
                .append(">")
                .append(XmlEscaper.escapeText(value))
                .append("</")
                .append(elementName)
                .append(">");
    }

    private void appendCodingAttributes(StringBuilder xml, RsaDocument.Coding coding) {
        appendAttributeIfPresent(xml, "code", coding.getCode());
        appendAttributeIfPresent(xml, "codeSystem", coding.getCodeSystem());
        appendAttributeIfPresent(xml, "codeSystemName", coding.getCodeSystemName());
        appendAttributeIfPresent(xml, "displayName", coding.getDisplayName());
    }

    private void appendAttributeIfPresent(StringBuilder xml, String attributeName, String value) {
        if (!XmlEscaper.hasText(value)) {
            return;
        }
        xml.append(" ")
                .append(attributeName)
                .append("=\"")
                .append(XmlEscaper.escapeAttribute(value))
                .append("\"");
    }

    private boolean hasAnyAddressValue(RsaDocument.Address address) {
        return address != null && (
                XmlEscaper.hasText(address.getCountryCode())
                        || XmlEscaper.hasText(address.getRegionCode())
                        || XmlEscaper.hasText(address.getProvinceCode())
                        || XmlEscaper.hasText(address.getCity())
                        || XmlEscaper.hasText(address.getIstatCityCode())
                        || XmlEscaper.hasText(address.getPostalCode())
                        || XmlEscaper.hasText(address.getStreetAddress())
        );
    }

    private boolean hasAnyPlaceValue(RsaDocument.Place place) {
        return place != null && (
                XmlEscaper.hasText(place.getCountryCode())
                        || XmlEscaper.hasText(place.getRegionCode())
                        || XmlEscaper.hasText(place.getProvinceCode())
                        || XmlEscaper.hasText(place.getCity())
                        || XmlEscaper.hasText(place.getIstatCityCode())
        );
    }

    private boolean hasAnyCodingValue(RsaDocument.Coding coding) {
        return coding != null && (
                XmlEscaper.hasText(coding.getCode())
                        || XmlEscaper.hasText(coding.getCodeSystem())
                        || XmlEscaper.hasText(coding.getCodeSystemName())
                        || XmlEscaper.hasText(coding.getDisplayName())
        );
    }

    private boolean hasAnyNameValue(String firstName, String lastName, String prefix) {
        return XmlEscaper.hasText(firstName)
                || XmlEscaper.hasText(lastName)
                || XmlEscaper.hasText(prefix);
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
            case "UN" -> "UNDIFFERENZIATO";
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
