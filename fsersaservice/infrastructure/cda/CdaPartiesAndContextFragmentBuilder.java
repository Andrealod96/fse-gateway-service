package it.thcs.fse.fsersaservice.infrastructure.cda;

import it.thcs.fse.fsersaservice.config.properties.OidProperties;
import it.thcs.fse.fsersaservice.domain.model.RsaDocument;
import it.thcs.fse.fsersaservice.support.util.XmlEscaper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class CdaPartiesAndContextFragmentBuilder {

    private static final String NATIONAL_ORGANIZATION_ROOT = "2.16.840.1.113883.2.9.4.1.2";
    private static final String NRE_ROOT = "2.16.840.1.113883.2.9.4.3.9";
    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssZ");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

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
        xml.append(buildInFulfillmentOf(rsaDocument.getOrder()));
        xml.append(buildDocumentationOf(rsaDocument.getServiceEvent()));
        xml.append(buildComponentOf(rsaDocument.getEncounter(), rsaDocument.getCustodian(), documentIdExtension));

        return xml.toString();
    }

    private String buildRecordTarget(RsaDocument.Patient patient) {
        StringBuilder xml = new StringBuilder();

        xml.append("<recordTarget>");
        xml.append("<patientRole>");

        xml.append("<id root=\"")
                .append(XmlEscaper.escapeAttribute(oidProperties.getCodiceFiscaleRoot()))
                .append("\" extension=\"")
                .append(XmlEscaper.escapeAttribute(patient.getFiscalCode()))
                .append("\" assigningAuthorityName=\"MEF\"/>");

        xml.append("<addr use=\"H\">");
        appendAddress(xml, patient.getAddress(), true);
        xml.append("</addr>");

        appendContactTelecom(xml, patient.getContact(), "HP");

        xml.append("<patient>");
        xml.append("<name>");
        xml.append("<family>").append(XmlEscaper.escapeText(patient.getLastName())).append("</family>");
        xml.append("<given>").append(XmlEscaper.escapeText(patient.getFirstName())).append("</given>");
        xml.append("</name>");

        xml.append("<administrativeGenderCode code=\"")
                .append(XmlEscaper.escapeAttribute(patient.getGenderCode()))
                .append("\" codeSystem=\"2.16.840.1.113883.5.1\" codeSystemName=\"HL7 AdministrativeGender\" displayName=\"")
                .append(XmlEscaper.escapeAttribute(resolveGenderDisplayName(patient.getGenderCode())))
                .append("\"/>");

        xml.append("<birthTime value=\"")
                .append(XmlEscaper.escapeAttribute(formatDate(patient.getBirthDate())))
                .append("\"/>");

        xml.append("<birthplace><place><addr>");
        appendPlace(xml, patient.getBirthPlace());
        xml.append("</addr></place></birthplace>");

        xml.append("</patient>");
        xml.append("</patientRole>");
        xml.append("</recordTarget>");

        return xml.toString();
    }

    private String buildAuthor(RsaDocument.Practitioner author, String timeValue) {
        StringBuilder xml = new StringBuilder();

        xml.append("<author>");
        xml.append("<time value=\"").append(XmlEscaper.escapeAttribute(timeValue)).append("\"/>");
        xml.append("<assignedAuthor classCode=\"ASSIGNED\">");

        appendFiscalCodeId(xml, author.getFiscalCode());
        xml.append("<addr use=\"HP\">");
        appendAddress(xml, author.getAddress(), true);
        xml.append("</addr>");
        appendPractitionerTelecom(xml, author.getContact());

        xml.append("<assignedPerson>");
        appendPersonName(xml, author.getFirstName(), author.getLastName(), author.getPrefix());
        xml.append("</assignedPerson>");

        xml.append("</assignedAuthor>");
        xml.append("</author>");

        return xml.toString();
    }

    private String buildDataEnterer(RsaDocument.Practitioner dataEnterer, String timeValue) {
        if (dataEnterer == null) {
            return "";
        }

        StringBuilder xml = new StringBuilder();

        xml.append("<dataEnterer typeCode=\"ENT\">");
        xml.append("<time value=\"").append(XmlEscaper.escapeAttribute(timeValue)).append("\"/>");
        xml.append("<assignedEntity>");

        appendFiscalCodeId(xml, dataEnterer.getFiscalCode());

        xml.append("<addr>");
        appendAddress(xml, dataEnterer.getAddress(), false);
        xml.append("</addr>");

        appendWorkTelecom(xml, dataEnterer.getContact());

        xml.append("<assignedPerson>");
        appendPersonName(xml, dataEnterer.getFirstName(), dataEnterer.getLastName(), null);
        xml.append("</assignedPerson>");

        xml.append("</assignedEntity>");
        xml.append("</dataEnterer>");

        return xml.toString();
    }

    private String buildCustodian(RsaDocument.Custodian custodian) {
        StringBuilder xml = new StringBuilder();

        xml.append("<custodian>");
        xml.append("<assignedCustodian>");
        xml.append("<representedCustodianOrganization>");

        xml.append("<id root=\"")
                .append(XmlEscaper.escapeAttribute(oidProperties.getPugliaStructureRoot()))
                .append("\" extension=\"")
                .append(XmlEscaper.escapeAttribute(custodian.getCode()))
                .append("\" assigningAuthorityName=\"Regione Puglia\"/>");

        xml.append("<name>").append(XmlEscaper.escapeText(custodian.getName())).append("</name>");
        xml.append("<telecom use=\"WP\" value=\"tel:")
                .append(XmlEscaper.escapeAttribute(custodian.getPhone()))
                .append("\"/>");

        xml.append("<addr>");
        appendAddress(xml, custodian.getAddress(), false);
        xml.append("</addr>");

        xml.append("</representedCustodianOrganization>");
        xml.append("</assignedCustodian>");
        xml.append("</custodian>");

        return xml.toString();
    }

    private String buildLegalAuthenticator(RsaDocument.Practitioner legalAuthenticator, String timeValue) {
        StringBuilder xml = new StringBuilder();

        xml.append("<legalAuthenticator>");
        xml.append("<time value=\"").append(XmlEscaper.escapeAttribute(timeValue)).append("\"/>");
        xml.append("<signatureCode code=\"S\"/>");
        xml.append("<assignedEntity>");

        appendFiscalCodeId(xml, legalAuthenticator.getFiscalCode());

        xml.append("<addr>");
        appendAddress(xml, legalAuthenticator.getAddress(), false);
        xml.append("</addr>");

        appendWorkTelecom(xml, legalAuthenticator.getContact());

        xml.append("<assignedPerson>");
        appendPersonName(
                xml,
                legalAuthenticator.getFirstName(),
                legalAuthenticator.getLastName(),
                legalAuthenticator.getPrefix()
        );
        xml.append("</assignedPerson>");

        xml.append("</assignedEntity>");
        xml.append("</legalAuthenticator>");

        return xml.toString();
    }

    private String buildInFulfillmentOf(RsaDocument.Order order) {
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
        StringBuilder xml = new StringBuilder();

        xml.append("<documentationOf>");
        xml.append("<serviceEvent>");
        xml.append("<code code=\"")
                .append(XmlEscaper.escapeAttribute(serviceEvent.getEventCode()))
                .append("\" codeSystem=\"2.16.840.1.113883.2.9.5.1.4\" codeSystemName=\"ActCode\" displayName=\"")
                .append(XmlEscaper.escapeAttribute(resolveServiceEventDisplayName(serviceEvent.getEventCode())))
                .append("\"/>");

        xml.append("<effectiveTime value=\"")
                .append(XmlEscaper.escapeAttribute(formatTs(serviceEvent.getEffectiveTime())))
                .append("\"/>");

        xml.append("</serviceEvent>");
        xml.append("</documentationOf>");

        return xml.toString();
    }

    private String buildComponentOf(RsaDocument.Encounter encounter, RsaDocument.Custodian custodian, String documentIdExtension) {
        String encounterExtension = XmlEscaper.hasText(encounter.getEncounterId())
                ? encounter.getEncounterId()
                : "ENC-" + UUID.randomUUID();

        StringBuilder xml = new StringBuilder();

        xml.append("<componentOf>");
        xml.append("<encompassingEncounter>");

        xml.append("<id root=\"")
                .append(XmlEscaper.escapeAttribute(oidProperties.getPugliaRoot()))
                .append("\" extension=\"")
                .append(XmlEscaper.escapeAttribute(encounterExtension))
                .append("\" assigningAuthorityName=\"Regione Puglia\"/>");

        xml.append("<effectiveTime>");
        xml.append("<low value=\"").append(XmlEscaper.escapeAttribute(formatTs(encounter.getStartDateTime()))).append("\"/>");
        xml.append("<high value=\"").append(XmlEscaper.escapeAttribute(formatTs(encounter.getEndDateTime()))).append("\"/>");
        xml.append("</effectiveTime>");

        xml.append("<location>");
        xml.append("<healthCareFacility>");

        xml.append("<id root=\"")
                .append(XmlEscaper.escapeAttribute(oidProperties.getPugliaStructureRoot()))
                .append("\" extension=\"")
                .append(XmlEscaper.escapeAttribute(encounter.getFacilityCode()))
                .append("\" assigningAuthorityName=\"Regione Puglia\"/>");

        xml.append("<location>");
        xml.append("<name>").append(XmlEscaper.escapeText(encounter.getFacilityName())).append("</name>");
        xml.append("<addr>");
        appendAddress(xml, encounter.getFacilityAddress(), true);
        xml.append("</addr>");
        xml.append("</location>");

        xml.append("<serviceProviderOrganization>");
        xml.append("<id root=\"")
                .append(XmlEscaper.escapeAttribute(oidProperties.getPugliaStructureRoot()))
                .append("\" extension=\"")
                .append(XmlEscaper.escapeAttribute(custodian.getCode()))
                .append("\" assigningAuthorityName=\"Regione Puglia\"/>");
        xml.append("</serviceProviderOrganization>");

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

    private void appendPractitionerTelecom(StringBuilder xml, RsaDocument.Contact contact) {
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

    private void appendWorkTelecom(StringBuilder xml, RsaDocument.Contact contact) {
        if (contact == null) {
            return;
        }
        if (XmlEscaper.hasText(contact.getEmail())) {
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

    private void appendContactTelecom(StringBuilder xml, RsaDocument.Contact contact, String emailUse) {
        if (contact == null) {
            return;
        }
        if (XmlEscaper.hasText(contact.getEmail())) {
            xml.append("<telecom use=\"")
                    .append(emailUse)
                    .append("\" value=\"mailto:")
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

    private String formatTs(OffsetDateTime dateTime) {
        return TS_FORMATTER.format(dateTime.withNano(0));
    }

    private String formatDate(LocalDate date) {
        return DATE_FORMATTER.format(date);
    }

    private String resolveGenderDisplayName(String genderCode) {
        return switch (genderCode) {
            case "M" -> "MASCHIO";
            case "F" -> "FEMMINA";
            default -> "NON NOTO";
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