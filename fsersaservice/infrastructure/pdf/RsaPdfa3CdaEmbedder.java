package it.thcs.fse.fsersaservice.infrastructure.pdf;

import it.thcs.fse.fsersaservice.config.properties.StorageProperties;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RsaPdfa3CdaEmbedder {

    private static final String CDA_EMBEDDED_MIME_TYPE = "application/xml";
    private static final String AF_RELATIONSHIP_VALUE = "Source";

    private final StorageProperties storageProperties;

    public RsaPdfa3CdaEmbedder(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public Path embed(String requestId, Path sourcePdfaFile, String cdaXml) {
        try {
            if (sourcePdfaFile == null || !Files.exists(sourcePdfaFile)) {
                throw new RsaPdfGenerationException(
                        "PDF/A-3 sorgente non trovato per embedding CDA: " + sourcePdfaFile
                );
            }

            if (cdaXml == null || cdaXml.isBlank()) {
                throw new RsaPdfGenerationException("Contenuto CDA XML vuoto: embedding non eseguibile");
            }

            Path embeddedDir = resolveEmbeddedDirectory();
            Files.createDirectories(embeddedDir);

            Path embeddedPdfFile = embeddedDir.resolve(requestId + ".pdf");
            Path tempFile = Files.createTempFile(embeddedDir, requestId + "-", ".pdf");

            byte[] xmlBytes = cdaXml.getBytes(StandardCharsets.UTF_8);
            String embeddedFileName = "cda.xml";

            try (PDDocument document = Loader.loadPDF(sourcePdfaFile.toFile())) {
                PDComplexFileSpecification fileSpecification = buildFileSpecification(
                        document,
                        embeddedFileName,
                        xmlBytes
                );

                attachToNamesDictionary(document, fileSpecification, embeddedFileName);
                attachToAssociatedFiles(document, fileSpecification, embeddedFileName);

                document.save(tempFile.toFile());
            } catch (Exception ex) {
                Files.deleteIfExists(tempFile);
                throw ex;
            }

            replaceTargetFile(tempFile, embeddedPdfFile);
            return embeddedPdfFile;
        } catch (Exception ex) {
            throw new RsaPdfGenerationException("Errore durante l'embedding del CDA nel PDF/A-3", ex);
        }
    }

    private Path resolveEmbeddedDirectory() {
        Path cdaDir = Path.of(storageProperties.getCdaDir()).toAbsolutePath().normalize();
        Path outputRoot = cdaDir.getParent();

        if (outputRoot == null) {
            throw new RsaPdfGenerationException(
                    "Impossibile derivare la directory pdfa-embedded da storage.cda-dir"
            );
        }

        return outputRoot.resolve("pdfa-embedded");
    }

    private PDComplexFileSpecification buildFileSpecification(
            PDDocument document,
            String embeddedFileName,
            byte[] xmlBytes
    ) throws Exception {
        PDEmbeddedFile embeddedFile;

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(xmlBytes)) {
            embeddedFile = new PDEmbeddedFile(document, inputStream);
            embeddedFile.setSubtype(CDA_EMBEDDED_MIME_TYPE);
            embeddedFile.setSize(xmlBytes.length);

            GregorianCalendar now = GregorianCalendar.from(ZonedDateTime.now(ZoneOffset.UTC));
            embeddedFile.setCreationDate(now);
            embeddedFile.setModDate(now);
        }

        PDComplexFileSpecification fileSpecification = new PDComplexFileSpecification();
        fileSpecification.setFile(embeddedFileName);
        fileSpecification.setFileUnicode(embeddedFileName);
        fileSpecification.setEmbeddedFile(embeddedFile);
        fileSpecification.getCOSObject().setName(COSName.AF_RELATIONSHIP, AF_RELATIONSHIP_VALUE);

        return fileSpecification;
    }

    private void attachToNamesDictionary(
            PDDocument document,
            PDComplexFileSpecification fileSpecification,
            String embeddedFileName
    ) throws Exception {
        PDDocumentCatalog catalog = document.getDocumentCatalog();

        PDDocumentNameDictionary namesDictionary = catalog.getNames();
        if (namesDictionary == null) {
            namesDictionary = new PDDocumentNameDictionary(catalog);
        }

        PDEmbeddedFilesNameTreeNode embeddedFiles = namesDictionary.getEmbeddedFiles();
        if (embeddedFiles == null) {
            embeddedFiles = new PDEmbeddedFilesNameTreeNode();
        }

        Map<String, PDComplexFileSpecification> names = embeddedFiles.getNames();
        Map<String, PDComplexFileSpecification> updatedNames = new LinkedHashMap<>();

        if (names != null && !names.isEmpty()) {
            updatedNames.putAll(names);
        }

        updatedNames.put(embeddedFileName, fileSpecification);

        embeddedFiles.setNames(updatedNames);
        namesDictionary.setEmbeddedFiles(embeddedFiles);
        catalog.setNames(namesDictionary);
    }

    private void attachToAssociatedFiles(
            PDDocument document,
            PDComplexFileSpecification fileSpecification,
            String embeddedFileName
    ) {
        PDDocumentCatalog catalog = document.getDocumentCatalog();

        COSArray updatedAfArray = new COSArray();
        COSArray existingAfArray = catalog.getCOSObject().getCOSArray(COSName.AF);

        if (existingAfArray != null) {
            for (COSBase base : existingAfArray) {
                COSBase resolved = unwrap(base);
                if (resolved == null) {
                    continue;
                }

                if (resolved instanceof org.apache.pdfbox.cos.COSDictionary dictionary) {
                    PDComplexFileSpecification existingSpec = new PDComplexFileSpecification(dictionary);
                    String existingName = resolveFileName(existingSpec);

                    if (!embeddedFileName.equals(existingName)) {
                        updatedAfArray.add(dictionary);
                    }
                } else {
                    updatedAfArray.add(resolved);
                }
            }
        }

        updatedAfArray.add(fileSpecification.getCOSObject());
        catalog.getCOSObject().setItem(COSName.AF, updatedAfArray);
    }

    private COSBase unwrap(COSBase base) {
        if (base instanceof COSObject object) {
            return object.getObject();
        }
        return base;
    }

    private String resolveFileName(PDComplexFileSpecification fileSpec) {
        if (notBlank(fileSpec.getFileUnicode())) {
            return fileSpec.getFileUnicode();
        }
        if (notBlank(fileSpec.getFilename())) {
            return fileSpec.getFilename();
        }
        if (notBlank(fileSpec.getFile())) {
            return fileSpec.getFile();
        }
        if (notBlank(fileSpec.getFileDos())) {
            return fileSpec.getFileDos();
        }
        if (notBlank(fileSpec.getFileMac())) {
            return fileSpec.getFileMac();
        }
        if (notBlank(fileSpec.getFileUnix())) {
            return fileSpec.getFileUnix();
        }
        return "";
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private void replaceTargetFile(Path tempFile, Path targetFile) throws Exception {
        try {
            Files.move(
                    tempFile,
                    targetFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(
                    tempFile,
                    targetFile,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}