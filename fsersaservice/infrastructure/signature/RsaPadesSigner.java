package it.thcs.fse.fsersaservice.infrastructure.signature;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import it.thcs.fse.fsersaservice.config.properties.SignatureProperties;
import it.thcs.fse.fsersaservice.config.properties.StorageProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import java.nio.file.StandardCopyOption;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.List;

@Component
public class RsaPadesSigner {

    private final SignatureProperties signatureProperties;
    private final StorageProperties storageProperties;
    private final ResourceLoader resourceLoader;

    public RsaPadesSigner(
            SignatureProperties signatureProperties,
            StorageProperties storageProperties,
            ResourceLoader resourceLoader
    ) {
        this.signatureProperties = signatureProperties;
        this.storageProperties = storageProperties;
        this.resourceLoader = resourceLoader;
    }

    public Path sign(String requestId, Path sourcePdfFile) {
        if (!signatureProperties.isEnabled()) {
            throw new RsaSignatureException("Firma PAdES disabilitata da configurazione");
        }

        if (sourcePdfFile == null || !Files.exists(sourcePdfFile)) {
            throw new RsaSignatureException("PDF sorgente da firmare non trovato: " + sourcePdfFile);
        }

        try {
            Path signedDir = resolveSignedDirectory();
            Files.createDirectories(signedDir);

            Path signedPdfFile = signedDir.resolve(requestId + ".pdf");

            DSSDocument documentToSign = new InMemoryDocument(
                    Files.readAllBytes(sourcePdfFile),
                    sourcePdfFile.getFileName().toString()
            );

            try (Pkcs12SignatureToken signingToken = openSignatureToken()) {
                DSSPrivateKeyEntry privateKeyEntry = resolvePrivateKey(signingToken);

                PAdESSignatureParameters parameters = buildSignatureParameters(privateKeyEntry);

                CommonCertificateVerifier certificateVerifier = new CommonCertificateVerifier();
                PAdESService padesService = new PAdESService(certificateVerifier);

                ToBeSigned dataToSign = padesService.getDataToSign(documentToSign, parameters);
                SignatureValue signatureValue = signingToken.sign(
                        dataToSign,
                        parameters.getDigestAlgorithm(),
                        privateKeyEntry
                );

                DSSDocument signedDocument = padesService.signDocument(
                        documentToSign,
                        parameters,
                        signatureValue
                );

                byte[] signedBytes = DSSUtils.toByteArray(signedDocument);
                if (signedBytes == null || signedBytes.length == 0) {
                    throw new RsaSignatureException("Il documento firmato restituito da DSS è vuoto");
                }

                Path tempFile = Files.createTempFile(signedDir, requestId + "-", ".pdf");
                try {
                    Files.write(tempFile, signedBytes);
                    Files.move(
                            tempFile,
                            signedPdfFile,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE
                    );
                    return signedPdfFile;
                } finally {
                    Files.deleteIfExists(tempFile);
                }
            }
        } catch (RsaSignatureException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RsaSignatureException("Errore durante la firma PAdES del PDF RSA", ex);
        }
    }

    private Path resolveSignedDirectory() {
        Path cdaDir = Path.of(storageProperties.getCdaDir()).toAbsolutePath().normalize();
        Path outputRoot = cdaDir.getParent();

        if (outputRoot == null) {
            throw new RsaSignatureException(
                    "Impossibile derivare la directory dei PDF firmati da storage.cda-dir"
            );
        }

        return outputRoot.resolve("signed");
    }

    private Pkcs12SignatureToken openSignatureToken() throws Exception {
        Resource keyStoreResource = resourceLoader.getResource(signatureProperties.getKeyStorePath());
        if (!keyStoreResource.exists()) {
            throw new RsaSignatureException(
                    "Keystore firma non trovato: " + signatureProperties.getKeyStorePath()
            );
        }

        if (!"PKCS12".equalsIgnoreCase(signatureProperties.getKeyStoreType())
                && !"P12".equalsIgnoreCase(signatureProperties.getKeyStoreType())) {
            throw new RsaSignatureException(
                    "Tipo keystore non supportato per questo signer: " + signatureProperties.getKeyStoreType()
            );
        }

        try (InputStream inputStream = keyStoreResource.getInputStream()) {
            return new Pkcs12SignatureToken(
                    inputStream,
                    new KeyStore.PasswordProtection(signatureProperties.getKeyStorePassword().toCharArray())
            );
        }
    }

    private DSSPrivateKeyEntry resolvePrivateKey(Pkcs12SignatureToken signingToken) throws Exception {
        String alias = signatureProperties.getKeyAlias();

        try {
            DSSPrivateKeyEntry byAlias = signingToken.getKey(
                    alias,
                    new KeyStore.PasswordProtection(signatureProperties.getKeyStorePassword().toCharArray())
            );
            if (byAlias != null) {
                return byAlias;
            }
        } catch (Exception ignored) {
            // fallback sotto
        }

        List<DSSPrivateKeyEntry> keys = signingToken.getKeys();
        if (keys == null || keys.isEmpty()) {
            throw new RsaSignatureException("Nessuna chiave privata trovata nel keystore di firma");
        }

        if (keys.size() == 1) {
            return keys.get(0);
        }

        throw new RsaSignatureException(
                "Impossibile risolvere la chiave privata per alias '" + alias + "' nel keystore"
        );
    }

    private PAdESSignatureParameters buildSignatureParameters(DSSPrivateKeyEntry privateKeyEntry) {
        PAdESSignatureParameters parameters = new PAdESSignatureParameters();
        parameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_B);
        parameters.setSignaturePackaging(SignaturePackaging.ENVELOPED);
        parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);
        parameters.setSigningCertificate(privateKeyEntry.getCertificate());
        parameters.setCertificateChain(privateKeyEntry.getCertificateChain());
        return parameters;
    }
}