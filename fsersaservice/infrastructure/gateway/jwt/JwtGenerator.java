package it.thcs.fse.fsersaservice.infrastructure.gateway.jwt;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import it.thcs.fse.fsersaservice.config.properties.JwtProperties;
import it.thcs.fse.fsersaservice.infrastructure.gateway.dto.request.GatewayDocumentValidationRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Generatore dei due token JWT richiesti dal Gateway FSE 2.0 per ogni chiamata.
 *
 * Authorization: Bearer → Authentication Bearer Token
 *   - iss: "auth:<CN_certificato>" (o "auth:<issuerIdentifier>" se configurato)
 *   - sub: CF operatore chiamante in formato CX IHE
 *
 * FSE-JWT-Signature → FSE-JWT-Signature Token
 *   - iss: "integrity:<CN_certificato>" (o "integrity:<issuerIdentifier>" se configurato)
 *   - sub: stesso CF operatore chiamante
 *   - custom claims clinici (subject_role, person_id, ecc.)
 *
 * Riferimento: it-fse-support/doc/integrazione-gateway/README.md cap. 14
 */
@Component
public class JwtGenerator {

    private static final String TAX_CODE_OID = "2.16.840.1.113883.2.9.4.3.2";
    private static final String AUTH_ISS_PREFIX = "auth:";
    private static final String INTEGRITY_ISS_PREFIX = "integrity:";

    private final JwtProperties jwtProperties;
    private final ResourceLoader resourceLoader;

    public JwtGenerator(JwtProperties jwtProperties, ResourceLoader resourceLoader) {
        this.jwtProperties = jwtProperties;
        this.resourceLoader = resourceLoader;
    }

    public Mono<GatewayJwtHeaders> generateGatewayHeaders(GatewayDocumentValidationRequest request) {
        return Mono.fromCallable(() -> generateGatewayHeadersBlocking(request))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private GatewayJwtHeaders generateGatewayHeadersBlocking(GatewayDocumentValidationRequest request) throws Exception {
        if (!jwtProperties.isEnabled()) {
            throw new JwtGenerationException("Generazione JWT disabilitata da configurazione");
        }

        KeyMaterial keyMaterial = loadKeyMaterial();

        Instant now = Instant.now();
        Instant exp = now.plusSeconds(jwtProperties.getExpirationSeconds());

        // FIX CRITICO: il claim "sub" deve essere il CF dell'operatore/sistema che chiama
        // il Gateway, NON il CF dell'autore clinico del documento CDA.
        // Viene letto da fse.jwt.subject-fiscal-code nella configurazione.
        // In ambiente Docker di test: CF di test accettato dal container ministeriale.
        // In produzione Sogei: CF dell'utente autenticato che invoca il servizio.
        String subjectCx = toCx(jwtProperties.getSubjectFiscalCode());

        // Il CF del paziente resta nel claim "person_id" del FSE-JWT-Signature
        String patientCx = toCx(request.getPatientFiscalCode());

        // issuerSuffix: usa issuerIdentifier dalla config se presente, altrimenti CN del cert.
        // Docker test: fse.jwt.issuer-identifier=<codiceStruttura>
        // Produzione: lasciare vuoto, usa il CN del certificato Sogei.
        String issuerSuffix = resolveIssuerSuffix(keyMaterial.certificate());

        String authToken = signAuthToken(keyMaterial, issuerSuffix, subjectCx, now, exp);
        String signatureToken = signSignatureToken(request, keyMaterial, issuerSuffix, subjectCx, patientCx, now, exp);

        org.slf4j.LoggerFactory.getLogger(JwtGenerator.class).info("DEBUG JWT Auth: {}", authToken);
        org.slf4j.LoggerFactory.getLogger(JwtGenerator.class).info("DEBUG JWT Signature: {}", signatureToken);

        return new GatewayJwtHeaders(authToken, signatureToken);
    }

    /**
     * Risolve il suffisso del claim iss.
     * Priorità: 1) fse.jwt.issuer-identifier, 2) CN del certificato X.509.
     */
    private String resolveIssuerSuffix(X509Certificate certificate) throws Exception {
        if (StringUtils.hasText(jwtProperties.getIssuerIdentifier())) {
            return jwtProperties.getIssuerIdentifier().trim();
        }
        return extractCommonName(certificate);
    }

    /**
     * Genera il token Authorization Bearer.
     * Claim: iss, sub, aud, iat, exp, jti.
     */
    private String signAuthToken(
            KeyMaterial keyMaterial,
            String issuerSuffix,
            String subjectCx,
            Instant now,
            Instant exp
    ) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(AUTH_ISS_PREFIX + issuerSuffix)
                .subject(subjectCx)
                .audience(jwtProperties.getAudience())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .jwtID(UUID.randomUUID().toString())
                .build();

        return sign(claims, keyMaterial);
    }

