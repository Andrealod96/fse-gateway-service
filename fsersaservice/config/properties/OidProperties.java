package it.thcs.fse.fsersaservice.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "fse.oid")
public class OidProperties {

    @NotBlank
    private String hl7ItaliaRoot;

    @NotBlank
    private String codiceFiscaleRoot;

    @NotBlank
    private String pugliaRoot;

    @NotBlank
    private String pugliaCdaDocumentRoot;

    @NotBlank
    private String pugliaOperatorRoot;

    @NotBlank
    private String pugliaStructureRoot;

    @NotBlank
    private String pugliaPrescriptionRoot;
}