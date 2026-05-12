package it.thcs.fse.fsersaservice.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "fse.cda")
public class CdaProperties {

    @NotBlank
    private String templateIdRoot;

    @NotBlank
    private String templateIdExtension;

    @NotBlank
    private String codeSystemLoinc;

    @NotBlank
    private String codeSystemIcd9cm;

    @NotBlank
    private String confidentialityCodeSystem;

    @NotBlank
    private String typeIdRoot;

    @NotBlank
    private String typeIdExtension;

    @NotBlank
    private String documentCodeRsa;

    @NotBlank
    private String documentTitle;

    @NotBlank
    private String languageCode;

    @NotBlank
    private String realmCode;
}