package com.skateboard.uibackend.service;

import com.skateboard.uibackend.client.podcast.PodcastClient;
import com.skateboard.uibackend.client.podcast.generated.model.AdminCategoryResponse;
import com.skateboard.uibackend.client.podcast.generated.model.CategoryResponse;
import com.skateboard.uibackend.client.podcast.generated.model.CreatePostRequest;
import com.skateboard.uibackend.client.podcast.generated.model.FeedPageResponse;
import com.skateboard.uibackend.client.podcast.generated.model.ImportPostsRequest;
import com.skateboard.uibackend.client.podcast.generated.model.ImportResult;
import com.skateboard.uibackend.client.podcast.generated.model.PostResponse;
import com.skateboard.uibackend.client.podcast.generated.model.ReorderCategoriesRequest;
import com.skateboard.uibackend.client.podcast.generated.model.SyncResultResponse;
import com.skateboard.uibackend.client.podcast.generated.model.UpdateCategoryRequest;
import com.skateboard.uibackend.client.podcast.generated.model.UpdatePostRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Thin pass-through today — the seam where orchestration/aggregation across
 * multiple downstream clients belongs once the frontend needs it (e.g. a
 * future {@code /api/home} combining this with an events/spots service).
 */
@Service
public class PodcastService {

    private final PodcastClient podcastClient;

    public PodcastService(PodcastClient podcastClient) {
        this.podcastClient = podcastClient;
    }

    public FeedPageResponse getFeed(Integer page, Integer size) {
        return podcastClient.getFeed(page, size);
    }

    public PostResponse getBySlug(String slug) {
        return podcastClient.getBySlug(slug);
    }

    public PostResponse create(CreatePostRequest request) {
        return podcastClient.create(request);
    }

    public PostResponse update(UUID id, UpdatePostRequest request) {
        return podcastClient.update(id, request);
    }

    public void delete(UUID id) {
        podcastClient.delete(id);
    }

    public ImportResult importPosts(ImportPostsRequest request) {
        return podcastClient.importPosts(request);
    }

    public SyncResultResponse triggerSync() {
        return podcastClient.triggerSync();
    }

    public List<CategoryResponse> getCategories() {
        return podcastClient.getCategories();
    }

    public FeedPageResponse getCategoryPosts(String slug, Integer page, Integer size) {
        return podcastClient.getCategoryPosts(slug, page, size);
    }

    public List<AdminCategoryResponse> getAdminCategories() {
        return podcastClient.getAdminCategories();
    }

    public AdminCategoryResponse updateCategory(UUID id, UpdateCategoryRequest request) {
        return podcastClient.updateCategory(id, request);
    }

    public List<AdminCategoryResponse> reorderCategories(ReorderCategoriesRequest request) {
        return podcastClient.reorderCategories(request);
    }

    public AdminCategoryResponse setDefaultCategory(UUID id) {
        return podcastClient.setDefaultCategory(id);
    }
}