    /**
     * Genera il token FSE-JWT-Signature.
     * Claim: tutti quelli del bearer + custom claims clinici ministeriali.
     */
    private String signSignatureToken(
            GatewayDocumentValidationRequest request,
            KeyMaterial keyMaterial,
            String issuerSuffix,
            String subjectCx,
            String patientCx,
            Instant now,
            Instant exp
    ) throws Exception {
        String purposeOfUse = firstNonBlank(request.getPurposeOfUse(), jwtProperties.getPurposeOfUse());
        String subjectOrganization = firstNonBlank(request.getSubjectOrganization(), jwtProperties.getSubjectOrganization());
        String subjectOrganizationId = firstNonBlank(request.getSubjectOrganizationId(), jwtProperties.getSubjectOrganizationId());
        String subjectApplicationId = firstNonBlank(request.getSubjectApplicationId(), jwtProperties.getSubjectApplicationId());
        String subjectApplicationVendor = firstNonBlank(request.getSubjectApplicationVendor(), jwtProperties.getSubjectApplicationVendor());
        String subjectApplicationVersion = firstNonBlank(request.getSubjectApplicationVersion(), jwtProperties.getSubjectApplicationVersion());
        String locality = firstNonBlank(request.getLocality(), jwtProperties.getLocality());

        validateRequired("subjectRole", request.getSubjectRole());
        validateRequired("purposeOfUse", purposeOfUse);
        validateRequired("subjectOrganization", subjectOrganization);
        validateRequired("subjectOrganizationId", subjectOrganizationId);
        validateRequired("subjectApplicationId", subjectApplicationId);
        validateRequired("subjectApplicationVendor", subjectApplicationVendor);
        validateRequired("subjectApplicationVersion", subjectApplicationVersion);
        validateRequired("resourceHl7Type", request.getResourceHl7Type());
        validateRequired("actionId", request.getActionId());
        validateRequired("locality", locality);

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .issuer(INTEGRITY_ISS_PREFIX + issuerSuffix)
                .subject(subjectCx)
                .audience(jwtProperties.getAudience())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .jwtID(UUID.randomUUID().toString())
                .claim("subject_role", request.getSubjectRole())
                .claim("purpose_of_use", purposeOfUse)
                .claim("subject_organization", subjectOrganization)
                .claim("subject_organization_id", subjectOrganizationId)
                .claim("patient_consent", Objects.requireNonNullElse(request.getPatientConsent(), Boolean.TRUE))
                .claim("action_id", request.getActionId())
                .claim("resource_hl7_type", request.getResourceHl7Type())
                .claim("person_id", patientCx)
                .claim("subject_application_id", subjectApplicationId)
                .claim("subject_application_vendor", subjectApplicationVendor)
                .claim("subject_application_version", subjectApplicationVersion)
                .claim("locality", locality);

        Boolean useSubjectAsAuthor = request.getUseSubjectAsAuthor() != null
                ? request.getUseSubjectAsAuthor()
                : jwtProperties.getUseSubjectAsAuthor();
        if (useSubjectAsAuthor != null) {
            builder.claim("use_subject_as_author", useSubjectAsAuthor);
        }

        if (request.isIncludeAttachmentHash()) {
            builder.claim("attachment_hash", sha256Hex(request.getFileBytes()));
        }

        return sign(builder.build(), keyMaterial);
    }

