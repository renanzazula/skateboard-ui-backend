package com.skateboard.uibackend.controller;

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
import com.skateboard.uibackend.service.PodcastService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link PodcastController} just delegates to {@link PodcastService} —
 * mirrors {@link CampaignControllerTest}'s shape; auth enforcement is covered
 * separately by {@link PodcastControllerSecurityTest}.
 */
class PodcastControllerTest {

    @Mock
    private PodcastService podcastService;

    private PodcastController controller;

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new PodcastController(podcastService);
    }

    @Test
    void getFeedDelegates() {
        FeedPageResponse response = new FeedPageResponse();
        when(podcastService.getFeed(0, 10, "board")).thenReturn(response);

        assertThat(controller.getFeed(0, 10, "board")).isSameAs(response);
    }

    @Test
    void getBySlugDelegates() {
        PostResponse response = new PostResponse().id(id);
        when(podcastService.getBySlug("a-post")).thenReturn(response);

        assertThat(controller.getBySlug("a-post")).isSameAs(response);
    }

    @Test
    void createDelegates() {
        CreatePostRequest request = new CreatePostRequest();
        PostResponse response = new PostResponse().id(id);
        when(podcastService.create(request)).thenReturn(response);

        assertThat(controller.create(request)).isSameAs(response);
    }

    @Test
    void updateDelegates() {
        UpdatePostRequest request = new UpdatePostRequest();
        PostResponse response = new PostResponse().id(id);
        when(podcastService.update(id, request)).thenReturn(response);

        assertThat(controller.update(id, request)).isSameAs(response);
    }

    @Test
    void deleteDelegates() {
        controller.delete(id);

        verify(podcastService).delete(id);
    }

    @Test
    void importPostsDelegates() {
        ImportPostsRequest request = new ImportPostsRequest();
        ImportResult response = new ImportResult();
        when(podcastService.importPosts(request)).thenReturn(response);

        assertThat(controller.importPosts(request)).isSameAs(response);
    }

    @Test
    void triggerSyncDelegates() {
        SyncResultResponse response = new SyncResultResponse();
        when(podcastService.triggerSync()).thenReturn(response);

        assertThat(controller.triggerSync()).isSameAs(response);
    }

    @Test
    void getCategoriesDelegates() {
        List<CategoryResponse> response = List.of(new CategoryResponse().slug("skate-clips"));
        when(podcastService.getCategories()).thenReturn(response);

        assertThat(controller.getCategories()).isSameAs(response);
    }

    @Test
    void getCategoryPostsDelegates() {
        FeedPageResponse response = new FeedPageResponse();
        when(podcastService.getCategoryPosts("skate-clips", 0, 10)).thenReturn(response);

        assertThat(controller.getCategoryPosts("skate-clips", 0, 10)).isSameAs(response);
    }

    @Test
    void getAdminCategoriesDelegates() {
        List<AdminCategoryResponse> response = List.of(new AdminCategoryResponse().slug("skate-clips"));
        when(podcastService.getAdminCategories()).thenReturn(response);

        assertThat(controller.getAdminCategories()).isSameAs(response);
    }

    @Test
    void updateCategoryDelegates() {
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        AdminCategoryResponse response = new AdminCategoryResponse().slug("skate-clips");
        when(podcastService.updateCategory(id, request)).thenReturn(response);

        assertThat(controller.updateCategory(id, request)).isSameAs(response);
    }

    @Test
    void reorderCategoriesDelegates() {
        ReorderCategoriesRequest request = new ReorderCategoriesRequest();
        List<AdminCategoryResponse> response = List.of(new AdminCategoryResponse().slug("skate-clips"));
        when(podcastService.reorderCategories(request)).thenReturn(response);

        assertThat(controller.reorderCategories(request)).isSameAs(response);
    }

    @Test
    void setDefaultCategoryDelegates() {
        AdminCategoryResponse response = new AdminCategoryResponse().slug("skate-clips");
        when(podcastService.setDefaultCategory(id)).thenReturn(response);

        assertThat(controller.setDefaultCategory(id)).isSameAs(response);
    }
}
