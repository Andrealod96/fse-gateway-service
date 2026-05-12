package it.thcs.fse.fsersaservice.infrastructure.pdf;

import it.thcs.fse.fsersaservice.config.properties.StorageProperties;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.nio.file.StandardCopyOption;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Component
public class RsaPdfRenderer {

    private static final String FONT_REGULAR_CLASSPATH = "fonts/NotoSans-Regular.ttf";
    private static final String FONT_BOLD_CLASSPATH = "fonts/NotoSans-Bold.ttf";

    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float MARGIN = 50f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2);

    private static final float TITLE_FONT_SIZE = 16f;
    private static final float HEADING_FONT_SIZE = 12f;
    private static final float BODY_FONT_SIZE = 10f;
    private static final float LINE_HEIGHT = 14f;
    private static final float PARAGRAPH_SPACING = 6f;
    private static final float SECTION_SPACING = 12f;

    private final StorageProperties storageProperties;

    public RsaPdfRenderer(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public Path render(String requestId, String cdaXml) {
        try {
            Document cdaDocument = parseXml(cdaXml);
            Path pdfDir = resolvePdfDirectory();
            Files.createDirectories(pdfDir);

            Path pdfFile = pdfDir.resolve(requestId + ".pdf");

            try (PDDocument pdfDocument = new PDDocument()) {
                PdfFonts fonts = loadFonts(pdfDocument);
                enrichMetadata(pdfDocument, cdaDocument, requestId);

                PdfCanvas canvas = new PdfCanvas(pdfDocument, fonts);

                canvas.writeLine("Referto di Specialistica Ambulatoriale", fonts.bold(), TITLE_FONT_SIZE);
                canvas.addVerticalSpace(SECTION_SPACING);

                canvas.writeKeyValue("Request ID", requestId);
                canvas.writeKeyValue("Data generazione PDF", OffsetDateTime.now().toString());
                canvas.writeKeyValue(
                        "Titolo CDA",
                        firstNonBlank(text(cdaDocument, "/hl7:ClinicalDocument/hl7:title"), "Documento clinico")
                );
                canvas.writeKeyValue("Documento", formatDocumentIdentifier(cdaDocument));
                canvas.writeKeyValue("Paziente", formatPatient(cdaDocument));
                canvas.writeKeyValue("Autore", formatAuthor(cdaDocument));
                canvas.writeKeyValue("Validatore legale", formatLegalAuthenticator(cdaDocument));
                canvas.writeKeyValue("Data documento", formatEffectiveTime(cdaDocument));
                canvas.writeKeyValue(
                        "Confidenzialità",
                        firstNonBlank(
                                attr(cdaDocument, "/hl7:ClinicalDocument/hl7:confidentialityCode", "displayName"),
                                attr(cdaDocument, "/hl7:ClinicalDocument/hl7:confidentialityCode", "code"),
                                "N/D"
                        )
                );

                canvas.addVerticalSpace(SECTION_SPACING);

                List<PdfSection> sections = extractSections(cdaDocument);
                if (sections.isEmpty()) {
                    canvas.writeSectionTitle("Contenuto clinico");
                    canvas.writeParagraph(
                            "Nessuna sezione clinica trovata nel CDA.",
                            fonts.regular(),
                            BODY_FONT_SIZE
                    );
                } else {
                    for (PdfSection section : sections) {
                        canvas.writeSectionTitle(section.title());
                        canvas.writeParagraph(section.content(), fonts.regular(), BODY_FONT_SIZE);
                    }
                }

                canvas.close();
                Path tempFile = Files.createTempFile(pdfDir, requestId + "-", ".pdf");
                try {
                    pdfDocument.save(tempFile.toFile());
                    Files.move(
                            tempFile,
                            pdfFile,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE
                    );
                } finally {
                    Files.deleteIfExists(tempFile);
                }
            }

            return pdfFile;
        } catch (Exception ex) {
            throw new RsaPdfGenerationException("Errore durante la generazione del PDF base RSA", ex);
        }
    }

    private PdfFonts loadFonts(PDDocument document) throws IOException {
        ClassPathResource regularResource = new ClassPathResource(FONT_REGULAR_CLASSPATH);
        ClassPathResource boldResource = new ClassPathResource(FONT_BOLD_CLASSPATH);

        if (!regularResource.exists()) {
            throw new RsaPdfGenerationException("Font non trovato: " + FONT_REGULAR_CLASSPATH);
        }
        if (!boldResource.exists()) {
            throw new RsaPdfGenerationException("Font non trovato: " + FONT_BOLD_CLASSPATH);
        }

        try (InputStream regularIs = regularResource.getInputStream();
             InputStream boldIs = boldResource.getInputStream()) {

            PDFont regular = PDType0Font.load(document, regularIs, true);
            PDFont bold = PDType0Font.load(document, boldIs, true);
            return new PdfFonts(regular, bold);
        }
    }

    private Path resolvePdfDirectory() {
        Path cdaDir = Path.of(storageProperties.getCdaDir()).toAbsolutePath().normalize();
        Path outputRoot = cdaDir.getParent();

        if (outputRoot == null) {
            throw new RsaPdfGenerationException("Impossibile derivare la directory PDF da storage.cda-dir");
        }

        return outputRoot.resolve("pdf");
    }

    private void enrichMetadata(PDDocument pdfDocument, Document cdaDocument, String requestId) throws Exception {
        PDDocumentInformation info = pdfDocument.getDocumentInformation();
        info.setTitle(firstNonBlank(
                text(cdaDocument, "/hl7:ClinicalDocument/hl7:title"),
                "Referto di Specialistica Ambulatoriale"
        ));
        info.setAuthor(firstNonBlank(formatAuthor(cdaDocument), "Microservizio FSE RSA"));
        info.setSubject("PDF base generato da CDA validato");
        info.setCreator("Microservizio FSE RSA");
        info.setProducer("Apache PDFBox");
        info.setKeywords("FSE,RSA,CDA,PDF");
        info.setCustomMetadataValue("requestId", requestId);
    }

    private Document parseXml(String xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        return factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
    }

    private List<PdfSection> extractSections(Document document) throws Exception {
        List<PdfSection> sections = new ArrayList<>();
        NodeList nodes = nodes(
                document,
                "/hl7:ClinicalDocument/hl7:component/hl7:structuredBody/hl7:component/hl7:section"
        );

        for (int i = 0; i < nodes.getLength(); i++) {
            Node section = nodes.item(i);

            String title = text(section, "normalize-space(string(hl7:title))");
            String code = text(section, "normalize-space(string(hl7:code/@code))");
            String content = normalizeWhitespace(text(section, "normalize-space(string(hl7:text))"));

            if (isBlank(title)) {
                title = "Sezione " + (isBlank(code) ? String.valueOf(i + 1) : code);
            }

            if (isBlank(content)) {
                content = "Sezione presente nel CDA ma senza narrative block valorizzato.";
            }

            sections.add(new PdfSection(title, content));
        }

        return sections;
    }

    private String formatDocumentIdentifier(Document document) throws Exception {
        String root = attr(document, "/hl7:ClinicalDocument/hl7:id", "root");
        String extension = attr(document, "/hl7:ClinicalDocument/hl7:id", "extension");
        return joinNonBlank(" / ", root, extension);
    }

    private String formatPatient(Document document) throws Exception {
        String family = text(
                document,
                "/hl7:ClinicalDocument/hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:name/hl7:family"
        );
        String given = text(
                document,
                "/hl7:ClinicalDocument/hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:name/hl7:given"
        );
        String fiscalCode = attr(
                document,
                "/hl7:ClinicalDocument/hl7:recordTarget/hl7:patientRole/hl7:id",
                "extension"
        );
        return joinNonBlank(" - ", joinNonBlank(" ", given, family), fiscalCode);
    }

    private String formatAuthor(Document document) throws Exception {
        String prefix = text(
                document,
                "/hl7:ClinicalDocument/hl7:author/hl7:assignedAuthor/hl7:assignedPerson/hl7:name/hl7:prefix"
        );
        String given = text(
                document,
                "/hl7:ClinicalDocument/hl7:author/hl7:assignedAuthor/hl7:assignedPerson/hl7:name/hl7:given"
        );
        String family = text(
                document,
                "/hl7:ClinicalDocument/hl7:author/hl7:assignedAuthor/hl7:assignedPerson/hl7:name/hl7:family"
        );
        String identifier = attr(
                document,
                "/hl7:ClinicalDocument/hl7:author/hl7:assignedAuthor/hl7:id",
                "extension"
        );
        return joinNonBlank(" - ", joinNonBlank(" ", prefix, given, family), identifier);
    }

    private String formatLegalAuthenticator(Document document) throws Exception {
        String prefix = text(
                document,
                "/hl7:ClinicalDocument/hl7:legalAuthenticator/hl7:assignedEntity/hl7:assignedPerson/hl7:name/hl7:prefix"
        );
        String given = text(
                document,
                "/hl7:ClinicalDocument/hl7:legalAuthenticator/hl7:assignedEntity/hl7:assignedPerson/hl7:name/hl7:given"
        );
        String family = text(
                document,
                "/hl7:ClinicalDocument/hl7:legalAuthenticator/hl7:assignedEntity/hl7:assignedPerson/hl7:name/hl7:family"
        );
        String identifier = attr(
                document,
                "/hl7:ClinicalDocument/hl7:legalAuthenticator/hl7:assignedEntity/hl7:id",
                "extension"
        );
        return joinNonBlank(" - ", joinNonBlank(" ", prefix, given, family), identifier);
    }

    private String formatEffectiveTime(Document document) throws Exception {
        return firstNonBlank(
                attr(document, "/hl7:ClinicalDocument/hl7:effectiveTime", "value"),
                "N/D"
        );
    }

    private String text(Document document, String expression) throws Exception {
        return normalizeWhitespace((String) xpath().evaluate(expression, document, XPathConstants.STRING));
    }

    private String text(Node node, String expression) throws Exception {
        return normalizeWhitespace((String) xpath().evaluate(expression, node, XPathConstants.STRING));
    }

    private String attr(Document document, String expression, String attributeName) throws Exception {
        Node node = (Node) xpath().evaluate(expression, document, XPathConstants.NODE);
        if (node == null || node.getAttributes() == null || node.getAttributes().getNamedItem(attributeName) == null) {
            return "";
        }
        return normalizeWhitespace(node.getAttributes().getNamedItem(attributeName).getNodeValue());
    }

    private NodeList nodes(Document document, String expression) throws Exception {
        return (NodeList) xpath().evaluate(expression, document, XPathConstants.NODESET);
    }

    private XPath xpath() {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new Hl7NamespaceContext());
        return xpath;
    }

    private String normalizeWhitespace(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String joinNonBlank(String separator, String... values) {
        List<String> filtered = new ArrayList<>();
        for (String value : values) {
            if (!isBlank(value)) {
                filtered.add(value.trim());
            }
        }
        return String.join(separator, filtered);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record PdfSection(String title, String content) {
    }

    private record PdfFonts(PDFont regular, PDFont bold) {
    }

    private static final class Hl7NamespaceContext implements NamespaceContext {

        @Override
        public String getNamespaceURI(String prefix) {
            if ("hl7".equals(prefix)) {
                return "urn:hl7-org:v3";
            }
            if ("sdtc".equals(prefix)) {
                return "urn:hl7-org:sdtc";
            }
            if ("xsi".equals(prefix)) {
                return XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
            }
            return XMLConstants.NULL_NS_URI;
        }

        @Override
        public String getPrefix(String namespaceURI) {
            return null;
        }

        @Override
        public Iterator<String> getPrefixes(String namespaceURI) {
            return Collections.emptyIterator();
        }
    }

    private static final class PdfCanvas implements AutoCloseable {

        private final PDDocument document;
        private final PdfFonts fonts;
        private PDPage currentPage;
        private PDPageContentStream contentStream;
        private float y;

        private PdfCanvas(PDDocument document, PdfFonts fonts) throws IOException {
            this.document = document;
            this.fonts = fonts;
            startNewPage();
        }

        private void writeSectionTitle(String text) throws IOException {
            addVerticalSpace(SECTION_SPACING);
            writeLine(text, fonts.bold(), HEADING_FONT_SIZE);
            addVerticalSpace(4f);
        }

        private void writeKeyValue(String label, String value) throws IOException {
            String safeValue = isBlank(value) ? "N/D" : value;
            writeParagraph(label + ": " + safeValue, fonts.regular(), BODY_FONT_SIZE);
        }

        private void writeParagraph(String text, PDFont font, float fontSize) throws IOException {
            String safeText = isBlank(text) ? "-" : text;
            List<String> lines = wrapText(safeText, font, fontSize, CONTENT_WIDTH);

            for (String line : lines) {
                ensureSpace(LINE_HEIGHT);
                writeRawLine(line, font, fontSize);
            }

            addVerticalSpace(PARAGRAPH_SPACING);
        }

        private void writeLine(String text, PDFont font, float fontSize) throws IOException {
            ensureSpace(LINE_HEIGHT);
            writeRawLine(text, font, fontSize);
        }

        private void writeRawLine(String text, PDFont font, float fontSize) throws IOException {
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(MARGIN, y);
            contentStream.showText(sanitize(text));
            contentStream.endText();
            y -= LINE_HEIGHT;
        }

        private void addVerticalSpace(float amount) throws IOException {
            ensureSpace(amount);
            y -= amount;
        }

        private void ensureSpace(float requiredHeight) throws IOException {
            if (y - requiredHeight < MARGIN) {
                startNewPage();
            }
        }

        private void startNewPage() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }

            currentPage = new PDPage(PDRectangle.A4);
            document.addPage(currentPage);
            contentStream = new PDPageContentStream(document, currentPage);
            y = PAGE_HEIGHT - MARGIN;
        }

        private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            String[] words = sanitize(text).split(" ");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                if (word.isBlank()) {
                    continue;
                }

                String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
                float candidateWidth = font.getStringWidth(candidate) / 1000f * fontSize;

                if (candidateWidth <= maxWidth) {
                    currentLine = new StringBuilder(candidate);
                } else {
                    if (!currentLine.isEmpty()) {
                        lines.add(currentLine.toString());
                    }
                    currentLine = new StringBuilder(word);
                }
            }

            if (!currentLine.isEmpty()) {
                lines.add(currentLine.toString());
            }

            if (lines.isEmpty()) {
                lines.add("-");
            }

            return lines;
        }

        private String sanitize(String value) {
            if (value == null) {
                return "";
            }
            return value.replace('\u00A0', ' ')
                    .replace('\u2013', '-')
                    .replace('\u2014', '-')
                    .replace('\u2018', '\'')
                    .replace('\u2019', '\'')
                    .replace('\u201C', '"')
                    .replace('\u201D', '"');
        }

        private boolean isBlank(String value) {
            return value == null || value.isBlank();
        }

        @Override
        public void close() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
        }
    }
}