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
     * Docker locale: FSE_Gateway
     * Sogei staging: https://modipa-val.fse.salute.gov.it/govway/rest/in/FSE/gateway/v1
     */
    private String audience;

    private long expirationSeconds = 3600;

    /**
     * Suffisso del claim iss.
     * Se valorizzato: iss = "auth:<issuerIdentifier>" / "integrity:<issuerIdentifier>"
     * Se vuoto: estratto dal CN del certificato X.509.
     * Docker test: valorizzare con codice struttura (es. "120201").
     * Produzione Sogei: lasciare vuoto.
     */
    private String issuerIdentifier;

    /**
     * CF dell'operatore/sistema che invoca il Gateway — claim "sub" di entrambi i JWT.
     *
     * ATTENZIONE: NON è il CF dell'autore clinico del documento.
     * È il CF dell'identità tecnica/operatore che chiama il Gateway.
     *
     * Formato: CF grezzo (es. RSSMRA22A01A399Z).
     * Il generatore produce: RSSMRA22A01A399Z^^^&2.16.840.1.113883.2.9.4.3.2&ISO
     *
     * Docker test: usare un CF di test accettato dal container ministeriale.
     */
    private String subjectFiscalCode;

    /**
     * Claim purpose_of_use. Per validazione e creazione: TREATMENT.
     */
    private String purposeOfUse = "TREATMENT";

    /**
     * Claim locality nel formato XON IHE (obbligatorio).
     * Formato: NOME^^^^^&OID_STRUTTURA&ISO^^^^CODICE_STS
     * Esempio: AMBULATORIO RSA^^^^^&2.16.840.1.113883.2.9.4.1.3&ISO^^^^160111123456
     */
    private String locality;

    private String subjectOrganization;
    private String subjectOrganizationId;
    private String subjectApplicationId;
    private String subjectApplicationVendor;
    private String subjectApplicationVersion;

    private Boolean useSubjectAsAuthor;
    private String kid;

    // ─── Validazione condizionale ─────────────────────────────────────────────

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

    @AssertTrue(message = "fse.jwt.locality è obbligatorio quando fse.jwt.enabled=true (formato: NOME^^^^^&OID&ISO^^^^CODICE)")
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