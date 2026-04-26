package it.thcs.fse.fsersaservice.infrastructure.validation.xsd;

import it.thcs.fse.fsersaservice.config.properties.XsdValidationProperties;
import it.thcs.fse.fsersaservice.domain.exception.CdaValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class CdaXsdValidator {

    private static final Logger log = LoggerFactory.getLogger(CdaXsdValidator.class);

    private final XsdValidationProperties xsdValidationProperties;
    private final ResourceLoader resourceLoader;

    public CdaXsdValidator(
            XsdValidationProperties xsdValidationProperties,
            ResourceLoader resourceLoader
    ) {
        this.xsdValidationProperties = xsdValidationProperties;
        this.resourceLoader = resourceLoader;
    }

    public void validate(String xmlContent) {
        if (!xsdValidationProperties.isEnabled()) {
            return;
        }

        List<String> errors = new ArrayList<>();

        try {
            Resource schemaResource = resourceLoader.getResource(xsdValidationProperties.getSchemaPath());
            if (!schemaResource.exists()) {
                throw new IllegalStateException("Schema XSD non trovato: " + xsdValidationProperties.getSchemaPath());
            }

            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            Schema schema = schemaFactory.newSchema(schemaResource.getURL());

            Validator validator = schema.newValidator();
            validator.setErrorHandler(new CollectingErrorHandler(errors));
            validator.validate(new StreamSource(new StringReader(xmlContent)));

            if (!errors.isEmpty()) {
                throw new CdaValidationException("Validazione XSD CDA fallita", List.copyOf(errors));
            }

        } catch (CdaValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Errore tecnico durante la validazione XSD del CDA", ex);
            throw new IllegalStateException(
                    "Errore tecnico durante la validazione XSD del CDA: " + ex.getMessage(),
                    ex
            );
        }
    }

    private static final class CollectingErrorHandler implements ErrorHandler {

        private final List<String> errors;

        private CollectingErrorHandler(List<String> errors) {
            this.errors = errors;
        }

        @Override
        public void warning(SAXParseException exception) {
            errors.add(format("WARNING", exception));
        }

        @Override
        public void error(SAXParseException exception) {
            errors.add(format("ERROR", exception));
        }

        @Override
        public void fatalError(SAXParseException exception) {
            errors.add(format("FATAL", exception));
        }

        private String format(String level, SAXParseException ex) {
            return level
                    + " [line=" + ex.getLineNumber()
                    + ", column=" + ex.getColumnNumber()
                    + "] " + ex.getMessage();
        }
    }
}