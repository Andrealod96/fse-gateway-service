package it.thcs.fse.fsersaservice.infrastructure.validation.terminology;

import it.thcs.fse.fsersaservice.config.properties.TerminologyValidationProperties;
import it.thcs.fse.fsersaservice.infrastructure.validation.terminology.model.TerminologyConcept;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TerminologyRegistry {

    private static final Logger log = LoggerFactory.getLogger(TerminologyRegistry.class);

    private final TerminologyValidationProperties properties;
    private final Map<String, Map<String, TerminologyConcept>> cache = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    public TerminologyRegistry(TerminologyValidationProperties properties) {
        this.properties = properties;
    }

    public boolean containsCode(String oid, String code) {
        ensureInitialized();
        Map<String, TerminologyConcept> concepts = cache.get(oid);
        return concepts != null && concepts.containsKey(normalize(code));
    }

    public Optional<TerminologyConcept> findConcept(String oid, String code) {
        ensureInitialized();
        Map<String, TerminologyConcept> concepts = cache.get(oid);
        if (concepts == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(concepts.get(normalize(code)));
    }

    public boolean hasOid(String oid) {
        ensureInitialized();
        return cache.containsKey(oid);
    }

    public Set<String> loadedOids() {
        ensureInitialized();
        return Collections.unmodifiableSet(cache.keySet());
    }

    private void ensureInitialized() {
        if (initialized) {
            return;
        }

        synchronized (this) {
            if (initialized) {
                return;
            }

            try {
                String base = properties.getCatalogsBasePath();
                String normalizedBase = base.startsWith("classpath:") ? base.substring("classpath:".length()) : base;
                if (normalizedBase.startsWith("/")) {
                    normalizedBase = normalizedBase.substring(1);
                }

                PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                Resource[] resources = resolver.getResources("classpath*:" + normalizedBase + "/*.csv");

                if (resources.length == 0) {
                    throw new IllegalStateException("Nessun catalogo terminologico trovato in " + properties.getCatalogsBasePath());
                }

                for (Resource resource : resources) {
                    String filename = resource.getFilename();
                    if (filename == null || !filename.endsWith(".csv")) {
                        continue;
                    }

                    String oid = filename.substring(0, filename.length() - 4);
                    if ("terminology".equalsIgnoreCase(oid)) {
                        continue;
                    }

                    Map<String, TerminologyConcept> concepts = loadCsv(resource, oid);
                    if (!concepts.isEmpty()) {
                        cache.put(oid, concepts);
                        log.info("Catalogo {} caricato con {} concetti", oid, concepts.size());
                    } else {
                        log.warn("Catalogo {} trovato ma senza concetti leggibili", oid);
                    }
                }

                if (cache.isEmpty()) {
                    throw new IllegalStateException("Cataloghi terminologici caricati ma nessun OID utilizzabile trovato");
                }

                initialized = true;
                log.info("Cataloghi terminologici caricati: {} OID", cache.size());

            } catch (Exception ex) {
                throw new IllegalStateException("Errore nel caricamento dei cataloghi terminologici", ex);
            }
        }
    }

    private Map<String, TerminologyConcept> loadCsv(Resource resource, String oid) {
        Map<String, TerminologyConcept> concepts = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            List<String> lines = reader.lines()
                    .map(this::stripBom)
                    .filter(line -> line != null && !line.isBlank())
                    .toList();

            if (lines.isEmpty()) {
                return concepts;
            }

            char delimiter = detectDelimiter(lines.get(0));
            List<String> header = split(lineOrEmpty(lines, 0), delimiter);

            int codeIndex = findColumnIndex(header,
                    "code", "codice", "conceptcode", "value", "valore");
            int displayIndex = findColumnIndex(header,
                    "display", "displayname", "descrizione", "description", "term", "label");

            boolean hasHeader = codeIndex >= 0 || displayIndex >= 0;
            int startRow = hasHeader ? 1 : 0;

            for (int i = startRow; i < lines.size(); i++) {
                String line = lines.get(i);
                List<String> cols = split(line, delimiter);
                if (cols.isEmpty()) {
                    continue;
                }

                String code;
                String display = null;

                if (hasHeader) {
                    code = getColumn(cols, codeIndex);
                    display = getColumn(cols, displayIndex);
                } else {
                    code = firstNonBlank(cols, 0, 1);
                    display = firstNonBlank(cols, 2, 3, 4);
                }

                if (code == null || code.isBlank()) {
                    continue;
                }

                String normalizedCode = normalize(code);
                concepts.put(
                        normalizedCode,
                        new TerminologyConcept(oid, normalizedCode, display != null ? display.trim() : null, line)
                );
            }

            return concepts;

        } catch (Exception ex) {
            throw new IllegalStateException("Errore nella lettura del catalogo " + oid, ex);
        }
    }

    private String lineOrEmpty(List<String> lines, int index) {
        return index >= 0 && index < lines.size() ? lines.get(index) : "";
    }

    private char detectDelimiter(String headerLine) {
        Map<Character, Integer> counts = new LinkedHashMap<>();
        counts.put(';', countOccurrences(headerLine, ';'));
        counts.put(',', countOccurrences(headerLine, ','));
        counts.put('\t', countOccurrences(headerLine, '\t'));
        counts.put('|', countOccurrences(headerLine, '|'));

        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .filter(e -> e.getValue() > 0)
                .map(Map.Entry::getKey)
                .orElse(';');
    }

    private int countOccurrences(String line, char ch) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }

    private List<String> split(String line, char delimiter) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }

            if (ch == delimiter && !inQuotes) {
                result.add(cleanCell(current.toString()));
                current.setLength(0);
                continue;
            }

            current.append(ch);
        }

        result.add(cleanCell(current.toString()));
        return result;
    }

    private String cleanCell(String value) {
        return value == null ? null : stripBom(value).trim();
    }

    private int findColumnIndex(List<String> header, String... aliases) {
        if (header == null || header.isEmpty()) {
            return -1;
        }

        List<String> normalizedAliases = Arrays.stream(aliases)
                .map(this::normalizeHeader)
                .toList();

        for (int i = 0; i < header.size(); i++) {
            String current = normalizeHeader(header.get(i));
            if (normalizedAliases.contains(current)) {
                return i;
            }
        }

        return -1;
    }

    private String normalizeHeader(String value) {
        return stripBom(value)
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }

    private String getColumn(List<String> cols, int index) {
        if (index < 0 || index >= cols.size()) {
            return null;
        }
        String value = cols.get(index);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstNonBlank(List<String> columns, int... indexes) {
        for (int idx : indexes) {
            if (idx >= 0 && idx < columns.size()) {
                String value = columns.get(idx);
                if (value != null && !value.isBlank()) {
                    return value.trim();
                }
            }
        }
        return null;
    }

    private String stripBom(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\uFEFF", "");
    }

    private String normalize(String value) {
        return value == null ? null : stripBom(value).trim();
    }
}