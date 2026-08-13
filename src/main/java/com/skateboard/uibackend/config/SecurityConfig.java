package com.skateboard.uibackend.config;

import com.skateboard.uibackend.web.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth2 resource server validating the same Keycloak-issued access tokens
 * skateboard-podcast-be accepts (RS256, signature + issuer + expiry). This
 * BFF relays the inbound token unmodified to downstream services (see
 * {@code web.BearerTokenExchangeFilter}), and those services validate their
 * own required audience — there is no separate Keycloak client/audience
 * configured for the BFF itself, so no {@code AudienceValidator} is applied
 * here. Authorities are read verbatim (no ROLE_/SCOPE_ prefix) from the
 * "authorities" claim, matching skateboard-podcast-be's realm-role mapper, so
 * {@code @PreAuthorize("hasAuthority('FUNC_...')")} on the controllers works
 * against the same permission strings used downstream.
 * <p>
 * The JWKS URI is built directly from {@code issuerUri} (Keycloak's stable
 * {@code /protocol/openid-connect/certs} convention) instead of doing OIDC
 * discovery ({@code JwtDecoders.fromIssuerLocation}), for the same reason
 * skateboard-podcast-be does: discovery blocks on Keycloak being reachable at
 * bean-construction time, whereas building the URI directly keeps key
 * fetching lazy (first token verification).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final String issuerUri;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
                           RestAuthenticationEntryPoint authenticationEntryPoint) {
        this.issuerUri = issuerUri;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(authenticationEntryPoint))
                .build();
    }

    private JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(issuerUri + "/protocol/openid-connect/certs").build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("authorities");
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
