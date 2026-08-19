package com.skateboard.uibackend.service;

import com.skateboard.uibackend.client.appconfig.AppConfigClient;
import com.skateboard.uibackend.client.appconfig.generated.model.HomeFeaturedPlayerConfigResponse;
import com.skateboard.uibackend.dto.HomeFeaturedPlayerResponse;
import com.skateboard.uibackend.exception.DownstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Resolves the Home dashboard's effective Featured Player: reads the default
 * configuration from skateboard-app-config-be, then hands the configured
 * {@code contentSource}/{@code contentId} to the matching {@link
 * FeaturedContentResolver}. Returns {@code null} whenever the player
 * shouldn't be shown — disabled, unconfigured, app-config-be unavailable, or
 * the referenced content unresolvable — never an error
 * (README-home-featured-mini-player.md §14/§21): the rest of Home must stay
 * usable regardless of this feature's state.
 */
@Service
public class HomeFeaturedPlayerService {

    private static final Logger log = LoggerFactory.getLogger(HomeFeaturedPlayerService.class);

    private final AppConfigClient appConfigClient;
    private final List<FeaturedContentResolver> resolvers;

    public HomeFeaturedPlayerService(AppConfigClient appConfigClient, List<FeaturedContentResolver> resolvers) {
        this.appConfigClient = appConfigClient;
        this.resolvers = resolvers;
    }

    public HomeFeaturedPlayerResponse getFeaturedPlayer() {
        HomeFeaturedPlayerConfigResponse config = loadConfigOrNull();
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())
                || config.getContentSource() == null || config.getContentId() == null) {
            return null;
        }
        FeaturedContentResolver resolver = resolvers.stream()
                .filter(r -> r.supports(config.getContentSource()))
                .findFirst()
                .orElse(null);
        if (resolver == null) {
            log.warn("No FeaturedContentResolver registered for source={}", config.getContentSource());
            return null;
        }
        String preferredPlatform = config.getPreferredPlatform() != null ? config.getPreferredPlatform().getValue() : null;
        HomeFeaturedPlayerResponse resolved = resolver.resolve(config.getContentId(), preferredPlatform);
        if (resolved == null) {
            return null;
        }
        // The resolver only knows the content itself — position is Home
        // layout configuration owned by app-config-be, filled in here.
        return new HomeFeaturedPlayerResponse(resolved.id(), resolved.source(), resolved.title(), resolved.subtitle(),
                resolved.thumbnailUrl(), resolved.duration(), resolved.playback(), config.getPosition().getValue());
    }

    private HomeFeaturedPlayerConfigResponse loadConfigOrNull() {
        try {
            return appConfigClient.getHomeFeaturedPlayerConfig();
        } catch (DownstreamServiceException ex) {
            log.warn("Home Featured Player config unavailable ({}); omitting player", ex.getCode());
            return null;
        }
    }
}
