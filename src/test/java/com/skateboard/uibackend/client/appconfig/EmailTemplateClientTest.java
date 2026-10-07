package com.skateboard.uibackend.client.appconfig;

import com.skateboard.uibackend.client.appconfig.generated.api.EmailTemplatesApi;
import com.skateboard.uibackend.client.appconfig.generated.model.EmailTemplateResponse;
import com.skateboard.uibackend.client.appconfig.generated.model.UpdateEmailTemplateRequest;
import com.skateboard.uibackend.exception.DownstreamServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.when;

class EmailTemplateClientTest {

    @Mock
    private EmailTemplatesApi emailTemplatesApi;

    private EmailTemplateClient client;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        client = new EmailTemplateClient(emailTemplatesApi);
    }

    private static WebClientResponseException responseException(HttpStatus status) {
        return WebClientResponseException.create(status, status.getReasonPhrase(), HttpHeaders.EMPTY,
                new byte[0], StandardCharsets.UTF_8, null);
    }

    @Test
    void listPassesThrough() {
        EmailTemplateResponse response = new EmailTemplateResponse().subject("Hi");
        when(emailTemplatesApi.listEmailTemplates()).thenReturn(Flux.just(response));

        assertThat(client.list()).containsExactly(response);
    }

    @Test
    void getPassesThrough() {
        EmailTemplateResponse response = new EmailTemplateResponse().subject("Hi");
        when(emailTemplatesApi.getEmailTemplate("GUEST_APPLICATION_RECEIVED", "en"))
                .thenReturn(Mono.just(response));

        assertThat(client.get("GUEST_APPLICATION_RECEIVED", "en")).isSameAs(response);
    }

    @Test
    void updatePassesThrough() {
        UUID id = UUID.randomUUID();
        UpdateEmailTemplateRequest request = new UpdateEmailTemplateRequest();
        EmailTemplateResponse response = new EmailTemplateResponse().subject("Hi");
        when(emailTemplatesApi.updateEmailTemplate(id, request)).thenReturn(Mono.just(response));

        assertThat(client.update(id, request)).isSameAs(response);
    }

    @Test
    void mapsANotFoundOnUpdateToADedicatedMessage() {
        UUID id = UUID.randomUUID();
        UpdateEmailTemplateRequest request = new UpdateEmailTemplateRequest();
        when(emailTemplatesApi.updateEmailTemplate(id, request))
                .thenReturn(Mono.error(responseException(HttpStatus.NOT_FOUND)));

        DownstreamServiceException ex =
                catchThrowableOfType(() -> client.update(id, request), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getCode()).isEqualTo("APP_CONFIG_NOT_FOUND");
    }

    @Test
    void mapsABadRequestOnUpdateToADedicatedMessage() {
        UUID id = UUID.randomUUID();
        UpdateEmailTemplateRequest request = new UpdateEmailTemplateRequest();
        when(emailTemplatesApi.updateEmailTemplate(id, request))
                .thenReturn(Mono.error(responseException(HttpStatus.BAD_REQUEST)));

        DownstreamServiceException ex =
                catchThrowableOfType(() -> client.update(id, request), DownstreamServiceException.class);

        assertThat(ex.getCode()).isEqualTo("APP_CONFIG_BAD_REQUEST");
    }

    @Test
    void mapsA5xxToServiceUnavailable() {
        when(emailTemplatesApi.listEmailTemplates())
                .thenReturn(Flux.error(responseException(HttpStatus.INTERNAL_SERVER_ERROR)));

        DownstreamServiceException ex = catchThrowableOfType(client::list, DownstreamServiceException.class);

        assertThat(ex.getCode()).isEqualTo("APP_CONFIG_SERVICE_UNAVAILABLE");
    }

    @Test
    void mapsAConnectivityFailureToServiceUnavailable() {
        when(emailTemplatesApi.listEmailTemplates())
                .thenReturn(Flux.error(new WebClientRequestException(
                        new RuntimeException("connection refused"), HttpMethod.GET,
                        URI.create("http://app-config-be/api/email-templates"), HttpHeaders.EMPTY)));

        DownstreamServiceException ex = catchThrowableOfType(client::list, DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
