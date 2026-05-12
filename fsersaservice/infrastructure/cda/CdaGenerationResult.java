package it.thcs.fse.fsersaservice.infrastructure.cda;

/**
 * Risultato della generazione del CDA2.
 *
 * Trasporta sia l'XML generato che gli identificativi del documento,
 * necessari per costruire l'identificativoDoc da passare al Gateway
 * nel flusso validate-and-create.
 *
 * @param cdaXml             il documento CDA2 in formato XML
 * @param documentIdRoot     il root OID del documento (es. 2.16.840.1.113883.2.9.2.160.4.4)
 * @param documentIdExtension l'extension generata (es. RSSMRA80A01H501U.20240115090000.A1B2C3D4)
 */
public record CdaGenerationResult(
        String cdaXml,
        String documentIdRoot,
        String documentIdExtension
) {
    /**
     * Restituisce l'identificativo documento nel formato XDS atteso dal Gateway:
     * root^extension
     */
    public String identificativoDocXds() {
        return documentIdRoot + "^" + documentIdExtension;
    }
}