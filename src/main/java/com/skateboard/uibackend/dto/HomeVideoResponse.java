package com.skateboard.uibackend.dto;

import java.util.UUID;

/**
 * Home dashboard's video shape — a slim projection of skateboard-podcast-be's
 * {@code PostResponse}, not a 1:1 passthrough (unlike {@code PodcastController}),
 * since the gallery needs only enough to render a thumbnail and open Video
 * Details. {@code slug} is required, not cosmetic: Video Details
 * (skateboard-fe's {@code /podcast/[slug]}) is keyed by slug, not id — there
 * is no by-id read route. See api/bff-openapi.yaml's "home" tag for the
 * published contract.
 */
public record HomeVideoResponse(UUID id, String slug, String title, String thumbnailUrl, String youtubeVideoId, String category) {
}
