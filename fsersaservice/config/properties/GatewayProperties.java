package it.thcs.fse.fsersaservice.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "fse.gateway")
public class GatewayProperties {

    @NotBlank
    private String baseUrl;

    @NotBlank
    private String validationPath;

    @NotBlank
    private String validateAndCreatePath;

    @NotBlank
    private String statusByWorkflowPath;

    @NotBlank
    private String statusByTracePath;

    @Min(1)
    private int connectTimeoutMillis;

    @Min(1)
    private int readTimeoutSeconds;

    @Min(1)
    private int writeTimeoutSeconds;

    @Min(1)
    private int responseTimeoutSeconds;
}