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
@ConfigurationProperties(prefix = "fse.jwt")
public class JwtProperties {

    private boolean enabled = true;

    private String keyStorePath;
    private String keyStoreType = "PKCS12";
    private String keyStorePassword;
    private String keyAlias;
    private String keyPassword;

    private String audience;
    private long expirationSeconds = 3600;
    private String issuerIdentifier;
    private String subjectFiscalCode;

    /**
     * Claim ministeriale subject_role.
     * Esempi ammessi da tabella ministeriale: AAS, APR, INF, FAR, DSA, RSA, DRS, ...
     */
    private String subjectRole;

    private String purposeOfUse = "TREATMENT";
    private String locality;
    private String subjectOrganization;
    private String subjectOrganizationId;
    private String subjectApplicationId;
    private String subjectApplicationVendor;
    private String subjectApplicationVersion;

    private Boolean useSubjectAsAuthor;
    private String kid;

    @AssertTrue(message = "fse.jwt.key-store-path è obbligatorio quando fse.jwt.enabled=true")
    public boolean isKeyStorePathValid() {
        return !enabled || StringUtils.hasText(keyStorePath);
    }

    @AssertTrue(message = "fse.jwt.key-store-type è obbligatorio quando fse.jwt.enabled=true")
    public boolean isKeyStoreTypeValid() {
        return !enabled || StringUtils.hasText(keyStoreType);
    }

    @AssertTrue(message = "fse.jwt.key-store-password è obbligatorio quando fse.jwt.enabled=true")
    public boolean isKeyStorePasswordValid() {
        return !enabled || StringUtils.hasText(keyStorePassword);
    }

    @AssertTrue(message = "fse.jwt.key-alias è obbligatorio quando fse.jwt.enabled=true")
    public boolean isKeyAliasValid() {
        return !enabled || StringUtils.hasText(keyAlias);
    }

    @AssertTrue(message = "fse.jwt.audience è obbligatorio quando fse.jwt.enabled=true")
    public boolean isAudienceValid() {
        return !enabled || StringUtils.hasText(audience);
    }

    @AssertTrue(message = "fse.jwt.subject-fiscal-code è obbligatorio quando fse.jwt.enabled=true")
    public boolean isSubjectFiscalCodeValid() {
        return !enabled || StringUtils.hasText(subjectFiscalCode);
    }

    @AssertTrue(message = "fse.jwt.subject-role è obbligatorio quando fse.jwt.enabled=true")
    public boolean isSubjectRoleValid() {
        return !enabled || StringUtils.hasText(subjectRole);
    }

    @AssertTrue(message = "fse.jwt.locality è obbligatorio quando fse.jwt.enabled=true")
    public boolean isLocalityValid() {
        return !enabled || StringUtils.hasText(locality);
    }

    @AssertTrue(message = "fse.jwt.subject-organization è obbligatorio quando fse.jwt.enabled=true")
    public boolean isSubjectOrganizationValid() {
        return !enabled || StringUtils.hasText(subjectOrganization);
    }

    @AssertTrue(message = "fse.jwt.subject-organization-id è obbligatorio quando fse.jwt.enabled=true")
    public boolean isSubjectOrganizationIdValid() {
        return !enabled || StringUtils.hasText(subjectOrganizationId);
    }

    @AssertTrue(message = "fse.jwt.subject-application-id è obbligatorio quando fse.jwt.enabled=true")
    public boolean isSubjectApplicationIdValid() {
        return !enabled || StringUtils.hasText(subjectApplicationId);
    }

    @AssertTrue(message = "fse.jwt.subject-application-vendor è obbligatorio quando fse.jwt.enabled=true")
    public boolean isSubjectApplicationVendorValid() {
        return !enabled || StringUtils.hasText(subjectApplicationVendor);
    }

    @AssertTrue(message = "fse.jwt.subject-application-version è obbligatorio quando fse.jwt.enabled=true")
    public boolean isSubjectApplicationVersionValid() {
        return !enabled || StringUtils.hasText(subjectApplicationVersion);
    }

    public String resolvedKeyPassword() {
        return StringUtils.hasText(keyPassword) ? keyPassword : keyStorePassword;
    }
}