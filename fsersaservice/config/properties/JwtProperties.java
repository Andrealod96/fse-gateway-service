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

    /**
     * Consente di disabilitare la generazione JWT in ambienti locali/non integrati.
     */
    private boolean enabled = true;

    /**
     * Keystore del certificato di signature usato per firmare i JWT ministeriali.
     */
    private String keyStorePath;

    private String keyStoreType = "PKCS12";
    private String keyStorePassword;
    private String keyAlias;
    private String keyPassword;

    /**
     * Audience del Gateway.
     * Validazione/Sogei: base URL del Gateway comprensiva di /v1.
     */
    private String audience;

    private long expirationSeconds = 3600;

    /**
     * Suffisso del claim iss.
     * Se valorizzato: iss = "auth:<issuerIdentifier>" / "integrity:<issuerIdentifier>".
     * Se vuoto: estratto dal CN del certificato X.509.
     */
    private String issuerIdentifier;

    /**
     * CF dell'operatore/sistema che invoca il Gateway: claim sub di entrambi i JWT.
     */
    private String subjectFiscalCode;

    /**
     * Ruolo dell'utente/sistema chiamante nel FSE-JWT-Signature.
     */
    private String subjectRole = "AAS";

    /**
     * Claim purpose_of_use. Per validazione/creazione: TREATMENT.
     */
    private String purposeOfUse = "TREATMENT";

    /**
     * Claim locality nel formato XON IHE.
     */
    private String locality;

    private String subjectOrganization;
    private String subjectOrganizationId;
    private String subjectApplicationId;
    private String subjectApplicationVendor;
    private String subjectApplicationVersion;

    /**
     * Resource HL7 type da usare nel JWT per la GET /status/{workflowInstanceId},
     * quando il contesto viene ricostruito dal workflow locale.
     */
    private String workflowStatusResourceHl7Type = "('11488-4^^2.16.840.1.113883.6.1')";

    private Boolean useSubjectAsAuthor;
    private String kid;

    @AssertTrue(message = "fse.jwt.key-store-path e obbligatorio quando fse.jwt.enabled=true")
    public boolean isKeyStorePathValid() {
        return !enabled || StringUtils.hasText(keyStorePath);
    }

    @AssertTrue(message = "fse.jwt.key-store-type e obbligatorio quando fse.jwt.enabled=true")
    public boolean isKeyStoreTypeValid() {
        return !enabled || StringUtils.hasText(keyStoreType);
    }

    @AssertTrue(message = "fse.jwt.key-store-password e obbligatorio quando fse.jwt.enabled=true")
    public boolean isKeyStorePasswordValid() {
        return !enabled || StringUtils.hasText(keyStorePassword);
    }

    @AssertTrue(message = "fse.jwt.key-alias e obbligatorio quando fse.jwt.enabled=true")
    public boolean isKeyAliasValid() {
        return !enabled || StringUtils.hasText(keyAlias);
    }

    @AssertTrue(message = "fse.jwt.audience e obbligatorio quando fse.jwt.enabled=true")
    public boolean isAudienceValid() {
        return !enabled || StringUtils.hasText(audience);
    }

    @AssertTrue(message = "fse.jwt.subject-fiscal-code e obbligatorio quando fse.jwt.enabled=true")
    public boolean isSubjectFiscalCodeValid() {
        return !enabled || StringUtils.hasText(subjectFiscalCode);
    }

    @AssertTrue(message = "fse.jwt.subject-role e obbligatorio quando fse.jwt.enabled=true")
    public boolean isSubjectRoleValid() {
        return !enabled || StringUtils.hasText(subjectRole);
    }

    @AssertTrue(message = "fse.jwt.locality e obbligatorio quando fse.jwt.enabled=true")
    public boolean isLocalityValid() {
        return !enabled || StringUtils.hasText(locality);
    }

    @AssertTrue(message = "fse.jwt.subject-organization e obbligatorio quando fse.jwt.enabled=true")
    public boolean isSubjectOrganizationValid() {
        return !enabled || StringUtils.hasText(subjectOrganization);
    }

    @AssertTrue(message = "fse.jwt.subject-organization-id e obbligatorio quando fse.jwt.enabled=true")
    public boolean isSubjectOrganizationIdValid() {
        return !enabled || StringUtils.hasText(subjectOrganizationId);
    }

    @AssertTrue(message = "fse.jwt.subject-application-id e obbligatorio quando fse.jwt.enabled=true")
    public boolean isSubjectApplicationIdValid() {
        return !enabled || StringUtils.hasText(subjectApplicationId);
    }

    @AssertTrue(message = "fse.jwt.subject-application-vendor e obbligatorio quando fse.jwt.enabled=true")
    public boolean isSubjectApplicationVendorValid() {
        return !enabled || StringUtils.hasText(subjectApplicationVendor);
    }

    @AssertTrue(message = "fse.jwt.subject-application-version e obbligatorio quando fse.jwt.enabled=true")
    public boolean isSubjectApplicationVersionValid() {
        return !enabled || StringUtils.hasText(subjectApplicationVersion);
    }

    public String resolvedKeyPassword() {
        return StringUtils.hasText(keyPassword) ? keyPassword : keyStorePassword;
    }
}
