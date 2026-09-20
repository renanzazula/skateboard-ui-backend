package com.skateboard.uibackend.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Propagates the current request's correlation id (set in MDC by {@link
 * CorrelationIdFilter}) onto outgoing downstream calls, and leaves the
 * request untouched when nothing is in MDC (e.g. a call made off the
 * request-handling thread).
 */
class CorrelationIdExchangeFilterTest {

    private final CorrelationIdExchangeFilter filter = new CorrelationIdExchangeFilter();

    @AfterEach
    void clearMdc() {
        MDC.remove(CorrelationIdFilter.MDC_KEY);
    }

    @Test
    void addsTheCorrelationIdHeaderWhenOneIsInMdc() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "abc-123");

        ClientRequest request = ClientRequest.create(HttpMethod.GET, URI.create("http://downstream/x")).build();
        ClientRequest[] captured = new ClientRequest[1];

        filter.filter(request, req -> {
            captured[0] = req;
            return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        }).block();

        assertThat(captured[0].headers().getFirst(CorrelationIdFilter.HEADER)).isEqualTo("abc-123");
    }

    @Test
    void passesTheRequestThroughUnmodifiedWithNoCorrelationIdInMdc() {
        MDC.remove(CorrelationIdFilter.MDC_KEY);

        ClientRequest request = ClientRequest.create(HttpMethod.GET, URI.create("http://downstream/x")).build();
        ClientRequest[] captured = new ClientRequest[1];

        filter.filter(request, req -> {
            captured[0] = req;
            return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        }).block();

        assertThat(captured[0]).isSameAs(request);
        assertThat(captured[0].headers().getFirst(CorrelationIdFilter.HEADER)).isNull();
    }
}
