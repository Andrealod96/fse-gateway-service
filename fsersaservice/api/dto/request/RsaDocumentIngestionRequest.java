package it.thcs.fse.fsersaservice.api.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RsaDocumentIngestionRequest {

    private String documentType;
    private String sourceSystem;
    private String sourceRequestId;

    @NotNull
    private PersonDto patient;

    @NotNull
    private ProfessionalDto author;

    private ProfessionalDto dataEnterer;

    @NotNull
    private ProfessionalDto legalAuthenticator;

    private ParticipantRefDto participantRef;

    @NotNull
    private CustodianDto custodian;

    private OrderDto order;
    private ServiceEventDto serviceEvent;

    @NotNull
    private EncounterDto encounter;

    @NotNull
    private ClinicalContentDto clinicalContent;

    /**
     * Metadati XDS/INI obbligatori per il flusso validate-and-create.
     * Se presente, il controller instraderà la richiesta verso
     * POST /v1/documents/validate-and-create invece di /v1/documents/validation.
     * Se assente, viene eseguita solo la validazione.
     */
    @Valid
    private PubblicazioneDto pubblicazione;

    // ─── Pubblicazione / XDS metadata ─────────────────────────────────────────

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PubblicazioneDto {

        /**
         * XDSDocumentEntry.healthcareFacilityTypeCode
         * Es: "Ospedale"
         */
        @NotBlank
        private String tipologiaStruttura;

        /**
         * XDSDocumentEntry.uniqueId — identificativo univoco del documento.
         * Formato OID: 2.16.840.1.113883.2.9.2.[REGIONE].4.4.[HASH]
         */
        @NotBlank
        private String identificativoDoc;

        /**
         * XDSDocumentEntry.repositoryUniqueId
         * Es: "2.16.840.1.113883.2.9.2.160.4.5.1"
         */
        @NotBlank
        private String identificativoRep;

        /**
         * XDSDocumentEntry.classCode
         * Es: "REF" (Referto)
         */
        @NotBlank
        private String tipoDocumentoLivAlto;

        /**
         * XDSDocumentEntry.practiceSettingCode
         * Es: "AD_PSC131" (Medicina Generale)
         */
        @NotBlank
        private String assettoOrganizzativo;

        /**
         * XDSSubmissionSet.contentTypeCode
         * Es: "PHR"
         */
        @NotBlank
        private String tipoAttivitaClinica;

        /**
         * XDSSubmissionSet.uniqueId
         * Formato: 2.16.840.1.113883.2.9.2.[REGIONE].4.3.[PROGRESSIVO]
         */
        @NotBlank
        private String identificativoSottomissione;

        // Campi opzionali
        private List<String> attiCliniciRegoleAccesso = new ArrayList<>();
        private String dataInizioPrestazione;
        private String dataFinePrestazione;
        private String conservazioneANorma;
        private List<String> descriptions = new ArrayList<>();
        private List<String> administrativeRequest = new ArrayList<>();
    }

    // ─── Persone ──────────────────────────────────────────────────────────────

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PersonDto {
        private String fiscalCode;
        private String firstName;
        private String lastName;
        private String genderCode;
        private String birthDate;
        private PlaceDto birthPlace;
        private AddressDto address;
        private ContactDto contact;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProfessionalDto {
        private String fiscalCode;
        private String firstName;
        private String lastName;
        private String prefix;
        private AddressDto address;
        private ContactDto contact;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParticipantRefDto {
        private String fiscalCode;
        private String firstName;
        private String lastName;
        private String prefix;
        private String eventTime;
        private CodingDto occupationCoding;
        private AddressDto address;
        private ContactDto contact;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustodianDto {
        private String code;
        private String name;
        private String phone;
        private AddressDto address;
    }

    // ─── Encounter / Order / ServiceEvent ─────────────────────────────────────

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EncounterDto {
        private String encounterId;
        private String startDateTime;
        private String endDateTime;
        private String facilityCode;
        private String facilityName;
        private AddressDto facilityAddress;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderDto {
        private String nre;
        private String priorityCode;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ServiceEventDto {
        private String eventCode;
        private String effectiveTime;
    }

    // ─── Contenuto clinico ────────────────────────────────────────────────────

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClinicalContentDto {
        private CodedTextDto quesitoDiagnostico;
        private String storiaClinica;
        private StoriaClinicaDto storiaClinicaDettaglio;
        private List<PrecedenteEsameDto> precedentiEsamiEseguiti = new ArrayList<>();
        private String esameObiettivo;
        private List<PrestazioneDto> prestazioni = new ArrayList<>();
        private String confrontoPrecedentiEsami;
        private String refertoText;
        private List<CodedTextDto> diagnosi = new ArrayList<>();
        private String conclusioni;
        private String suggerimentiMedicoPrescrittore;
        private List<AccertamentoControlloDto> accertamentiControlliConsigliati = new ArrayList<>();
        private List<TerapiaFarmacologicaDto> terapiaFarmacologicaConsigliata = new ArrayList<>();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CodedTextDto {
        private String text;
        private CodingDto coding;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CodingDto {
        private String code;
        private String codeSystem;
        private String codeSystemName;
        private String displayName;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StoriaClinicaDto {
        private String anamnesiPatologicaRemotaText;
        private String anamnesiPatologicaProssimaText;
        private String anamnesiPatologicaFisiologicaText;
        private CodingDto problemCoding;
        private String problemStartDateTime;
        private String problemEndDateTime;
        private CodingDto decorsoClinicoCoding;
        private String decorsoClinicoReferenceText;
        private CodingDto statoClinicoCoding;
        private String statoClinicoReferenceText;
        private List<FamilyHistoryDto> familyHistories = new ArrayList<>();
        private List<AllergyDto> allergies = new ArrayList<>();
        private List<TerapiaFarmacologicaDto> terapiaFarmacologicaInAtto = new ArrayList<>();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PrecedenteEsameDto {
        private String description;
        private String executionStartDateTime;
        private String executionEndDateTime;
        private String outcomeText;
        private CodingDto coding;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PrestazioneDto {
        private String text;
        private CodingDto coding;
        private String performedAt;
        private List<ProceduraOperativaDto> procedureOperative = new ArrayList<>();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProceduraOperativaDto {
        private String text;
        private CodingDto coding;
        private String performedAt;
        private String quantityText;
        private String executionMode;
        private String instrumentation;
        private String descriptiveParameters;
        private String notes;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AccertamentoControlloDto {
        private String text;
        private CodingDto coding;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FamilyHistoryDto {
        private String relationshipCode;
        private String relationshipCodeSystem;
        private String relationshipCodeSystemName;
        private String relationshipDisplayName;
        private String subjectGenderCode;
        private CodingDto diagnosisCoding;
        private String diagnosisReferenceText;
        private Integer ageAtDiagnosisYears;
        private Integer ageAtDeathYears;
        private String effectiveTime;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AllergyDto {
        private String narrativeText;
        private String startDateTime;
        private String endDateTime;
        private CodingDto agentCoding;
        private CodingDto reactionCoding;
        private CodingDto criticalityCoding;
        private CodingDto statusCoding;
        private String comment;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TerapiaFarmacologicaDto {
        private String text;
        private String aicCode;
        private String aicDisplayName;
        private String atcCode;
        private String atcDisplayName;
        private String startDateTime;
        private String endDateTime;
        private Integer frequencyHours;
        private String routeCode;
        private String routeDisplayName;
        private String doseLowValue;
        private String doseHighValue;
        private String doseUnit;
        private String rateLowValue;
        private String rateHighValue;
        private String rateUnit;
        private CodingDto administrationUnitCoding;
        private String grammaturaValue;
        private String packageQuantityValue;
        private Integer quantity;
        private ParticipantRefDto participantRef;
    }

    // ─── Indirizzo / Luogo / Contatto ─────────────────────────────────────────

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AddressDto {
        private String countryCode;
        private String regionCode;
        private String provinceCode;
        private String city;
        private String istatCityCode;
        private String postalCode;
        private String streetAddress;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlaceDto {
        private String countryCode;
        private String regionCode;
        private String provinceCode;
        private String city;
        private String istatCityCode;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContactDto {
        private String email;
        private String mobile;
    }
}