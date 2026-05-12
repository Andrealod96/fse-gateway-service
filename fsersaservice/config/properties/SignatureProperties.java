package it.thcs.fse.fsersaservice.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "fse.signature")
public class SignatureProperties {

    private boolean enabled;

    @NotBlank
    private String keyStorePath;

    @NotBlank
    private String keyStoreType;

    @NotBlank
    private String keyStorePassword;

    /**
     * Facoltativa.
     * Se assente, si usa la stessa password del keystore.
     */
    private String privateKeyPassword;

    @NotBlank
    private String keyAlias;

    /**
     * Cartella output PDF firmati.
     */
    @NotBlank
    private String signedPdfDir;

    /**
     * Metadati firma PAdES baseline B.
     */
    private String reason;
    private String location;
    private String contactInfo;
}