package com.skateboard.uibackend.client.appconfig;

import com.skateboard.uibackend.client.appconfig.generated.api.CampaignApi;
import com.skateboard.uibackend.client.appconfig.generated.model.CampaignResponse;
import com.skateboard.uibackend.exception.DownstreamServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.when;

/**
 * The BFF's downstream-error mapping for campaigns — the one piece of
 * {@link CampaignClient} with real logic. Success is a pass-through; failures
 * become {@link DownstreamServiceException} carrying the status, an
 * {@code APP_CONFIG_*} code, and — unlike the other app-config clients —
 * app-config-be's own error message when it sent one.
 */
class CampaignClientTest {

    @Mock
    private CampaignApi campaignApi;

    private CampaignClient client;

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        client = new CampaignClient(campaignApi);
    }

    private static WebClientResponseException responseException(HttpStatus status, String body) {
        return WebClientResponseException.create(
                status, status.getReasonPhrase(), HttpHeaders.EMPTY,
                body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8, null);
    }

    @Test
    void passesASuccessfulResponseThrough() {
        CampaignResponse response = new CampaignResponse().id(id).name("Launch week");
        when(campaignApi.getCampaign(id)).thenReturn(Mono.just(response));

        assertThat(client.getCampaign(id)).isSameAs(response);
    }

    @Test
    void maps404ToNotFoundWithTheDownstreamMessage() {
        when(campaignApi.getCampaign(id)).thenReturn(Mono.error(responseException(
                HttpStatus.NOT_FOUND, "{\"status\":404,\"error\":\"Not Found\",\"message\":\"Campaign not found: " + id + "\"}")));

        DownstreamServiceException ex = catchThrowableOfType(
                () -> client.getCampaign(id), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getCode()).isEqualTo("APP_CONFIG_NOT_FOUND");
        assertThat(ex.getMessage()).isEqualTo("Campaign not found: " + id);
    }

    @Test
    void maps409ToConflictWithTheDomainInvariantMessage() {
        when(campaignApi.publishCampaign(id)).thenReturn(Mono.error(responseException(
                HttpStatus.CONFLICT,
                "{\"message\":\"Screen 1 needs a background image or a fallback colour before publishing.\"}")));

        DownstreamServiceException ex = catchThrowableOfType(
                () -> client.publishCampaign(id), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getCode()).isEqualTo("APP_CONFIG_CONFLICT");
        assertThat(ex.getMessage())
                .isEqualTo("Screen 1 needs a background image or a fallback colour before publishing.");
    }

    @Test
    void fallsBackToTheCannedMessageWhenTheBodyIsNotParseable() {
        when(campaignApi.createCampaign(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Mono.error(responseException(HttpStatus.BAD_REQUEST, "<html>gateway error</html>")));

        DownstreamServiceException ex = catchThrowableOfType(
                () -> client.createCampaign(null), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getCode()).isEqualTo("APP_CONFIG_BAD_REQUEST");
        assertThat(ex.getMessage()).contains("could not be saved");
    }

    @Test
    void maps5xxToServiceUnavailable() {
        when(campaignApi.getCampaign(id)).thenReturn(Mono.error(responseException(
                HttpStatus.INTERNAL_SERVER_ERROR, "{\"message\":\"boom\"}")));

        DownstreamServiceException ex = catchThrowableOfType(
                () -> client.getCampaign(id), DownstreamServiceException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(ex.getCode()).isEqualTo("APP_CONFIG_SERVICE_UNAVAILABLE");
    }

    @Test
    void mapsAConnectivityFailureToServiceUnavailable() {
        when(campaignApi.listCampaigns()).thenReturn(reactor.core.publisher.Flux.error(
                new WebClientRequestException(new RuntimeException("connection refused"),
                        org.springframework.http.HttpMethod.GET, URI.create("http://app-config-be/api/campaigns/admin"),
                        HttpHeaders.EMPTY)));

        assertThatThrownBy(() -> client.listCampaigns())
                .isInstanceOf(DownstreamServiceException.class)
                .satisfies(t -> assertThat(((DownstreamServiceException) t).getStatus())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
