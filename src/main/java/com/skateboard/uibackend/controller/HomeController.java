package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.dto.HomeFeaturedPlayerResponse;
import com.skateboard.uibackend.dto.HomeVideoResponse;
import com.skateboard.uibackend.service.HomeFeaturedPlayerService;
import com.skateboard.uibackend.service.HomeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Backs the mobile Home dashboard (README_HOME_DASHBOARD.md) — aggregates
 * skateboard-app-config-be's category configuration with
 * skateboard-podcast-be's videos. Gated by FUNC_TAB_HOME, the same authority
 * that controls the Home tab's visibility, matching how PodcastController's
 * routes mirror FUNC_TAB_PODCAST.
 */
@RestController
public class HomeController {

    private final HomeService homeService;
    private final HomeFeaturedPlayerService homeFeaturedPlayerService;

    public HomeController(HomeService homeService, HomeFeaturedPlayerService homeFeaturedPlayerService) {
        this.homeService = homeService;
        this.homeFeaturedPlayerService = homeFeaturedPlayerService;
    }

    @GetMapping("/api/home/videos")
    @PreAuthorize("hasAuthority('FUNC_TAB_HOME')")
    public List<HomeVideoResponse> getVideos() {
        return homeService.getVideos();
    }

    // 204 (no body) when there is no active Featured Player to show — distinct
    // from a bare JSON null, which a top-level-null response body doesn't
    // serialize as (Spring writes nothing at all for a null return value).
    @GetMapping("/api/home/featured-player")
    @PreAuthorize("hasAuthority('FUNC_TAB_HOME')")
    public ResponseEntity<HomeFeaturedPlayerResponse> getFeaturedPlayer() {
        HomeFeaturedPlayerResponse response = homeFeaturedPlayerService.getFeaturedPlayer();
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.noContent().build();
    }
}
