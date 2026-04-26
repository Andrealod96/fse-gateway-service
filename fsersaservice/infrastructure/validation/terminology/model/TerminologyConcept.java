package it.thcs.fse.fsersaservice.infrastructure.validation.terminology.model;

public record TerminologyConcept(
        String oid,
        String code,
        String display,
        String rawLine
) {
}