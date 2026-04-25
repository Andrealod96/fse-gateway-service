package it.thcs.fse.fsersaservice.infrastructure.gateway.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GatewayRequestBodyDto {

    /**
     * Obbligatorio solo nei flussi di pubblicazione, viene restituito dalla validazione.
     */
    private String workflowInstanceId;

    /**
     * Es: CDA
     */
    private String healthDataFormat;

    /**
     * Es: ATTACHMENT, RESOURCE
     */
    private String mode;

    /**
     * Es: VALIDATION, VERIFICA
     */
    private String activity;

    /**
     * Metadati XDS/INI: non servono alla validazione pura /documents/validation,
     * ma servono nei flussi /documents e validate-and-create / validate-and-replace.
     */
    private String tipologiaStruttura;
    private List<String> attiCliniciRegoleAccesso;
    private String identificativoDoc;
    private String identificativoRep;
    private String tipoDocumentoLivAlto;
    private String assettoOrganizzativo;
    private String dataInizioPrestazione;
    private String dataFinePrestazione;
    private String conservazioneANorma;
    private String tipoAttivitaClinica;
    private String identificativoSottomissione;
    private List<String> descriptions;
    private List<String> administrativeRequest;
}