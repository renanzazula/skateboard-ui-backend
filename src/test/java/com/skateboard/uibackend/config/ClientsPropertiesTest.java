package com.skateboard.uibackend.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain getter/setter binding target for the {@code clients.*} configuration
 * properties — one nested class per downstream service. Exercises every
 * accessor since {@code @ConfigurationProperties} binding relies on them all
 * being present and correctly wired, and none of them carry any logic beyond
 * the field access itself.
 */
class ClientsPropertiesTest {

    private final ClientsProperties properties = new ClientsProperties();

    @Test
    void defaultsEachNestedConfigToItsOwnInstance() {
        assertThat(properties.getPodcast()).isNotNull();
        assertThat(properties.getUser()).isNotNull();
        assertThat(properties.getAppConfig()).isNotNull();
        assertThat(properties.getNotification()).isNotNull();
    }

    @Test
    void podcastConfigBindsBaseUrlAndTimeouts() {
        properties.getPodcast().setBaseUrl("http://podcast-be");
        properties.getPodcast().setConnectTimeoutMs(111);
        properties.getPodcast().setReadTimeoutMs(222);

        assertThat(properties.getPodcast().getBaseUrl()).isEqualTo("http://podcast-be");
        assertThat(properties.getPodcast().getConnectTimeoutMs()).isEqualTo(111);
        assertThat(properties.getPodcast().getReadTimeoutMs()).isEqualTo(222);
    }

    @Test
    void userConfigBindsBaseUrlAndTimeouts() {
        properties.getUser().setBaseUrl("http://user-be");
        properties.getUser().setConnectTimeoutMs(111);
        properties.getUser().setReadTimeoutMs(222);

        assertThat(properties.getUser().getBaseUrl()).isEqualTo("http://user-be");
        assertThat(properties.getUser().getConnectTimeoutMs()).isEqualTo(111);
        assertThat(properties.getUser().getReadTimeoutMs()).isEqualTo(222);
    }

    @Test
    void appConfigConfigBindsBaseUrlAndTimeouts() {
        properties.getAppConfig().setBaseUrl("http://app-config-be");
        properties.getAppConfig().setConnectTimeoutMs(111);
        properties.getAppConfig().setReadTimeoutMs(222);

        assertThat(properties.getAppConfig().getBaseUrl()).isEqualTo("http://app-config-be");
        assertThat(properties.getAppConfig().getConnectTimeoutMs()).isEqualTo(111);
        assertThat(properties.getAppConfig().getReadTimeoutMs()).isEqualTo(222);
    }

    @Test
    void notificationConfigBindsBaseUrlAndTimeouts() {
        properties.getNotification().setBaseUrl("http://notification-be");
        properties.getNotification().setConnectTimeoutMs(111);
        properties.getNotification().setReadTimeoutMs(222);

        assertThat(properties.getNotification().getBaseUrl()).isEqualTo("http://notification-be");
        assertThat(properties.getNotification().getConnectTimeoutMs()).isEqualTo(111);
        assertThat(properties.getNotification().getReadTimeoutMs()).isEqualTo(222);
    }

    @Test
    void defaultTimeoutsMatchEveryNestedConfig() {
        assertThat(properties.getPodcast().getConnectTimeoutMs()).isEqualTo(3000);
        assertThat(properties.getPodcast().getReadTimeoutMs()).isEqualTo(5000);
        assertThat(properties.getUser().getConnectTimeoutMs()).isEqualTo(3000);
        assertThat(properties.getUser().getReadTimeoutMs()).isEqualTo(5000);
        assertThat(properties.getAppConfig().getConnectTimeoutMs()).isEqualTo(3000);
        assertThat(properties.getAppConfig().getReadTimeoutMs()).isEqualTo(5000);
        assertThat(properties.getNotification().getConnectTimeoutMs()).isEqualTo(3000);
        assertThat(properties.getNotification().getReadTimeoutMs()).isEqualTo(5000);
    }
}
