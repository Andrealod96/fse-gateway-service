package it.thcs.fse.fsersaservice.infrastructure.gateway.jwt;

public record GatewayStatusJwtContext(
        String subjectFiscalCode,
        String patientFiscalCode,
        String subjectRole,
        String purposeOfUse,
        String subjectOrganization,
        String subjectOrganizationId,
        String subjectApplicationId,
        String subjectApplicationVendor,
        String subjectApplicationVersion,
        String locality,
        Boolean patientConsent,
        String actionId,
        String resourceHl7Type,
        Boolean useSubjectAsAuthor
) {
}