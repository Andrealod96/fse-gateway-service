package it.thcs.fse.fsersaservice.infrastructure.validation.schematron;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.schematron.sch.SchematronResourceSCH;
import it.thcs.fse.fsersaservice.config.properties.SchematronValidationProperties;
import it.thcs.fse.fsersaservice.domain.exception.SchematronValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
public class CdaSchematronValidator {

    private static final Logger log = LoggerFactory.getLogger(CdaSchematronValidator.class);
    private static final String SVRL_NS = "http://purl.oclc.org/dsdl/svrl";

    private final SchematronValidationProperties schematronValidationProperties;

    private volatile SchematronResourceSCH cachedSchematron;

    public CdaSchematronValidator(SchematronValidationProperties schematronValidationProperties) {
        this.schematronValidationProperties = schematronValidationProperties;
    }

    public void validate(String xmlContent) {
        if (!schematronValidationProperties.isEnabled()) {
            return;
        }

        try {
            SchematronResourceSCH schematron = getOrCreateSchematron();

            Document svrlDocument = schematron.applySchematronValidation(
                    new StreamSource(new StringReader(xmlContent))
            );

            if (svrlDocument == null) {
                throw new IllegalStateException("Validazione Schematron terminata senza produrre SVRL");
            }

            ValidationOutcome outcome = extractOutcome(svrlDocument);

            if (!outcome.warnings().isEmpty()) {
                log.warn("Validazione Schematron completata con warning: {}", outcome.warnings());
            }

            if (!outcome.errors().isEmpty()) {
                outcome.errors().forEach(e -> log.error("SCHEMATRON ERROR: {}", e));
                throw new SchematronValidationException(
                        "Validazione Schematron RSA fallita",
                        outcome.errors(),
                        outcome.warnings()
                );
            }

        } catch (SchematronValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Errore tecnico durante la validazione Schematron del CDA", ex);
            throw new IllegalStateException(
                    "Errore tecnico durante la validazione Schematron del CDA: " + ex.getMessage(),
                    ex
            );
        }
    }

    private SchematronResourceSCH getOrCreateSchematron() {
        SchematronResourceSCH localRef = cachedSchematron;
        if (localRef != null) {
            return localRef;
        }

        synchronized (this) {
            if (cachedSchematron == null) {
                String classpathLocation = normalizeClasspathLocation(schematronValidationProperties.getRsaPath());

                SchematronResourceSCH resource = new SchematronResourceSCH(
                        new ClassPathResource(classpathLocation)
                );
                resource.setUseCache(true);

                if (!resource.isValidSchematron()) {
                    throw new IllegalStateException(
                            "Il file Schematron RSA non è valido: " + schematronValidationProperties.getRsaPath()
                    );
                }

                cachedSchematron = resource;
            }

            return cachedSchematron;
        }
    }

    private ValidationOutcome extractOutcome(Document svrlDocument) throws Exception {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new SvrlNamespaceContext());

        List<String> errors = extractMessages(
                xpath,
                svrlDocument,
                "//svrl:failed-assert"
        );

        List<String> warnings = extractMessages(
                xpath,
                svrlDocument,
                "//svrl:successful-report"
        );

        return new ValidationOutcome(errors, warnings);
    }

    private List<String> extractMessages(XPath xpath, Document document, String expression) throws Exception {
        NodeList nodes = (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);
        List<String> messages = new ArrayList<>();

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            String location = xpath.evaluate("@location", node);
            String test = xpath.evaluate("@test", node);
            String text = xpath.evaluate("normalize-space(svrl:text)", node);

            StringBuilder sb = new StringBuilder();

            if (text != null && !text.isBlank()) {
                sb.append(text.trim());
            } else {
                sb.append("Violazione Schematron senza messaggio esplicito");
            }

            if (location != null && !location.isBlank()) {
                sb.append(" | location=").append(location);
            }

            if (test != null && !test.isBlank()) {
                sb.append(" | test=").append(test);
            }

            messages.add(sb.toString());
        }

        return messages;
    }

    private String normalizeClasspathLocation(String location) {
        String normalized = location.trim();
        if (normalized.startsWith("classpath:")) {
            normalized = normalized.substring("classpath:".length());
        }
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private record ValidationOutcome(List<String> errors, List<String> warnings) {
    }

    private static final class SvrlNamespaceContext implements NamespaceContext {

        @Override
        public String getNamespaceURI(String prefix) {
            if ("svrl".equals(prefix)) {
                return SVRL_NS;
            }
            return XMLConstants.NULL_NS_URI;
        }

        @Override
        public String getPrefix(String namespaceURI) {
            if (SVRL_NS.equals(namespaceURI)) {
                return "svrl";
            }
            return null;
        }

        @Override
        public Iterator<String> getPrefixes(String namespaceURI) {
            return List.of(getPrefix(namespaceURI)).iterator();
        }
    }
}