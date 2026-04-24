package it.thcs.fse.fsersaservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class RsaDocument {

    private final DocumentType documentType;
    private final String sourceSystem;
    private final String sourceRequestId;

    private final Patient patient;
    private final Practitioner author;
    private final Practitioner dataEnterer;
    private final Practitioner legalAuthenticator;
    private final Custodian custodian;
    private final Order order;
    private final ServiceEvent serviceEvent;
    private final Encounter encounter;
    private final ClinicalContent clinicalContent;

    public enum DocumentType {
        RSA
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Patient {
        private final String fiscalCode;
        private final String firstName;
        private final String lastName;
        private final String genderCode;
        private final LocalDate birthDate;
        private final Place birthPlace;
        private final Address address;
        private final Contact contact;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Practitioner {
        private final String fiscalCode;
        private final String firstName;
        private final String lastName;
        private final String prefix;
        private final Address address;
        private final Contact contact;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Custodian {
        private final String code;
        private final String name;
        private final String phone;
        private final Address address;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Order {
        private final String nre;
        private final String priorityCode;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ServiceEvent {
        private final String eventCode;
        private final OffsetDateTime effectiveTime;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Encounter {
        private final String encounterId;
        private final OffsetDateTime startDateTime;
        private final OffsetDateTime endDateTime;
        private final String facilityCode;
        private final String facilityName;
        private final Address facilityAddress;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ClinicalContent {
        private final QuesitoDiagnostico quesitoDiagnostico;
        private final List<Prestazione> prestazioni;
        private final String storiaClinica;
        private final String confrontoPrecedentiEsami;
        private final String refertoText;
        private final List<Diagnosis> diagnosi;
        private final String conclusioni;
        private final String suggerimentiMedicoPrescrittore;
        private final List<String> accertamentiControlliConsigliati;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class QuesitoDiagnostico {
        private final String text;
        private final Coding coding;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Prestazione {
        private final String text;
        private final Coding coding;
        private final OffsetDateTime performedAt;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Diagnosis {
        private final String text;
        private final Coding coding;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Coding {
        private final String code;
        private final String codeSystem;
        private final String codeSystemName;
        private final String displayName;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Address {
        private final String countryCode;
        private final String regionCode;
        private final String provinceCode;
        private final String city;
        private final String istatCityCode;
        private final String postalCode;
        private final String streetAddress;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Place {
        private final String countryCode;
        private final String regionCode;
        private final String provinceCode;
        private final String city;
        private final String istatCityCode;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Contact {
        private final String email;
        private final String mobile;
    }
}