package com.skateboard.uibackend.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * Token relay: propagates the caller's own Keycloak JWT to downstream
 * services unmodified, so they can validate it (and its authorities)
 * independently — see SecurityConfig for why this BFF doesn't mint or
 * exchange tokens of its own. Reads the inbound token off
 * {@link SecurityContextHolder}'s ThreadLocal, which is only populated for
 * the thread handling the current HTTP request, so this relies on client
 * wrappers (e.g. PodcastClient) blocking on that same thread rather than
 * hopping to another one.
 */
@Component
public class BearerTokenExchangeFilter implements ExchangeFilterFunction {

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            return next.exchange(request);
        }

        Jwt jwt = jwtAuth.getToken();
        ClientRequest withAuth = ClientRequest.from(request)
                .headers(headers -> headers.setBearerAuth(jwt.getTokenValue()))
                .build();
        return next.exchange(withAuth);
    }
}
