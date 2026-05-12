package it.thcs.fse.fsersaservice.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "fse.validation.terminology")
public class TerminologyValidationProperties {

    private boolean enabled;

    @NotBlank
    private String catalogsBasePath;
}