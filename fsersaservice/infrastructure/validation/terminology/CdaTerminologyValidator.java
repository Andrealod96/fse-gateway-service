package it.thcs.fse.fsersaservice.infrastructure.validation.terminology;

import it.thcs.fse.fsersaservice.config.properties.TerminologyValidationProperties;
import it.thcs.fse.fsersaservice.domain.exception.TerminologyValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class CdaTerminologyValidator {

    private static final Logger log = LoggerFactory.getLogger(CdaTerminologyValidator.class);

    private static final Set<String> OIDS_STRICT = Set.of(
            "2.16.840.1.113883.6.1",
            "2.16.840.1.113883.6.103",
            "2.16.840.1.113883.6.73",
            "2.16.840.1.113883.2.9.6.1.5",
            "2.16.840.1.113883.5.1",
            "2.16.840.1.113883.5.4",
            "2.16.840.1.113883.5.7",
            "2.16.840.1.113883.5.25",
            "2.16.840.1.113883.5.1063",
            "2.16.840.1.113883.5.111"
    );

    private final TerminologyValidationProperties properties;
    private final TerminologyRegistry terminologyRegistry;

    public CdaTerminologyValidator(
            TerminologyValidationProperties properties,
            TerminologyRegistry terminologyRegistry
    ) {
        this.properties = properties;
        this.terminologyRegistry = terminologyRegistry;
    }

    public void validate(String xmlContent) {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            Document document = parse(xmlContent);

            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            NodeList codedNodes = document.getElementsByTagNameNS("*", "*");

            for (int i = 0; i < codedNodes.getLength(); i++) {
                org.w3c.dom.Node node = codedNodes.item(i);
                if (!(node instanceof org.w3c.dom.Element element)) {
                    continue;
                }

                if (!element.hasAttribute("codeSystem") || !element.hasAttribute("code")) {
                    continue;
                }

                String codeSystem = element.getAttribute("codeSystem").trim();
                String code = element.getAttribute("code").trim();
                String localName = element.getLocalName();

                if (codeSystem.isBlank() || code.isBlank()) {
                    continue;
                }

                if (!terminologyRegistry.hasOid(codeSystem)) {
                    String message = "Catalogo terminologico non disponibile per codeSystem=" + codeSystem
                            + " su elemento <" + localName + "> con code=" + code;

                    if (OIDS_STRICT.contains(codeSystem)) {
                        errors.add(message);
                    } else {
                        warnings.add(message);
                    }
                    continue;
                }

                if (!terminologyRegistry.containsCode(codeSystem, code)) {
                    String message = "Codice non trovato nel catalogo: elemento <" + localName + ">, codeSystem="
                            + codeSystem + ", code=" + code;

                    if (OIDS_STRICT.contains(codeSystem)) {
                        errors.add(message);
                    } else {
                        warnings.add(message);
                    }
                }
            }

            if (!warnings.isEmpty()) {
                log.warn("Validazione terminologica completata con warning: {}", warnings);
            }

            if (!errors.isEmpty()) {
                throw new TerminologyValidationException(
                        "Validazione terminologica CDA fallita",
                        errors,
                        warnings
                );
            }

        } catch (TerminologyValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Errore tecnico durante la validazione terminologica del CDA", ex);
            throw new IllegalStateException(
                    "Errore tecnico durante la validazione terminologica del CDA: " + ex.getMessage(),
                    ex
            );
        }
    }

    private Document parse(String xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
    }
}