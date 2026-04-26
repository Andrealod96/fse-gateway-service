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
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Component
public class JwtGenerator {

    private static final String TAX_CODE_OID = "2.16.840.1.113883.2.9.4.3.2";
    private static final String AUTH_ISS_PREFIX = "auth:";
    private static final String INTEGRITY_ISS_PREFIX = "integrity:";
    private static final String DEFAULT_SUBJECT_ROLE = "AAS";
    private static final String DEFAULT_WORKFLOW_ACTION_ID = "CREATE";
    private static final String DEFAULT_RSA_RESOURCE_HL7_TYPE = "('11488-4^^2.16.840.1.113883.6.1')";

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

    public Mono<GatewayJwtHeaders> generateGatewayHeadersForWorkflowStatus(GatewayStatusJwtContext context) {
        return Mono.fromCallable(() -> generateGatewayHeadersForWorkflowStatusBlocking(context))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<String> generateAuthenticationBearerToken() {
        return Mono.fromCallable(this::generateAuthenticationBearerTokenBlocking)
                .subscribeOn(Schedulers.boundedElastic());
    }

    private GatewayJwtHeaders generateGatewayHeadersBlocking(GatewayDocumentValidationRequest request) throws Exception {
        assertJwtEnabled();

        if (request == null) {
            throw new JwtGenerationException("Richiesta Gateway nulla");
        }

        KeyMaterial keyMaterial = loadKeyMaterial();
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(jwtProperties.getExpirationSeconds());

        String subjectCx = toCx(jwtProperties.getSubjectFiscalCode());
        String patientCx = toCx(request.getPatientFiscalCode());
        String issuerSuffix = resolveIssuerSuffix(keyMaterial.certificate());

        String authToken = signAuthToken(keyMaterial, issuerSuffix, subjectCx, now, exp);
        String signatureToken = signSignatureTokenForDocumentValidation(
                request,
                keyMaterial,
                issuerSuffix,
                subjectCx,
                patientCx,
                now,
                exp
        );

        return new GatewayJwtHeaders(authToken, signatureToken);
    }

    private GatewayJwtHeaders generateGatewayHeadersForWorkflowStatusBlocking(GatewayStatusJwtContext context) throws Exception {
        assertJwtEnabled();

        if (context == null) {
            throw new JwtGenerationException("Contesto JWT status workflow nullo");
        }

        KeyMaterial keyMaterial = loadKeyMaterial();
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(jwtProperties.getExpirationSeconds());

        String subjectFiscalCode = firstNonBlank(context.subjectFiscalCode(), jwtProperties.getSubjectFiscalCode());
        String subjectCx = toCx(subjectFiscalCode);
        String patientCx = toCx(context.patientFiscalCode());
        String issuerSuffix = resolveIssuerSuffix(keyMaterial.certificate());

        String authToken = signAuthToken(keyMaterial, issuerSuffix, subjectCx, now, exp);
        String signatureToken = signSignatureTokenForWorkflowStatus(
                context,
                keyMaterial,
                issuerSuffix,
                subjectCx,
                patientCx,
                now,
                exp
        );

        return new GatewayJwtHeaders(authToken, signatureToken);
    }

    private String generateAuthenticationBearerTokenBlocking() throws Exception {
        assertJwtEnabled();

        KeyMaterial keyMaterial = loadKeyMaterial();
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(jwtProperties.getExpirationSeconds());

        String subjectCx = toCx(jwtProperties.getSubjectFiscalCode());
        String issuerSuffix = resolveIssuerSuffix(keyMaterial.certificate());

        return signAuthToken(keyMaterial, issuerSuffix, subjectCx, now, exp);
    }

    private void assertJwtEnabled() {
        if (!jwtProperties.isEnabled()) {
            throw new JwtGenerationException("Generazione JWT disabilitata da configurazione");
        }
    }

    private String resolveIssuerSuffix(X509Certificate certificate) throws Exception {
        if (StringUtils.hasText(jwtProperties.getIssuerIdentifier())) {
            return jwtProperties.getIssuerIdentifier().trim();
        }
        return extractCommonName(certificate);
    }

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

    private String signSignatureTokenForDocumentValidation(
            GatewayDocumentValidationRequest request,
            KeyMaterial keyMaterial,
            String issuerSuffix,
            String subjectCx,
            String patientCx,
            Instant now,
            Instant exp
    ) throws Exception {
        String subjectRole = firstNonBlank(request.getSubjectRole(), jwtProperties.getSubjectRole(), DEFAULT_SUBJECT_ROLE);
        String purposeOfUse = firstNonBlank(request.getPurposeOfUse(), jwtProperties.getPurposeOfUse());
        String subjectOrganization = firstNonBlank(request.getSubjectOrganization(), jwtProperties.getSubjectOrganization());
        String subjectOrganizationId = firstNonBlank(request.getSubjectOrganizationId(), jwtProperties.getSubjectOrganizationId());
        String subjectApplicationId = firstNonBlank(request.getSubjectApplicationId(), jwtProperties.getSubjectApplicationId());
        String subjectApplicationVendor = firstNonBlank(request.getSubjectApplicationVendor(), jwtProperties.getSubjectApplicationVendor());
        String subjectApplicationVersion = firstNonBlank(request.getSubjectApplicationVersion(), jwtProperties.getSubjectApplicationVersion());
        String locality = firstNonBlank(request.getLocality(), jwtProperties.getLocality());

        validateRequired("subject_role", subjectRole);
        validateRequired("purpose_of_use", purposeOfUse);
        validateRequired("subject_organization", subjectOrganization);
        validateRequired("subject_organization_id", subjectOrganizationId);
        validateRequired("subject_application_id", subjectApplicationId);
        validateRequired("subject_application_vendor", subjectApplicationVendor);
        validateRequired("subject_application_version", subjectApplicationVersion);
        validateRequired("resource_hl7_type", request.getResourceHl7Type());
        validateRequired("action_id", request.getActionId());
        validateRequired("locality", locality);

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .issuer(INTEGRITY_ISS_PREFIX + issuerSuffix)
                .subject(subjectCx)
                .audience(jwtProperties.getAudience())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .jwtID(UUID.randomUUID().toString())
                .claim("subject_role", subjectRole)
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

    private String signSignatureTokenForWorkflowStatus(
            GatewayStatusJwtContext context,
            KeyMaterial keyMaterial,
            String issuerSuffix,
            String subjectCx,
            String patientCx,
            Instant now,
            Instant exp
    ) throws Exception {
        String subjectRole = firstNonBlank(context.subjectRole(), jwtProperties.getSubjectRole(), DEFAULT_SUBJECT_ROLE);
        String purposeOfUse = firstNonBlank(context.purposeOfUse(), jwtProperties.getPurposeOfUse());
        String subjectOrganization = firstNonBlank(context.subjectOrganization(), jwtProperties.getSubjectOrganization());
        String subjectOrganizationId = firstNonBlank(context.subjectOrganizationId(), jwtProperties.getSubjectOrganizationId());
        String subjectApplicationId = firstNonBlank(context.subjectApplicationId(), jwtProperties.getSubjectApplicationId());
        String subjectApplicationVendor = firstNonBlank(context.subjectApplicationVendor(), jwtProperties.getSubjectApplicationVendor());
        String subjectApplicationVersion = firstNonBlank(context.subjectApplicationVersion(), jwtProperties.getSubjectApplicationVersion());
        String locality = firstNonBlank(context.locality(), jwtProperties.getLocality());
        String actionId = firstNonBlank(context.actionId(), DEFAULT_WORKFLOW_ACTION_ID);
        String resourceHl7Type = firstNonBlank(
                context.resourceHl7Type(),
                jwtProperties.getWorkflowStatusResourceHl7Type(),
                DEFAULT_RSA_RESOURCE_HL7_TYPE
        );

        validateRequired("subject_role", subjectRole);
        validateRequired("purpose_of_use", purposeOfUse);
        validateRequired("subject_organization", subjectOrganization);
        validateRequired("subject_organization_id", subjectOrganizationId);
        validateRequired("subject_application_id", subjectApplicationId);
        validateRequired("subject_application_vendor", subjectApplicationVendor);
        validateRequired("subject_application_version", subjectApplicationVersion);
        validateRequired("locality", locality);
        validateRequired("action_id", actionId);
        validateRequired("resource_hl7_type", resourceHl7Type);

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .issuer(INTEGRITY_ISS_PREFIX + issuerSuffix)
                .subject(subjectCx)
                .audience(jwtProperties.getAudience())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .jwtID(UUID.randomUUID().toString())
                .claim("subject_role", subjectRole)
                .claim("purpose_of_use", purposeOfUse)
                .claim("subject_organization", subjectOrganization)
                .claim("subject_organization_id", subjectOrganizationId)
                .claim("patient_consent", Objects.requireNonNullElse(context.patientConsent(), Boolean.TRUE))
                .claim("action_id", actionId)
                .claim("resource_hl7_type", resourceHl7Type)
                .claim("person_id", patientCx)
                .claim("subject_application_id", subjectApplicationId)
                .claim("subject_application_vendor", subjectApplicationVendor)
                .claim("subject_application_version", subjectApplicationVersion)
                .claim("locality", locality);

        Boolean useSubjectAsAuthor = context.useSubjectAsAuthor() != null
                ? context.useSubjectAsAuthor()
                : jwtProperties.getUseSubjectAsAuthor();
        if (useSubjectAsAuthor != null) {
            builder.claim("use_subject_as_author", useSubjectAsAuthor);
        }

        return sign(builder.build(), keyMaterial);
    }

    private String sign(JWTClaimsSet claims, KeyMaterial keyMaterial) throws Exception {
        JWSHeader.Builder headerBuilder = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .x509CertChain(List.of(new Base64(
                        java.util.Base64.getEncoder().encodeToString(keyMaterial.certificate().getEncoded())
                )));

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

    private String toCx(String fiscalCode) {
        if (!StringUtils.hasText(fiscalCode)) {
            throw new JwtGenerationException("Codice fiscale mancante per costruire il claim CX");
        }

        String normalized = fiscalCode.trim().toUpperCase(Locale.ITALY);
        if (normalized.contains("^^^&")) {
            return normalized;
        }

        return normalized + "^^^&" + TAX_CODE_OID + "&ISO";
    }

    private String sha256Hex(byte[] payload) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(payload);
        return HexFormat.of().formatHex(hash);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
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
