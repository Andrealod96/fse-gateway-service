package it.thcs.fse.fsersaservice.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "fse.validation")
public class ValidationProperties {

    @Valid
    private Xsd xsd = new Xsd();

    @Valid
    private Schematron schematron = new Schematron();

    @Valid
    private Terminology terminology = new Terminology();

    @Getter
    @Setter
    public static class Xsd {
        private boolean enabled;

        @NotBlank
        private String schemaPath;
    }

    @Getter
    @Setter
    public static class Schematron {
        private boolean enabled;

        @NotBlank
        private String rsaPath;
    }

    @Getter
    @Setter
    public static class Terminology {
        private boolean enabled;

        @NotBlank
        private String catalogsBasePath;
    }
}