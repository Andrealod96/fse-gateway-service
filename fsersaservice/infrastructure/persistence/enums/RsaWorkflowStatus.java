package it.thcs.fse.fsersaservice.infrastructure.persistence.enums;

/**
 * Stati del workflow di ingestion di un documento RSA.
 *
 * I primi 7 stati coprono il pipeline documentale locale (generazione, validazione, firma).
 * Gli ultimi 4 stati coprono l'interazione con il Gateway FSE 2.0.
 * FAILED è lo stato terminale di errore in qualsiasi fase.
 */
public enum RsaWorkflowStatus {

    // --- Pipeline documentale locale ---

    /** Richiesta ricevuta e registrata. */
    RECEIVED,

    /** CDA2 XML generato correttamente. */
    CDA_GENERATED,

    /** CDA2 validato (XSD + Schematron RSA + Terminologia). */
    CDA_VALIDATED,

    /** PDF base renderizzato dal CDA2. */
    PDF_RENDERED,

    /** PDF convertito in PDF/A-3. */
    PDFA_CREATED,

    /** CDA2 embedded nel PDF/A-3 come allegato. */
    CDA_EMBEDDED,

    /** PDF/A-3 firmato con PAdES baseline-B. */
    SIGNED,

    // --- Interazione con Gateway FSE 2.0 ---

    /** Richiesta di validazione inviata al Gateway. */
    GATEWAY_SUBMITTED,

    /** Gateway ha restituito esito di validazione positivo (201 VALIDATION o 200 VERIFICA). */
    GATEWAY_VALIDATED,

    /** Gateway ha restituito esito di validazione negativo (4xx). */
    GATEWAY_REJECTED,

    /** Documento pubblicato sul FSE (esito pubblicazione positivo). */
    PUBLISHED,

    // --- Errore ---

    /** Errore in qualsiasi fase del pipeline. Vedere error_message per dettagli. */
    FAILED
}