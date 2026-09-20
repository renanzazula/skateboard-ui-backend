package com.skateboard.uibackend.service;

import com.skateboard.uibackend.client.appconfig.generated.model.FeaturedContentSource;
import com.skateboard.uibackend.dto.HomeFeaturedPlayerResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link FeaturedContentResolver#resolveAuto} defaults to {@code null}
 * (AUTO mode unsupported) for any implementation that doesn't override it —
 * {@link PodcastFeaturedContentResolver} is the only current implementation
 * and does override it, so this exercises the default directly against a
 * minimal anonymous implementation.
 */
class FeaturedContentResolverTest {

    @Test
    void resolveAutoDefaultsToNullWhenNotOverridden() {
        FeaturedContentResolver resolver = new FeaturedContentResolver() {
            @Override
            public boolean supports(FeaturedContentSource source) {
                return false;
            }

            @Override
            public HomeFeaturedPlayerResponse resolve(String contentId, String preferredPlatform) {
                return null;
            }
        };

        assertThat(resolver.resolveAuto("SPOTIFY")).isNull();
    }
}
