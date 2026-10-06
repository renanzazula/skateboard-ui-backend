package com.skateboard.uibackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "clients")
public class ClientsProperties {

    private final ClientConfig podcast = new ClientConfig();
    private final ClientConfig user = new ClientConfig();
    private final ClientConfig appConfig = new ClientConfig();
    private final ClientConfig notification = new ClientConfig();

    @NestedConfigurationProperty
    public ClientConfig getPodcast() {
        return podcast;
    }

    @NestedConfigurationProperty
    public ClientConfig getUser() {
        return user;
    }

    @NestedConfigurationProperty
    public ClientConfig getAppConfig() {
        return appConfig;
    }

    @NestedConfigurationProperty
    public ClientConfig getNotification() {
        return notification;
    }

    /**
     * Binding target shared by every {@code clients.*} entry (base URL +
     * connect/read timeouts) — one instance per downstream service, not one
     * class per service, since the shape is identical across all of them.
     */
    public static class ClientConfig {
        private String baseUrl;
        private int connectTimeoutMs = 3000;
        private int readTimeoutMs = 5000;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }
    }
}
