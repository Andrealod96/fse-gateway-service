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
     * Esempi: classpath:certs/gateway-signature.p12, file:/opt/app/certs/gateway-signature.p12
     */
    private String keyStorePath;

    /** PKCS12, JKS, ... */
    private String keyStoreType = "PKCS12";

    private String keyStorePassword;

    /** Alias della chiave di firma nel keystore. */
    private String keyAlias;

    /**
     * Password della private key. Se omessa, viene riusata keyStorePassword.
     */
    private String keyPassword;

    /**
     * Audience del Gateway.
     * Docker locale: FSE_Gateway
     * Sogei staging: https://modipa-val.fse.salute.gov.it/govway/rest/in/FSE/gateway/v1
     */
    private String audience;

    /** Durata token in secondi. */
    private long expirationSeconds = 3600;

    /**
     * Identificativo usato come suffisso del claim iss.
     *
     * Se valorizzato: iss = "auth:<issuerIdentifier>" / "integrity:<issuerIdentifier>"
     * Se vuoto o assente: il suffisso viene estratto dal CN del certificato X.509.
     *
     * Usare in ambiente Docker di test valorizzando con il codice struttura (es. "120201").
     * In produzione Sogei lasciare vuoto: il CN del certificato è il valore corretto.
     */
    private String issuerIdentifier;

    /**
     * Claim default per FSE-JWT-Signature.
     * Per validazione/creazione la documentazione indica TREATMENT.
     */
    private String purposeOfUse = "TREATMENT";

    /**
     * Claim locality.
     */
    private String locality;

    private String subjectOrganization;
    private String subjectOrganizationId;

    private String subjectApplicationId;
    private String subjectApplicationVendor;
    private String subjectApplicationVersion;

    /** Claim opzionale use_subject_as_author. */
    private Boolean useSubjectAsAuthor;

    /** Header JWT opzionale kid. */
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