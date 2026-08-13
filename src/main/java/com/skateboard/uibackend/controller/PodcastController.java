package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.podcast.generated.model.CreatePostRequest;
import com.skateboard.uibackend.client.podcast.generated.model.FeedPageResponse;
import com.skateboard.uibackend.client.podcast.generated.model.ImportPostsRequest;
import com.skateboard.uibackend.client.podcast.generated.model.ImportResult;
import com.skateboard.uibackend.client.podcast.generated.model.PostResponse;
import com.skateboard.uibackend.client.podcast.generated.model.UpdatePostRequest;
import com.skateboard.uibackend.service.PodcastService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Passes the frontend straight through to skateboard-podcast-be — same
 * paths, same request/response DTOs, since there's no shape divergence to
 * justify a UI-specific reshaping yet. The {@code @PreAuthorize} authorities
 * are copied from api/openapi.yaml's {@code x-required-permissions}:
 * coarse-grained, token-claim-only checks at this API boundary (a BFF
 * responsibility), not a duplicate of skateboard-podcast-be's own domain
 * authorization, which still runs downstream against the relayed token.
 */
@RestController
public class PodcastController {

    private final PodcastService podcastService;

    public PodcastController(PodcastService podcastService) {
        this.podcastService = podcastService;
    }

    @GetMapping("/api/podcast")
    @PreAuthorize("hasAuthority('FUNC_TAB_PODCAST')")
    public FeedPageResponse getFeed(@RequestParam(defaultValue = "0") Integer page,
                                     @RequestParam(defaultValue = "10") Integer size) {
        return podcastService.getFeed(page, size);
    }

    @GetMapping("/api/podcast/{slug}")
    @PreAuthorize("hasAuthority('FUNC_TAB_PODCAST')")
    public PostResponse getBySlug(@PathVariable String slug) {
        return podcastService.getBySlug(slug);
    }

    @PostMapping("/api/podcast")
    @PreAuthorize("hasAuthority('FUNC_PODCAST_CREATE_POST')")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(@RequestBody CreatePostRequest request) {
        return podcastService.create(request);
    }

    @PutMapping("/api/podcast/{id}")
    @PreAuthorize("hasAuthority('FUNC_PODCAST_EDIT_POST')")
    public PostResponse update(@PathVariable UUID id, @RequestBody UpdatePostRequest request) {
        return podcastService.update(id, request);
    }

    @DeleteMapping("/api/podcast/{id}")
    @PreAuthorize("hasAuthority('FUNC_PODCAST_DELETE_POST')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        podcastService.delete(id);
    }

    @PostMapping("/api/podcast/import")
    @PreAuthorize("hasAuthority('FUNC_PODCAST_IMPORT_JSON')")
    public ImportResult importPosts(@RequestBody ImportPostsRequest request) {
        return podcastService.importPosts(request);
    }
}
