package it.thcs.fse.fsersaservice.infrastructure.pdf;

import it.thcs.fse.fsersaservice.config.properties.StorageProperties;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.AdobePDFSchema;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.schema.XMPBasicSchema;
import org.apache.xmpbox.xml.XmpSerializer;
import org.springframework.stereotype.Component;
import java.nio.file.StandardCopyOption;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;

@Component
public class RsaPdfa3Converter {

    private static final String PDF_A_CONFORMANCE = "B";
    private static final int PDF_A_PART = 3;
    private static final String SRGB_OUTPUT_CONDITION = "sRGB IEC61966-2.1";
    private static final String SRGB_REGISTRY_NAME = "https://www.color.org";

    private final StorageProperties storageProperties;

    public RsaPdfa3Converter(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public Path convert(String requestId, Path sourcePdfFile) {
        try {
            if (sourcePdfFile == null || !Files.exists(sourcePdfFile)) {
                throw new RsaPdfGenerationException("PDF sorgente non trovato per conversione PDF/A-3: " + sourcePdfFile);
            }

            Path pdfaDir = resolvePdfaDirectory();
            Files.createDirectories(pdfaDir);

            Path pdfaFile = pdfaDir.resolve(requestId + ".pdf");

            try (PDDocument document = Loader.loadPDF(sourcePdfFile.toFile())) {
                PDDocumentCatalog catalog = document.getDocumentCatalog();
                catalog.setLanguage("it-IT");

                attachXmpMetadata(document);
                attachSrgbOutputIntent(document);

                Path tempFile = Files.createTempFile(pdfaDir, requestId + "-", ".pdf");
                try {
                    document.save(tempFile.toFile());
                    Files.move(
                            tempFile,
                            pdfaFile,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE
                    );
                } finally {
                    Files.deleteIfExists(tempFile);
                }            }

            return pdfaFile;
        } catch (Exception ex) {
            throw new RsaPdfGenerationException("Errore durante la conversione del PDF in PDF/A-3", ex);
        }
    }

    private Path resolvePdfaDirectory() {
        Path cdaDir = Path.of(storageProperties.getCdaDir()).toAbsolutePath().normalize();
        Path outputRoot = cdaDir.getParent();

        if (outputRoot == null) {
            throw new RsaPdfGenerationException("Impossibile derivare la directory PDF/A-3 da storage.cda-dir");
        }

        return outputRoot.resolve("pdfa");
    }

    private void attachXmpMetadata(PDDocument document) throws Exception {
        PDDocumentInformation info = document.getDocumentInformation();

        XMPMetadata xmp = XMPMetadata.createXMPMetadata();

        DublinCoreSchema dc = xmp.createAndAddDublinCoreSchema();
        dc.setTitle(firstNonBlank(info.getTitle(), "Referto di Specialistica Ambulatoriale"));
        dc.addCreator(firstNonBlank(info.getAuthor(), "Microservizio FSE RSA"));
        dc.setDescription(firstNonBlank(info.getSubject(), "Documento PDF/A-3 generato dal microservizio RSA"));

        AdobePDFSchema adobePdf = xmp.createAndAddAdobePDFSchema();
        adobePdf.setProducer(firstNonBlank(info.getProducer(), "Apache PDFBox"));

        XMPBasicSchema xmpBasic = xmp.createAndAddXMPBasicSchema();
        GregorianCalendar now = GregorianCalendar.from(ZonedDateTime.now(ZoneOffset.UTC));
        xmpBasic.setCreateDate(now);
        xmpBasic.setModifyDate(now);
        xmpBasic.setMetadataDate(now);
        xmpBasic.setCreatorTool(firstNonBlank(info.getCreator(), "Microservizio FSE RSA"));

        PDFAIdentificationSchema pdfaId = xmp.createAndAddPDFAIdentificationSchema();
        pdfaId.setPart(PDF_A_PART);
        pdfaId.setConformance(PDF_A_CONFORMANCE);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new XmpSerializer().serialize(xmp, baos, true);

        PDMetadata metadata = new PDMetadata(document);
        metadata.importXMPMetadata(baos.toByteArray());
        document.getDocumentCatalog().setMetadata(metadata);
    }

    private void attachSrgbOutputIntent(PDDocument document) throws Exception {
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        if (catalog.getOutputIntents() != null && !catalog.getOutputIntents().isEmpty()) {
            return;
        }

        ICC_Profile iccProfile = ICC_Profile.getInstance(ColorSpace.CS_sRGB);

        try (ByteArrayInputStream colorProfileStream = new ByteArrayInputStream(iccProfile.getData())) {
            PDOutputIntent outputIntent = new PDOutputIntent(document, colorProfileStream);
            outputIntent.setInfo(SRGB_OUTPUT_CONDITION);
            outputIntent.setOutputCondition(SRGB_OUTPUT_CONDITION);
            outputIntent.setOutputConditionIdentifier(SRGB_OUTPUT_CONDITION);
            outputIntent.setRegistryName(SRGB_REGISTRY_NAME);

            catalog.addOutputIntent(outputIntent);
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}