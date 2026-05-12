package it.thcs.fse.fsersaservice.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "fse.storage")
public class StorageProperties {

    @NotBlank
    private String workingDir;

    @NotBlank
    private String inputDir;

    @NotBlank
    private String outputDir;

    @NotBlank
    private String cdaDir;

    @NotBlank
    private String pdfDir;

    @NotBlank
    private String signedPdfDir;

    @NotBlank
    private String accreditationDir;
}
