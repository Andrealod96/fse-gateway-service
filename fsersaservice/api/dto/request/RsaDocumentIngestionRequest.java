package it.thcs.fse.fsersaservice.api.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    private PersonDto patient;
    private ProfessionalDto author;
    private ProfessionalDto dataEnterer;
    private ProfessionalDto legalAuthenticator;
    private ParticipantRefDto participantRef;

    private CustodianDto custodian;
    private OrderDto order;
    private ServiceEventDto serviceEvent;
    private EncounterDto encounter;
    private ClinicalContentDto clinicalContent;

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
}