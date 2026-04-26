package it.thcs.fse.fsersaservice.application.mapper;

import it.thcs.fse.fsersaservice.api.dto.request.RsaDocumentIngestionRequest;
import it.thcs.fse.fsersaservice.domain.model.RsaDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RsaDocumentRequestMapper {

    public RsaDocument toDomain(RsaDocumentIngestionRequest request) {
        RsaDocument document = new RsaDocument();
        document.setDocumentType(request.getDocumentType());
        document.setSourceSystem(request.getSourceSystem());
        document.setSourceRequestId(request.getSourceRequestId());

        document.setPatient(mapPerson(request.getPatient()));
        document.setAuthor(mapProfessional(request.getAuthor()));
        document.setDataEnterer(mapProfessional(request.getDataEnterer()));
        document.setLegalAuthenticator(mapProfessional(request.getLegalAuthenticator()));
        document.setParticipantRef(mapParticipantRef(request.getParticipantRef()));

        document.setCustodian(mapCustodian(request.getCustodian()));
        document.setOrder(mapOrder(request.getOrder()));
        document.setServiceEvent(mapServiceEvent(request.getServiceEvent()));
        document.setEncounter(mapEncounter(request.getEncounter()));
        document.setClinicalContent(mapClinicalContent(request.getClinicalContent()));

        return document;
    }

    private RsaDocument.Person mapPerson(RsaDocumentIngestionRequest.PersonDto source) {
        if (source == null) return null;
        RsaDocument.Person target = new RsaDocument.Person();
        target.setFiscalCode(source.getFiscalCode());
        target.setFirstName(source.getFirstName());
        target.setLastName(source.getLastName());
        target.setGenderCode(source.getGenderCode());
        target.setBirthDate(source.getBirthDate());
        target.setBirthPlace(mapPlace(source.getBirthPlace()));
        target.setAddress(mapAddress(source.getAddress()));
        target.setContact(mapContact(source.getContact()));
        return target;
    }

    private RsaDocument.Professional mapProfessional(RsaDocumentIngestionRequest.ProfessionalDto source) {
        if (source == null) return null;
        RsaDocument.Professional target = new RsaDocument.Professional();
        target.setFiscalCode(source.getFiscalCode());
        target.setFirstName(source.getFirstName());
        target.setLastName(source.getLastName());
        target.setPrefix(source.getPrefix());
        target.setAddress(mapAddress(source.getAddress()));
        target.setContact(mapContact(source.getContact()));
        return target;
    }

    private RsaDocument.ParticipantRef mapParticipantRef(RsaDocumentIngestionRequest.ParticipantRefDto source) {
        if (source == null) return null;
        RsaDocument.ParticipantRef target = new RsaDocument.ParticipantRef();
        target.setFiscalCode(source.getFiscalCode());
        target.setFirstName(source.getFirstName());
        target.setLastName(source.getLastName());
        target.setPrefix(source.getPrefix());
        target.setEventTime(source.getEventTime());
        target.setOccupationCoding(mapCoding(source.getOccupationCoding()));
        target.setAddress(mapAddress(source.getAddress()));
        target.setContact(mapContact(source.getContact()));
        return target;
    }

    private RsaDocument.Custodian mapCustodian(RsaDocumentIngestionRequest.CustodianDto source) {
        if (source == null) return null;
        RsaDocument.Custodian target = new RsaDocument.Custodian();
        target.setCode(source.getCode());
        target.setName(source.getName());
        target.setPhone(source.getPhone());
        target.setAddress(mapAddress(source.getAddress()));
        return target;
    }

    private RsaDocument.Order mapOrder(RsaDocumentIngestionRequest.OrderDto source) {
        if (source == null) return null;
        RsaDocument.Order target = new RsaDocument.Order();
        target.setNre(source.getNre());
        target.setPriorityCode(source.getPriorityCode());
        return target;
    }

    private RsaDocument.ServiceEvent mapServiceEvent(RsaDocumentIngestionRequest.ServiceEventDto source) {
        if (source == null) return null;
        RsaDocument.ServiceEvent target = new RsaDocument.ServiceEvent();
        target.setEventCode(source.getEventCode());
        target.setEffectiveTime(source.getEffectiveTime());
        return target;
    }

    private RsaDocument.Encounter mapEncounter(RsaDocumentIngestionRequest.EncounterDto source) {
        if (source == null) return null;
        RsaDocument.Encounter target = new RsaDocument.Encounter();
        target.setEncounterId(source.getEncounterId());
        target.setStartDateTime(source.getStartDateTime());
        target.setEndDateTime(source.getEndDateTime());
        target.setFacilityCode(source.getFacilityCode());
        target.setFacilityName(source.getFacilityName());
        target.setFacilityAddress(mapAddress(source.getFacilityAddress()));
        return target;
    }

    private RsaDocument.Address mapAddress(RsaDocumentIngestionRequest.AddressDto source) {
        if (source == null) return null;
        RsaDocument.Address target = new RsaDocument.Address();
        target.setCountryCode(source.getCountryCode());
        target.setRegionCode(source.getRegionCode());
        target.setProvinceCode(source.getProvinceCode());
        target.setCity(source.getCity());
        target.setIstatCityCode(source.getIstatCityCode());
        target.setPostalCode(source.getPostalCode());
        target.setStreetAddress(source.getStreetAddress());
        return target;
    }

    private RsaDocument.Place mapPlace(RsaDocumentIngestionRequest.PlaceDto source) {
        if (source == null) return null;
        RsaDocument.Place target = new RsaDocument.Place();
        target.setCountryCode(source.getCountryCode());
        target.setRegionCode(source.getRegionCode());
        target.setProvinceCode(source.getProvinceCode());
        target.setCity(source.getCity());
        target.setIstatCityCode(source.getIstatCityCode());
        return target;
    }

    private RsaDocument.Contact mapContact(RsaDocumentIngestionRequest.ContactDto source) {
        if (source == null) return null;
        RsaDocument.Contact target = new RsaDocument.Contact();
        target.setEmail(source.getEmail());
        target.setMobile(source.getMobile());
        return target;
    }

    private RsaDocument.ClinicalContent mapClinicalContent(RsaDocumentIngestionRequest.ClinicalContentDto source) {
        if (source == null) return null;

        RsaDocument.ClinicalContent target = new RsaDocument.ClinicalContent();
        target.setQuesitoDiagnostico(mapCodedText(source.getQuesitoDiagnostico()));
        target.setStoriaClinica(source.getStoriaClinica());
        target.setStoriaClinicaDettaglio(mapStoriaClinica(source.getStoriaClinicaDettaglio()));
        target.setPrecedentiEsamiEseguiti(mapPrecedentiEsami(source.getPrecedentiEsamiEseguiti()));
        target.setEsameObiettivo(source.getEsameObiettivo());
        target.setPrestazioni(mapPrestazioni(source.getPrestazioni()));
        target.setConfrontoPrecedentiEsami(source.getConfrontoPrecedentiEsami());
        target.setRefertoText(source.getRefertoText());
        target.setDiagnosi(mapCodedTexts(source.getDiagnosi()));
        target.setConclusioni(source.getConclusioni());
        target.setSuggerimentiMedicoPrescrittore(source.getSuggerimentiMedicoPrescrittore());
        target.setAccertamentiControlliConsigliati(mapAccertamenti(source.getAccertamentiControlliConsigliati()));
        target.setTerapiaFarmacologicaConsigliata(mapTerapie(source.getTerapiaFarmacologicaConsigliata()));
        return target;
    }

    private RsaDocument.StoriaClinica mapStoriaClinica(RsaDocumentIngestionRequest.StoriaClinicaDto source) {
        if (source == null) return null;
        RsaDocument.StoriaClinica target = new RsaDocument.StoriaClinica();
        target.setAnamnesiPatologicaRemotaText(source.getAnamnesiPatologicaRemotaText());
        target.setAnamnesiPatologicaProssimaText(source.getAnamnesiPatologicaProssimaText());
        target.setAnamnesiPatologicaFisiologicaText(source.getAnamnesiPatologicaFisiologicaText());
        target.setProblemCoding(mapCoding(source.getProblemCoding()));
        target.setProblemStartDateTime(source.getProblemStartDateTime());
        target.setProblemEndDateTime(source.getProblemEndDateTime());
        target.setDecorsoClinicoCoding(mapCoding(source.getDecorsoClinicoCoding()));
        target.setDecorsoClinicoReferenceText(source.getDecorsoClinicoReferenceText());
        target.setStatoClinicoCoding(mapCoding(source.getStatoClinicoCoding()));
        target.setStatoClinicoReferenceText(source.getStatoClinicoReferenceText());
        target.setFamilyHistories(mapFamilyHistories(source.getFamilyHistories()));
        target.setAllergies(mapAllergies(source.getAllergies()));
        target.setTerapiaFarmacologicaInAtto(mapTerapie(source.getTerapiaFarmacologicaInAtto()));
        return target;
    }

    private RsaDocument.CodedText mapCodedText(RsaDocumentIngestionRequest.CodedTextDto source) {
        if (source == null) return null;
        RsaDocument.CodedText target = new RsaDocument.CodedText();
        target.setText(source.getText());
        target.setCoding(mapCoding(source.getCoding()));
        return target;
    }

    private List<RsaDocument.CodedText> mapCodedTexts(List<RsaDocumentIngestionRequest.CodedTextDto> source) {
        List<RsaDocument.CodedText> result = new ArrayList<>();
        if (source == null) return result;
        for (RsaDocumentIngestionRequest.CodedTextDto item : source) {
            result.add(mapCodedText(item));
        }
        return result;
    }

    private RsaDocument.Coding mapCoding(RsaDocumentIngestionRequest.CodingDto source) {
        if (source == null) return null;
        RsaDocument.Coding target = new RsaDocument.Coding();
        target.setCode(source.getCode());
        target.setCodeSystem(source.getCodeSystem());
        target.setCodeSystemName(source.getCodeSystemName());
        target.setDisplayName(source.getDisplayName());
        return target;
    }

    private List<RsaDocument.PrecedenteEsame> mapPrecedentiEsami(List<RsaDocumentIngestionRequest.PrecedenteEsameDto> source) {
        List<RsaDocument.PrecedenteEsame> result = new ArrayList<>();
        if (source == null) return result;

        for (RsaDocumentIngestionRequest.PrecedenteEsameDto item : source) {
            RsaDocument.PrecedenteEsame target = new RsaDocument.PrecedenteEsame();
            target.setDescription(item.getDescription());
            target.setExecutionStartDateTime(item.getExecutionStartDateTime());
            target.setExecutionEndDateTime(item.getExecutionEndDateTime());
            target.setOutcomeText(item.getOutcomeText());
            target.setCoding(mapCoding(item.getCoding()));
            result.add(target);
        }
        return result;
    }

    private List<RsaDocument.Prestazione> mapPrestazioni(List<RsaDocumentIngestionRequest.PrestazioneDto> source) {
        List<RsaDocument.Prestazione> result = new ArrayList<>();
        if (source == null) return result;

        for (RsaDocumentIngestionRequest.PrestazioneDto item : source) {
            RsaDocument.Prestazione target = new RsaDocument.Prestazione();
            target.setText(item.getText());
            target.setCoding(mapCoding(item.getCoding()));
            target.setPerformedAt(item.getPerformedAt());
            target.setProcedureOperative(mapProcedureOperative(item.getProcedureOperative()));
            result.add(target);
        }
        return result;
    }

    private List<RsaDocument.ProceduraOperativa> mapProcedureOperative(List<RsaDocumentIngestionRequest.ProceduraOperativaDto> source) {
        List<RsaDocument.ProceduraOperativa> result = new ArrayList<>();
        if (source == null) return result;

        for (RsaDocumentIngestionRequest.ProceduraOperativaDto item : source) {
            RsaDocument.ProceduraOperativa target = new RsaDocument.ProceduraOperativa();
            target.setText(item.getText());
            target.setCoding(mapCoding(item.getCoding()));
            target.setPerformedAt(item.getPerformedAt());
            target.setQuantityText(item.getQuantityText());
            target.setExecutionMode(item.getExecutionMode());
            target.setInstrumentation(item.getInstrumentation());
            target.setDescriptiveParameters(item.getDescriptiveParameters());
            target.setNotes(item.getNotes());
            result.add(target);
        }
        return result;
    }

    private List<RsaDocument.FamilyHistory> mapFamilyHistories(List<RsaDocumentIngestionRequest.FamilyHistoryDto> source) {
        List<RsaDocument.FamilyHistory> result = new ArrayList<>();
        if (source == null) return result;

        for (RsaDocumentIngestionRequest.FamilyHistoryDto item : source) {
            RsaDocument.FamilyHistory target = new RsaDocument.FamilyHistory();
            target.setRelationshipCode(item.getRelationshipCode());
            target.setRelationshipCodeSystem(item.getRelationshipCodeSystem());
            target.setRelationshipCodeSystemName(item.getRelationshipCodeSystemName());
            target.setRelationshipDisplayName(item.getRelationshipDisplayName());
            target.setSubjectGenderCode(item.getSubjectGenderCode());
            target.setDiagnosisCoding(mapCoding(item.getDiagnosisCoding()));
            target.setDiagnosisReferenceText(item.getDiagnosisReferenceText());
            target.setAgeAtDiagnosisYears(item.getAgeAtDiagnosisYears());
            target.setAgeAtDeathYears(item.getAgeAtDeathYears());
            target.setEffectiveTime(item.getEffectiveTime());
            result.add(target);
        }
        return result;
    }

    private List<RsaDocument.Allergy> mapAllergies(List<RsaDocumentIngestionRequest.AllergyDto> source) {
        List<RsaDocument.Allergy> result = new ArrayList<>();
        if (source == null) return result;

        for (RsaDocumentIngestionRequest.AllergyDto item : source) {
            RsaDocument.Allergy target = new RsaDocument.Allergy();
            target.setNarrativeText(item.getNarrativeText());
            target.setStartDateTime(item.getStartDateTime());
            target.setEndDateTime(item.getEndDateTime());
            target.setAgentCoding(mapCoding(item.getAgentCoding()));
            target.setReactionCoding(mapCoding(item.getReactionCoding()));
            target.setCriticalityCoding(mapCoding(item.getCriticalityCoding()));
            target.setStatusCoding(mapCoding(item.getStatusCoding()));
            target.setComment(item.getComment());
            result.add(target);
        }
        return result;
    }

    private List<RsaDocument.AccertamentoControllo> mapAccertamenti(List<RsaDocumentIngestionRequest.AccertamentoControlloDto> source) {
        List<RsaDocument.AccertamentoControllo> result = new ArrayList<>();
        if (source == null) return result;

        for (RsaDocumentIngestionRequest.AccertamentoControlloDto item : source) {
            RsaDocument.AccertamentoControllo target = new RsaDocument.AccertamentoControllo();
            target.setText(item.getText());
            target.setCoding(mapCoding(item.getCoding()));
            result.add(target);
        }
        return result;
    }

    private List<RsaDocument.TerapiaFarmacologica> mapTerapie(List<RsaDocumentIngestionRequest.TerapiaFarmacologicaDto> source) {
        List<RsaDocument.TerapiaFarmacologica> result = new ArrayList<>();
        if (source == null) return result;

        for (RsaDocumentIngestionRequest.TerapiaFarmacologicaDto item : source) {
            RsaDocument.TerapiaFarmacologica target = new RsaDocument.TerapiaFarmacologica();
            target.setText(item.getText());
            target.setAicCode(item.getAicCode());
            target.setAicDisplayName(item.getAicDisplayName());
            target.setAtcCode(item.getAtcCode());
            target.setAtcDisplayName(item.getAtcDisplayName());
            target.setStartDateTime(item.getStartDateTime());
            target.setEndDateTime(item.getEndDateTime());
            target.setFrequencyHours(item.getFrequencyHours());
            target.setRouteCode(item.getRouteCode());
            target.setRouteDisplayName(item.getRouteDisplayName());
            target.setDoseLowValue(item.getDoseLowValue());
            target.setDoseHighValue(item.getDoseHighValue());
            target.setDoseUnit(item.getDoseUnit());
            target.setRateLowValue(item.getRateLowValue());
            target.setRateHighValue(item.getRateHighValue());
            target.setRateUnit(item.getRateUnit());
            target.setAdministrationUnitCoding(mapCoding(item.getAdministrationUnitCoding()));
            target.setGrammaturaValue(item.getGrammaturaValue());
            target.setPackageQuantityValue(item.getPackageQuantityValue());
            target.setQuantity(item.getQuantity());
            target.setParticipantRef(mapParticipantRef(item.getParticipantRef()));
            result.add(target);
        }

        return result;
    }
}