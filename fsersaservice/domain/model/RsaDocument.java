package it.thcs.fse.fsersaservice.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RsaDocument {

    private String documentType;
    private String sourceSystem;
    private String sourceRequestId;

    private Person patient;
    private Professional author;
    private Professional dataEnterer;
    private Professional legalAuthenticator;
    private ParticipantRef participantRef;

    private Custodian custodian;
    private Order order;
    private ServiceEvent serviceEvent;
    private Encounter encounter;
    private ClinicalContent clinicalContent;

    @Getter
    @Setter
    public static class Person {
        private String fiscalCode;
        private String firstName;
        private String lastName;
        private String genderCode;
        private String birthDate;
        private Place birthPlace;
        private Address address;
        private Contact contact;
    }

    @Getter
    @Setter
    public static class Professional {
        private String fiscalCode;
        private String firstName;
        private String lastName;
        private String prefix;
        private Address address;
        private Contact contact;
    }

    @Getter
    @Setter
    public static class ParticipantRef {
        private String fiscalCode;
        private String firstName;
        private String lastName;
        private String prefix;
        private String eventTime;
        private Coding occupationCoding;
        private Address address;
        private Contact contact;
    }

    @Getter
    @Setter
    public static class Custodian {
        private String code;
        private String name;
        private String phone;
        private Address address;
    }

    @Getter
    @Setter
    public static class Order {
        private String nre;
        private String priorityCode;
    }

    @Getter
    @Setter
    public static class ServiceEvent {
        private String eventCode;
        private String effectiveTime;
    }

    @Getter
    @Setter
    public static class Encounter {
        private String encounterId;
        private String startDateTime;
        private String endDateTime;
        private String facilityCode;
        private String facilityName;
        private Address facilityAddress;
    }

    @Getter
    @Setter
    public static class Address {
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
    public static class Place {
        private String countryCode;
        private String regionCode;
        private String provinceCode;
        private String city;
        private String istatCityCode;
    }

    @Getter
    @Setter
    public static class Contact {
        private String email;
        private String mobile;
    }

    @Getter
    @Setter
    public static class ClinicalContent {
        private CodedText quesitoDiagnostico;
        private String storiaClinica;
        private StoriaClinica storiaClinicaDettaglio;

        private List<PrecedenteEsame> precedentiEsamiEseguiti = new ArrayList<>();
        private String esameObiettivo;
        private List<Prestazione> prestazioni = new ArrayList<>();
        private String confrontoPrecedentiEsami;
        private String refertoText;
        private List<CodedText> diagnosi = new ArrayList<>();
        private String conclusioni;
        private String suggerimentiMedicoPrescrittore;
        private List<AccertamentoControllo> accertamentiControlliConsigliati = new ArrayList<>();
        private List<TerapiaFarmacologica> terapiaFarmacologicaConsigliata = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class CodedText {
        private String text;
        private Coding coding;
    }

    @Getter
    @Setter
    public static class Coding {
        private String code;
        private String codeSystem;
        private String codeSystemName;
        private String displayName;
    }

    @Getter
    @Setter
    public static class StoriaClinica {
        private String anamnesiPatologicaRemotaText;
        private String anamnesiPatologicaProssimaText;
        private String anamnesiPatologicaFisiologicaText;

        private Coding problemCoding;
        private String problemStartDateTime;
        private String problemEndDateTime;

        private Coding decorsoClinicoCoding;
        private String decorsoClinicoReferenceText;

        private Coding statoClinicoCoding;
        private String statoClinicoReferenceText;

        private List<FamilyHistory> familyHistories = new ArrayList<>();
        private List<Allergy> allergies = new ArrayList<>();
        private List<TerapiaFarmacologica> terapiaFarmacologicaInAtto = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class FamilyHistory {
        private String relationshipCode;
        private String relationshipCodeSystem;
        private String relationshipCodeSystemName;
        private String relationshipDisplayName;
        private String subjectGenderCode;
        private Coding diagnosisCoding;
        private String diagnosisReferenceText;
        private Integer ageAtDiagnosisYears;
        private Integer ageAtDeathYears;
        private String effectiveTime;
    }

    @Getter
    @Setter
    public static class Allergy {
        private String narrativeText;
        private String startDateTime;
        private String endDateTime;
        private Coding agentCoding;
        private Coding reactionCoding;
        private Coding criticalityCoding;
        private Coding statusCoding;
        private String comment;
    }

    @Getter
    @Setter
    public static class PrecedenteEsame {
        private String description;
        private String executionStartDateTime;
        private String executionEndDateTime;
        private String outcomeText;
        private Coding coding;
    }

    @Getter
    @Setter
    public static class Prestazione {
        private String text;
        private Coding coding;
        private String performedAt;
        private List<ProceduraOperativa> procedureOperative = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class ProceduraOperativa {
        private String text;
        private Coding coding;
        private String performedAt;
        private String quantityText;
        private String executionMode;
        private String instrumentation;
        private String descriptiveParameters;
        private String notes;
    }

    @Getter
    @Setter
    public static class AccertamentoControllo {
        private String text;
        private Coding coding;
    }

    @Getter
    @Setter
    public static class TerapiaFarmacologica {
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

        private Coding administrationUnitCoding;

        private String grammaturaValue;
        private String packageQuantityValue;
        private Integer quantity;

        private ParticipantRef participantRef;
    }
}