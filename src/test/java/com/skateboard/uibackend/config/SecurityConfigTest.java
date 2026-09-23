package com.skateboard.uibackend.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The BFF's token validators.
 *
 * <p>Regression cover for a decoder built with a bare
 * {@code NimbusJwtDecoder.withJwkSetUri(...).build()}, which validates
 * timestamps and signature but <em>not</em> the issuer. That gap only shows
 * itself when two Keycloak deployments share one database: they serve the same
 * realm signing keys under different hostnames, so a token from the wrong host
 * verifies here, gets relayed, and is rejected by skateboard-podcast-be /
 * skateboard-user-be / skateboard-app-config-be on their own issuer check —
 * turning a hostname mismatch into a 401 from a data route instead of a clean
 * authentication failure at the edge.
 *
 * <p>Validators are exercised directly against hand-built {@link Jwt} objects
 * rather than through the decoder, so no signing key or live JWKS endpoint is
 * involved — same shape as skateboard-podcast-be's {@code AudienceValidatorTest}.
 */
class SecurityConfigTest {

    private static final String ISSUER = "https://skateboard-keycloak-production.up.railway.app/realms/skateboard-podcast";

    private final OAuth2TokenValidator<Jwt> validator = SecurityConfig.jwtValidator(ISSUER);

    @Test
    void acceptsATokenFromTheConfiguredIssuer() {
        OAuth2TokenValidatorResult result = validator.validate(token(ISSUER, Instant.now().plus(15, ChronoUnit.MINUTES)));

        assertThat(result.hasErrors()).isFalse();
    }

    /**
     * The retired Keycloak service. Same realm, same database, same signing
     * keys — so the signature is genuinely valid and only "iss" separates it
     * from a token this deployment should accept.
     */
    @Test
    void rejectsATokenFromAnotherKeycloakHostServingTheSameRealm() {
        Jwt jwt = token(
                "https://keycloak-production-2f7a.up.railway.app/realms/skateboard-podcast",
                Instant.now().plus(15, ChronoUnit.MINUTES));

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
                .extracting("errorCode")
                .containsOnly("invalid_token");
    }

    @Test
    void rejectsATokenFromADifferentRealmOnTheSameHost() {
        Jwt jwt = token(
                "https://skateboard-keycloak-production.up.railway.app/realms/some-other-realm",
                Instant.now().plus(15, ChronoUnit.MINUTES));

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void rejectsATokenWithNoIssuerClaim() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "a-user")
                .issuedAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build();

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    /**
     * The default timestamp validation must survive adding the issuer check —
     * {@code createDefaultWithIssuer} is expiry <em>and</em> issuer, not a
     * replacement for the former.
     */
    @Test
    void stillRejectsAnExpiredTokenFromTheCorrectIssuer() {
        Jwt jwt = token(ISSUER, Instant.now().minus(1, ChronoUnit.MINUTES));

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    private static Jwt token(String issuer, Instant expiresAt) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("iss", issuer)
                .claim("sub", "a-user")
                .issuedAt(expiresAt.minus(15, ChronoUnit.MINUTES))
                .expiresAt(expiresAt)
                .build();
    }
}
