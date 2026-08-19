package com.skateboard.uibackend.service;

import com.skateboard.uibackend.client.appconfig.generated.model.FeaturedContentSource;
import com.skateboard.uibackend.dto.HomeFeaturedPlayerResponse;

/**
 * Resolves a Home Featured Player {@code contentSource}/{@code contentId}
 * reference (owned by skateboard-app-config-be) into UI-ready content owned
 * by the matching source service. One implementation per {@link
 * FeaturedContentSource} — kept as a plain interface with a single
 * implementation ({@link PodcastFeaturedContentResolver}) until a second
 * source actually exists, rather than a registry/factory built ahead of need.
 */
public interface FeaturedContentResolver {

    boolean supports(FeaturedContentSource source);

    /**
     * Returns {@code null} when the referenced content no longer
     * exists/isn't published/has no resolvable playback — callers must treat
     * that the same as "no Featured Player configured" (README-home-featured-mini-player.md §14),
     * not as an error.
     *
     * @param preferredPlatform the admin-configured platform to prefer when
     *                          the content has more than one available (raw
     *                          enum value, e.g. "SPOTIFY"/"YOUTUBE"), or
     *                          {@code null} to let the resolver pick its own
     *                          default. Still falls back to whichever
     *                          platform is actually available if the
     *                          preferred one isn't — implementations should
     *                          never fail just because the exact preference
     *                          can't be honored.
     */
    HomeFeaturedPlayerResponse resolve(String contentId, String preferredPlatform);
}
