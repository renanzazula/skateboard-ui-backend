package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.dto.HomeVideoResponse;
import com.skateboard.uibackend.service.HomeService;
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

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/api/home/videos")
    @PreAuthorize("hasAuthority('FUNC_TAB_HOME')")
    public List<HomeVideoResponse> getVideos() {
        return homeService.getVideos();
    }
}
