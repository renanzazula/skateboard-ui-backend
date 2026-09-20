package com.skateboard.uibackend.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Token relay: propagates the caller's own JWT (read off {@link
 * SecurityContextHolder}) as a bearer header on the outgoing downstream
 * request, and leaves the request untouched when there's no JWT on the
 * current thread (pre-auth routes, background work).
 */
class BearerTokenExchangeFilterTest {

    private final BearerTokenExchangeFilter filter = new BearerTokenExchangeFilter();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addsTheCallersBearerTokenWhenAuthenticatedWithAJwt() {
        Jwt jwt = Jwt.withTokenValue("the-token")
                .header("alg", "none")
                .claim("sub", "user-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        ClientRequest request = ClientRequest.create(org.springframework.http.HttpMethod.GET, URI.create("http://downstream/x")).build();
        ClientRequest[] captured = new ClientRequest[1];

        filter.filter(request, req -> {
            captured[0] = req;
            return Mono.just(ClientResponse.create(org.springframework.http.HttpStatus.OK).build());
        }).block();

        assertThat(captured[0].headers().getFirst("Authorization")).isEqualTo("Bearer the-token");
    }

    @Test
    void passesTheRequestThroughUnmodifiedWithNoAuthentication() {
        SecurityContextHolder.clearContext();

        ClientRequest request = ClientRequest.create(org.springframework.http.HttpMethod.GET, URI.create("http://downstream/x")).build();
        ClientRequest[] captured = new ClientRequest[1];

        filter.filter(request, req -> {
            captured[0] = req;
            return Mono.just(ClientResponse.create(org.springframework.http.HttpStatus.OK).build());
        }).block();

        assertThat(captured[0]).isSameAs(request);
        assertThat(captured[0].headers().getFirst("Authorization")).isNull();
    }

    @Test
    void passesTheRequestThroughUnmodifiedWhenAuthenticatedWithoutAJwt() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user", "credentials", List.of()));

        ClientRequest request = ClientRequest.create(org.springframework.http.HttpMethod.GET, URI.create("http://downstream/x")).build();
        ClientRequest[] captured = new ClientRequest[1];

        filter.filter(request, req -> {
            captured[0] = req;
            return Mono.just(ClientResponse.create(org.springframework.http.HttpStatus.OK).build());
        }).block();

        assertThat(captured[0]).isSameAs(request);
    }
}
