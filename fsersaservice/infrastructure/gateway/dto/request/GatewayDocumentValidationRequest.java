package it.thcs.fse.fsersaservice.infrastructure.gateway.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.MediaType;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayDocumentValidationRequest {

    @Valid
    @NotNull
    private GatewayRequestBodyDto requestBody;

    @NotNull
    @Size(min = 1)
    private byte[] fileBytes;

    @NotBlank
    private String fileName;

    @Builder.Default
    @NotBlank
    private String fileContentType = MediaType.APPLICATION_PDF_VALUE;

    /**
     * CF del professionista che invoca il Gateway.
     * Il generator lo convertirà nel formato CX richiesto.
     */
    @NotBlank
    private String practitionerFiscalCode;

    /**
     * CF del paziente / assistito.
     * Il generator lo convertirà nel formato CX richiesto.
     */
    @NotBlank
    private String patientFiscalCode;

    /**
     * Es: RSA, AAS, DRS... dipende dal profilo che userai.
     */
    @NotBlank
    private String subjectRole;

    /**
     * Per validazione e creazione il valore documentato è CREATE.
     */
    @Builder.Default
    @NotBlank
    private String actionId = "CREATE";

    /**
     * Es: ('11488-4^^2.16.840.1.113883.6.1')
     */
    @NotBlank
    private String resourceHl7Type;

    @Builder.Default
    @NotNull
    private Boolean patientConsent = Boolean.TRUE;

    /**
     * Per /documents/validation normalmente false.
     * Ti serve quando passerai a pubblicazione/replace.
     */
    @Builder.Default
    private boolean includeAttachmentHash = false;

    /**
     * Override opzionali per claim normalmente presi da JwtProperties.
     * Se null, il generator usa la configurazione.
     */
    private String purposeOfUse;
    private String locality;
    private String subjectOrganization;
    private String subjectOrganizationId;
    private String subjectApplicationId;
    private String subjectApplicationVendor;
    private String subjectApplicationVersion;
    private Boolean useSubjectAsAuthor;
}