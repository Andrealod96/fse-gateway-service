package it.thcs.fse.fsersaservice.application.mapper;

import it.thcs.fse.fsersaservice.api.dto.request.RsaDocumentIngestionRequest;
import it.thcs.fse.fsersaservice.domain.model.RsaDocument;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RsaDocumentRequestMapper {

    public RsaDocument toDomain(RsaDocumentIngestionRequest request) {
        return RsaDocument.builder()
                .documentType(mapDocumentType(request.getDocumentType()))
                .sourceSystem(request.getSourceSystem())
                .sourceRequestId(request.getSourceRequestId())
                .patient(mapPatient(request.getPatient()))
                .author(mapPractitioner(request.getAuthor()))
                .dataEnterer(request.getDataEnterer() != null ? mapPractitioner(request.getDataEnterer()) : null)
                .legalAuthenticator(mapPractitioner(request.getLegalAuthenticator()))
                .custodian(mapCustodian(request.getCustodian()))
                .order(mapOrder(request.getOrder()))
                .serviceEvent(mapServiceEvent(request.getServiceEvent()))
                .encounter(mapEncounter(request.getEncounter()))
                .clinicalContent(mapClinicalContent(request.getClinicalContent()))
                .build();
    }

    private RsaDocument.DocumentType mapDocumentType(RsaDocumentIngestionRequest.DocumentType documentType) {
        return switch (documentType) {
            case RSA -> RsaDocument.DocumentType.RSA;
        };
    }

    private RsaDocument.Patient mapPatient(RsaDocumentIngestionRequest.Patient patient) {
        return RsaDocument.Patient.builder()
                .fiscalCode(patient.getFiscalCode())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .genderCode(patient.getGenderCode())
                .birthDate(patient.getBirthDate())
                .birthPlace(mapPlace(patient.getBirthPlace()))
                .address(mapAddress(patient.getAddress()))
                .contact(patient.getContact() != null ? mapContact(patient.getContact()) : null)
                .build();
    }

    private RsaDocument.Practitioner mapPractitioner(RsaDocumentIngestionRequest.Practitioner practitioner) {
        return RsaDocument.Practitioner.builder()
                .fiscalCode(practitioner.getFiscalCode())
                .firstName(practitioner.getFirstName())
                .lastName(practitioner.getLastName())
                .prefix(practitioner.getPrefix())
                .address(mapAddress(practitioner.getAddress()))
                .contact(practitioner.getContact() != null ? mapContact(practitioner.getContact()) : null)
                .build();
    }

    private RsaDocument.Custodian mapCustodian(RsaDocumentIngestionRequest.Custodian custodian) {
        return RsaDocument.Custodian.builder()
                .code(custodian.getCode())
                .name(custodian.getName())
                .phone(custodian.getPhone())
                .address(mapAddress(custodian.getAddress()))
                .build();
    }

    private RsaDocument.Order mapOrder(RsaDocumentIngestionRequest.Order order) {
        return RsaDocument.Order.builder()
                .nre(order.getNre())
                .priorityCode(order.getPriorityCode())
                .build();
    }

    private RsaDocument.ServiceEvent mapServiceEvent(RsaDocumentIngestionRequest.ServiceEvent serviceEvent) {
        return RsaDocument.ServiceEvent.builder()
                .eventCode(serviceEvent.getEventCode())
                .effectiveTime(serviceEvent.getEffectiveTime())
                .build();
    }

    private RsaDocument.Encounter mapEncounter(RsaDocumentIngestionRequest.Encounter encounter) {
        return RsaDocument.Encounter.builder()
                .encounterId(encounter.getEncounterId())
                .startDateTime(encounter.getStartDateTime())
                .endDateTime(encounter.getEndDateTime())
                .facilityCode(encounter.getFacilityCode())
                .facilityName(encounter.getFacilityName())
                .facilityAddress(mapAddress(encounter.getFacilityAddress()))
                .build();
    }

    private RsaDocument.ClinicalContent mapClinicalContent(RsaDocumentIngestionRequest.ClinicalContent clinicalContent) {
        return RsaDocument.ClinicalContent.builder()
                .quesitoDiagnostico(mapQuesitoDiagnostico(clinicalContent.getQuesitoDiagnostico()))
                .prestazioni(clinicalContent.getPrestazioni().stream().map(this::mapPrestazione).toList())
                .storiaClinica(clinicalContent.getStoriaClinica())
                .confrontoPrecedentiEsami(clinicalContent.getConfrontoPrecedentiEsami())
                .refertoText(clinicalContent.getRefertoText())
                .diagnosi(clinicalContent.getDiagnosi().stream().map(this::mapDiagnosis).toList())
                .conclusioni(clinicalContent.getConclusioni())
                .suggerimentiMedicoPrescrittore(clinicalContent.getSuggerimentiMedicoPrescrittore())
                .accertamentiControlliConsigliati(
                        clinicalContent.getAccertamentiControlliConsigliati() != null
                                ? List.copyOf(clinicalContent.getAccertamentiControlliConsigliati())
                                : List.of()
                )
                .build();
    }

    private RsaDocument.QuesitoDiagnostico mapQuesitoDiagnostico(RsaDocumentIngestionRequest.QuesitoDiagnostico quesitoDiagnostico) {
        return RsaDocument.QuesitoDiagnostico.builder()
                .text(quesitoDiagnostico.getText())
                .coding(mapCoding(quesitoDiagnostico.getCoding()))
                .build();
    }

    private RsaDocument.Prestazione mapPrestazione(RsaDocumentIngestionRequest.Prestazione prestazione) {
        return RsaDocument.Prestazione.builder()
                .text(prestazione.getText())
                .coding(mapCoding(prestazione.getCoding()))
                .performedAt(prestazione.getPerformedAt())
                .build();
    }

    private RsaDocument.Diagnosis mapDiagnosis(RsaDocumentIngestionRequest.Diagnosis diagnosis) {
        return RsaDocument.Diagnosis.builder()
                .text(diagnosis.getText())
                .coding(mapCoding(diagnosis.getCoding()))
                .build();
    }

    private RsaDocument.Coding mapCoding(RsaDocumentIngestionRequest.Coding coding) {
        return RsaDocument.Coding.builder()
                .code(coding.getCode())
                .codeSystem(coding.getCodeSystem())
                .codeSystemName(coding.getCodeSystemName())
                .displayName(coding.getDisplayName())
                .build();
    }

    private RsaDocument.Address mapAddress(RsaDocumentIngestionRequest.Address address) {
        return RsaDocument.Address.builder()
                .countryCode(address.getCountryCode())
                .regionCode(address.getRegionCode())
                .provinceCode(address.getProvinceCode())
                .city(address.getCity())
                .istatCityCode(address.getIstatCityCode())
                .postalCode(address.getPostalCode())
                .streetAddress(address.getStreetAddress())
                .build();
    }

    private RsaDocument.Place mapPlace(RsaDocumentIngestionRequest.Place place) {
        return RsaDocument.Place.builder()
                .countryCode(place.getCountryCode())
                .regionCode(place.getRegionCode())
                .provinceCode(place.getProvinceCode())
                .city(place.getCity())
                .istatCityCode(place.getIstatCityCode())
                .build();
    }

    private RsaDocument.Contact mapContact(RsaDocumentIngestionRequest.Contact contact) {
        return RsaDocument.Contact.builder()
                .email(contact.getEmail())
                .mobile(contact.getMobile())
                .build();
    }
}