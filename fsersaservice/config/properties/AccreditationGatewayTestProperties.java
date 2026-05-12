package it.thcs.fse.fsersaservice.config.properties;

import it.thcs.fse.fsersaservice.infrastructure.gateway.jwt.JwtNegativeTestMode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "fse.accreditation.gateway-tests")
public class AccreditationGatewayTestProperties {

    /**
     * Abilita la modalita specifica per i test di accreditamento Gateway.
     *
     * Deve restare false in modalita normale, staging reale e produzione.
     */
    private boolean enabled = false;

    /**
     * Se true, gli errori locali CDA/XSD/Schematron/terminologia vengono registrati,
     * ma non bloccano l'invio al Gateway.
     *
     * Serve solo per i casi KO ministeriali in cui il Gateway deve ricevere
     * un documento volutamente non conforme.
     */
    private boolean bypassLocalCdaValidation = false;

    /**
     * Se true, il claim person_id del FSE-JWT-Signature usa il codice fiscale
     * paziente esattamente come presente nel dominio/CDA, senza conversione
     * automatica in maiuscolo.
     *
     * Serve per casi KO CDA come CT6, dove il patientRole/id/@extension
     * deve restare volutamente in minuscolo ma il Gateway non deve fermarsi
     * prima sul disallineamento JWT/CDA.
     *
     * Deve restare false in modalita normale, staging reale e produzione.
     */
    private boolean preserveRawPatientFiscalCodeInJwt = false;

    /**
     * Modalita di alterazione controllata del JWT per i casi KO ministeriali.
     */
    private JwtNegativeTestMode jwtNegativeTestMode = JwtNegativeTestMode.NONE;
}
