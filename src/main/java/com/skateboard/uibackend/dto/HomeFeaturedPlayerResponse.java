package com.skateboard.uibackend.dto;

/**
 * Home dashboard's Featured Player content shape (README-home-featured-mini-player.md
 * §13) — a source-agnostic, UI-ready projection resolved by whichever {@link
 * com.skateboard.uibackend.service.FeaturedContentResolver} owns {@code
 * source}. Mirrors {@link HomeVideoResponse}'s "slim projection, not a 1:1
 * passthrough" shape.
 */
public record HomeFeaturedPlayerResponse(String id, String source, String title, String subtitle,
                                          String thumbnailUrl, Integer duration, Playback playback, String position) {

    public record Playback(String type, String reference) {
    }
}
