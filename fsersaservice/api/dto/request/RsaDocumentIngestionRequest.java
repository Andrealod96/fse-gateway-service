package it.thcs.fse.fsersaservice.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RsaDocumentIngestionRequest {

    @NotNull
    private DocumentType documentType;

    @NotBlank
    private String sourceSystem;

    private String sourceRequestId;

    @NotNull
    @Valid
    private Patient patient;

    @NotNull
    @Valid
    private Practitioner author;

    @Valid
    private Practitioner dataEnterer;

    @NotNull
    @Valid
    private Practitioner legalAuthenticator;

    @NotNull
    @Valid
    private Custodian custodian;

    @NotNull
    @Valid
    private Order order;

    @NotNull
    @Valid
    private ServiceEvent serviceEvent;

    @NotNull
    @Valid
    private Encounter encounter;

    @NotNull
    @Valid
    private ClinicalContent clinicalContent;

    public enum DocumentType {
        RSA
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Patient {
        @NotBlank
        private String fiscalCode;

        @NotBlank
        private String firstName;

        @NotBlank
        private String lastName;

        @NotBlank
        private String genderCode;

        @NotNull
        private LocalDate birthDate;

        @NotNull
        @Valid
        private Place birthPlace;

        @NotNull
        @Valid
        private Address address;

        @Valid
        private Contact contact;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Practitioner {
        @NotBlank
        private String fiscalCode;

        @NotBlank
        private String firstName;

        @NotBlank
        private String lastName;

        private String prefix;

        @NotNull
        @Valid
        private Address address;

        @Valid
        private Contact contact;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Custodian {
        @NotBlank
        private String code;

        @NotBlank
        private String name;

        @NotBlank
        private String phone;

        @NotNull
        @Valid
        private Address address;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Order {
        @NotBlank
        private String nre;

        @NotBlank
        private String priorityCode;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceEvent {
        @NotBlank
        private String eventCode;

        @NotNull
        private OffsetDateTime effectiveTime;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Encounter {
        private String encounterId;

        @NotNull
        private OffsetDateTime startDateTime;

        @NotNull
        private OffsetDateTime endDateTime;

        @NotBlank
        private String facilityCode;

        @NotBlank
        private String facilityName;

        @NotNull
        @Valid
        private Address facilityAddress;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClinicalContent {
        @NotNull
        @Valid
        private QuesitoDiagnostico quesitoDiagnostico;

        @NotEmpty
        @Valid
        private List<Prestazione> prestazioni;

        private String storiaClinica;

        private String confrontoPrecedentiEsami;

        @NotBlank
        private String refertoText;

        @NotEmpty
        @Valid
        private List<Diagnosis> diagnosi;

        private String conclusioni;

        private String suggerimentiMedicoPrescrittore;

        private List<@NotBlank String> accertamentiControlliConsigliati;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuesitoDiagnostico {
        @NotBlank
        private String text;

        @NotNull
        @Valid
        private Coding coding;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Prestazione {
        @NotBlank
        private String text;

        @NotNull
        @Valid
        private Coding coding;

        @NotNull
        private OffsetDateTime performedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Diagnosis {
        @NotBlank
        private String text;

        @NotNull
        @Valid
        private Coding coding;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Coding {
        @NotBlank
        private String code;

        @NotBlank
        private String codeSystem;

        @NotBlank
        private String codeSystemName;

        @NotBlank
        private String displayName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        @NotBlank
        private String countryCode;

        @NotBlank
        private String regionCode;

        @NotBlank
        private String provinceCode;

        @NotBlank
        private String city;

        @NotBlank
        private String istatCityCode;

        private String postalCode;

        @NotBlank
        private String streetAddress;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Place {
        @NotBlank
        private String countryCode;

        @NotBlank
        private String regionCode;

        @NotBlank
        private String provinceCode;

        @NotBlank
        private String city;

        @NotBlank
        private String istatCityCode;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Contact {
        private String email;
        private String mobile;
    }
}