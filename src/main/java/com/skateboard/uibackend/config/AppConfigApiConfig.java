package com.skateboard.uibackend.config;

import com.skateboard.uibackend.client.appconfig.generated.api.AboutUsApi;
import com.skateboard.uibackend.client.appconfig.generated.api.AdminApi;
import com.skateboard.uibackend.client.appconfig.generated.api.HomeApi;
import com.skateboard.uibackend.client.appconfig.generated.api.HomeFeaturedPlayerApi;
import com.skateboard.uibackend.client.appconfig.generated.api.PublicApi;
import com.skateboard.uibackend.client.appconfig.generated.invoker.ApiClient;
import com.skateboard.uibackend.web.BearerTokenExchangeFilter;
import com.skateboard.uibackend.web.CorrelationIdExchangeFilter;
import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Wires the openapi-generator-produced {@link PublicApi}/{@link AdminApi}
 * (generated from api/app-config-openapi.yaml's "public"/"admin" tags) to a
 * WebClient configured with skateboard-app-config-be's base URL/timeouts and
 * the two cross-cutting filters (bearer token relay, correlation id
 * propagation). Mirrors {@link PodcastApiConfig} — see its javadoc for why
 * {@code webClientBuilder} (not a raw {@code WebClient.builder()}) and a
 * separately-set {@link ApiClient#setBasePath} are both required.
 */
@Configuration
public class AppConfigApiConfig {

    @Bean
    public ApiClient appConfigApiClient(WebClient.Builder webClientBuilder,
                                         ClientsProperties clientsProperties,
                                         BearerTokenExchangeFilter bearerTokenExchangeFilter,
                                         CorrelationIdExchangeFilter correlationIdExchangeFilter) {
        ClientsProperties.AppConfig config = clientsProperties.getAppConfig();

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeoutMs())
                .responseTimeout(Duration.ofMillis(config.getReadTimeoutMs()));

        WebClient webClient = webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(bearerTokenExchangeFilter)
                .filter(correlationIdExchangeFilter)
                .build();

        ApiClient apiClient = new ApiClient(webClient);
        apiClient.setBasePath(config.getBaseUrl());
        return apiClient;
    }

    @Bean
    public PublicApi publicConfigApi(ApiClient appConfigApiClient) {
        return new PublicApi(appConfigApiClient);
    }

    @Bean
    public AdminApi brandingAdminApi(ApiClient appConfigApiClient) {
        return new AdminApi(appConfigApiClient);
    }

    @Bean
    public HomeApi homeVideoCategoryConfigApi(ApiClient appConfigApiClient) {
        return new HomeApi(appConfigApiClient);
    }

    @Bean
    public HomeFeaturedPlayerApi homeFeaturedPlayerApi(ApiClient appConfigApiClient) {
        return new HomeFeaturedPlayerApi(appConfigApiClient);
    }

    @Bean
    public AboutUsApi aboutUsApi(ApiClient appConfigApiClient) {
        return new AboutUsApi(appConfigApiClient);
    }
}