    private String sign(JWTClaimsSet claims, KeyMaterial keyMaterial) throws Exception {
        JWSHeader.Builder headerBuilder = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .x509CertChain(List.of(new Base64(java.util.Base64.getEncoder().encodeToString(
                        keyMaterial.certificate().getEncoded()
                ))));

        if (StringUtils.hasText(jwtProperties.getKid())) {
            headerBuilder.keyID(jwtProperties.getKid());
        }

        SignedJWT signedJwt = new SignedJWT(headerBuilder.build(), claims);
        signedJwt.sign(new RSASSASigner(keyMaterial.privateKey()));
        return signedJwt.serialize();
    }

    private KeyMaterial loadKeyMaterial() throws Exception {
        Resource keyStoreResource = resourceLoader.getResource(jwtProperties.getKeyStorePath());
        if (!keyStoreResource.exists()) {
            throw new JwtGenerationException("Keystore JWT non trovato: " + jwtProperties.getKeyStorePath());
        }

        KeyStore keyStore = KeyStore.getInstance(jwtProperties.getKeyStoreType());
        try (InputStream inputStream = keyStoreResource.getInputStream()) {
            keyStore.load(inputStream, jwtProperties.getKeyStorePassword().toCharArray());
        }

        Key key = keyStore.getKey(
                jwtProperties.getKeyAlias(),
                jwtProperties.resolvedKeyPassword().toCharArray()
        );

        if (!(key instanceof RSAPrivateKey rsaPrivateKey)) {
            throw new JwtGenerationException("La chiave JWT deve essere RSA per RS256");
        }

        Certificate certificate = keyStore.getCertificate(jwtProperties.getKeyAlias());
        if (!(certificate instanceof X509Certificate x509Certificate)) {
            throw new JwtGenerationException("Certificato X509 non trovato per alias: " + jwtProperties.getKeyAlias());
        }

        return new KeyMaterial(rsaPrivateKey, x509Certificate);
    }

    private String extractCommonName(X509Certificate certificate) throws Exception {
        LdapName ldapName = new LdapName(certificate.getSubjectX500Principal().getName());
        for (Rdn rdn : ldapName.getRdns()) {
            if ("CN".equalsIgnoreCase(rdn.getType())) {
                return String.valueOf(rdn.getValue());
            }
        }
        throw new JwtGenerationException("Common Name (CN) non trovato nel certificato JWT");
    }

    /**
     * Converte un codice fiscale grezzo nel formato CX IHE (ITI TF-3).
     * Input:  RSSMRA22A01A399Z
     * Output: RSSMRA22A01A399Z^^^&2.16.840.1.113883.2.9.4.3.2&ISO
     */
    private String toCx(String fiscalCode) {
        if (!StringUtils.hasText(fiscalCode)) {
            throw new JwtGenerationException("Codice fiscale mancante per costruire il claim CX");
        }
        String normalized = fiscalCode.trim().toUpperCase(Locale.ITALY);
        if (normalized.contains("^^^&")) {
            return normalized; // già in formato CX
        }
        return normalized + "^^^&" + TAX_CODE_OID + "&ISO";
    }

    private String sha256Hex(byte[] payload) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(payload);
        return HexFormat.of().formatHex(hash);
    }

    private String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) return first.trim();
        if (StringUtils.hasText(second)) return second.trim();
        return null;
    }

    private void validateRequired(String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new JwtGenerationException("Claim JWT obbligatorio mancante: " + fieldName);
        }
    }

    private record KeyMaterial(RSAPrivateKey privateKey, X509Certificate certificate) {
    }
}