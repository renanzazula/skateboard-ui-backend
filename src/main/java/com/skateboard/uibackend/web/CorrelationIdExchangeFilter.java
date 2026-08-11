package com.skateboard.uibackend.web;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * Adds the current request's correlation id (set in MDC by
 * {@link CorrelationIdFilter}) to outgoing downstream calls. Safe to read
 * from MDC here because client wrappers (e.g. PodcastClient) block on the
 * same thread that's handling the inbound request.
 */
@Component
public class CorrelationIdExchangeFilter implements ExchangeFilterFunction {

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId == null) {
            return next.exchange(request);
        }
        ClientRequest withHeader = ClientRequest.from(request)
                .header(CorrelationIdFilter.HEADER, correlationId)
                .build();
        return next.exchange(withHeader);
    }
}
