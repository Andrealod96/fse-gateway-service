package it.thcs.fse.fsersaservice.infrastructure.gateway.jwt;

public record GatewayJwtHeaders(
        String authorizationBearerToken,
        String fseJwtSignatureToken
) {
}