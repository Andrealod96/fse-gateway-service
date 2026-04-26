package it.thcs.fse.fsersaservice.config.properties;

import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "fse.security.mtls")
public class MtlsProperties {

    private boolean enabled;

    private String keyStorePath;
    private String keyStoreType;
    private String keyStorePassword;
    private String keyAlias;

    private String trustStorePath;
    private String trustStoreType;
    private String trustStorePassword;

    @AssertTrue(message = "fse.security.mtls.key-store-path è obbligatorio quando mTLS è abilitato")
    public boolean isKeyStorePathValid() {
        return !enabled || StringUtils.hasText(keyStorePath);
    }

    @AssertTrue(message = "fse.security.mtls.key-store-type è obbligatorio quando mTLS è abilitato")
    public boolean isKeyStoreTypeValid() {
        return !enabled || StringUtils.hasText(keyStoreType);
    }

    @AssertTrue(message = "fse.security.mtls.key-store-password è obbligatorio quando mTLS è abilitato")
    public boolean isKeyStorePasswordValid() {
        return !enabled || StringUtils.hasText(keyStorePassword);
    }

    @AssertTrue(message = "fse.security.mtls.key-alias è obbligatorio quando mTLS è abilitato")
    public boolean isKeyAliasValid() {
        return !enabled || StringUtils.hasText(keyAlias);
    }

    @AssertTrue(message = "fse.security.mtls.trust-store-path è obbligatorio quando mTLS è abilitato")
    public boolean isTrustStorePathValid() {
        return !enabled || StringUtils.hasText(trustStorePath);
    }

    @AssertTrue(message = "fse.security.mtls.trust-store-type è obbligatorio quando mTLS è abilitato")
    public boolean isTrustStoreTypeValid() {
        return !enabled || StringUtils.hasText(trustStoreType);
    }

    @AssertTrue(message = "fse.security.mtls.trust-store-password è obbligatorio quando mTLS è abilitato")
    public boolean isTrustStorePasswordValid() {
        return !enabled || StringUtils.hasText(trustStorePassword);
    }
}