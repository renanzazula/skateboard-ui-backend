package com.skateboard.uibackend.config;

import com.skateboard.uibackend.client.appconfig.generated.api.AboutUsApi;
import com.skateboard.uibackend.client.appconfig.generated.api.AdminApi;
import com.skateboard.uibackend.client.appconfig.generated.api.CampaignApi;
import com.skateboard.uibackend.client.appconfig.generated.api.HomeApi;
import com.skateboard.uibackend.client.appconfig.generated.api.HomeFeaturedPlayerApi;
import com.skateboard.uibackend.client.appconfig.generated.api.PublicApi;
import com.skateboard.uibackend.client.appconfig.generated.invoker.ApiClient;
import com.skateboard.uibackend.web.BearerTokenExchangeFilter;
import com.skateboard.uibackend.web.CorrelationIdExchangeFilter;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These {@code @Bean} methods are plain Java — invoking them directly (no
 * Spring context) is enough to exercise the WebClient/ApiClient wiring and
 * confirm the configured base URL makes it through, mirroring how the other
 * {@code *ApiConfig} classes are tested.
 */
class AppConfigApiConfigTest {

    private final AppConfigApiConfig config = new AppConfigApiConfig();

    @Test
    void wiresTheApiClientToTheConfiguredBaseUrl() {
        ClientsProperties properties = new ClientsProperties();
        properties.getAppConfig().setBaseUrl("http://app-config-be");
        properties.getAppConfig().setConnectTimeoutMs(1000);
        properties.getAppConfig().setReadTimeoutMs(2000);

        ApiClient apiClient = config.appConfigApiClient(WebClient.builder(), properties,
                new BearerTokenExchangeFilter(), new CorrelationIdExchangeFilter());

        assertThat(apiClient.getBasePath()).isEqualTo("http://app-config-be");
    }

    @Test
    void wiresEachGeneratedApiToTheSharedApiClient() {
        ApiClient apiClient = new ApiClient();

        assertThat(config.publicConfigApi(apiClient)).isInstanceOf(PublicApi.class);
        assertThat(config.brandingAdminApi(apiClient)).isInstanceOf(AdminApi.class);
        assertThat(config.homeVideoCategoryConfigApi(apiClient)).isInstanceOf(HomeApi.class);
        assertThat(config.homeFeaturedPlayerApi(apiClient)).isInstanceOf(HomeFeaturedPlayerApi.class);
        assertThat(config.aboutUsApi(apiClient)).isInstanceOf(AboutUsApi.class);
        assertThat(config.campaignApi(apiClient)).isInstanceOf(CampaignApi.class);
    }
}
