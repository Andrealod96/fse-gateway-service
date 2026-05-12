package it.thcs.fse.fsersaservice.infrastructure.gateway.jwt;

public enum JwtNegativeTestMode {

    /**
     * Nessuna alterazione del JWT.
     * Modalità normale.
     */
    NONE,

    /**
     * Caso test accreditamento:
     * VALIDAZIONE_TOKEN_JWT_RSA_KO
     *
     * Il claim purpose_of_use viene omesso dal FSE-JWT-Signature.
     */
    MISSING_PURPOSE_OF_USE,

    /**
     * Caso test accreditamento:
     * VALIDAZIONE_TOKEN_JWT_CAMPO_RSA_KO
     *
     * Il claim action_id viene valorizzato con TEST.
     */
    INVALID_ACTION_ID
}