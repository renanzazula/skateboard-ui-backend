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
 * {@link PodcastService} is a thin passthrough to {@link PodcastClient} —
 * mirrors {@link AppConfigServiceTest}'s shape.
 */
class PodcastServiceTest {

    @Mock
    private PodcastClient podcastClient;

    private PodcastService service;

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PodcastService(podcastClient);
    }

    @Test
    void getFeedDelegates() {
        FeedPageResponse response = new FeedPageResponse();
        when(podcastClient.getFeed(0, 10, "board")).thenReturn(response);

        assertThat(service.getFeed(0, 10, "board")).isSameAs(response);
    }

    @Test
    void getBySlugDelegates() {
        PostResponse response = new PostResponse().id(id);
        when(podcastClient.getBySlug("a-post")).thenReturn(response);

        assertThat(service.getBySlug("a-post")).isSameAs(response);
    }

    @Test
    void createDelegates() {
        CreatePostRequest request = new CreatePostRequest();
        PostResponse response = new PostResponse().id(id);
        when(podcastClient.create(request)).thenReturn(response);

        assertThat(service.create(request)).isSameAs(response);
    }

    @Test
    void updateDelegates() {
        UpdatePostRequest request = new UpdatePostRequest();
        PostResponse response = new PostResponse().id(id);
        when(podcastClient.update(id, request)).thenReturn(response);

        assertThat(service.update(id, request)).isSameAs(response);
    }

    @Test
    void deleteDelegates() {
        service.delete(id);

        verify(podcastClient).delete(id);
    }

    @Test
    void importPostsDelegates() {
        ImportPostsRequest request = new ImportPostsRequest();
        ImportResult response = new ImportResult();
        when(podcastClient.importPosts(request)).thenReturn(response);

        assertThat(service.importPosts(request)).isSameAs(response);
    }

    @Test
    void triggerSyncDelegates() {
        SyncResultResponse response = new SyncResultResponse();
        when(podcastClient.triggerSync()).thenReturn(response);

        assertThat(service.triggerSync()).isSameAs(response);
    }

    @Test
    void getCategoriesDelegates() {
        List<CategoryResponse> response = List.of(new CategoryResponse().slug("skate-clips"));
        when(podcastClient.getCategories()).thenReturn(response);

        assertThat(service.getCategories()).isSameAs(response);
    }

    @Test
    void getCategoryPostsDelegates() {
        FeedPageResponse response = new FeedPageResponse();
        when(podcastClient.getCategoryPosts("skate-clips", 0, 10)).thenReturn(response);

        assertThat(service.getCategoryPosts("skate-clips", 0, 10)).isSameAs(response);
    }

    @Test
    void getAdminCategoriesDelegates() {
        List<AdminCategoryResponse> response = List.of(new AdminCategoryResponse().slug("skate-clips"));
        when(podcastClient.getAdminCategories()).thenReturn(response);

        assertThat(service.getAdminCategories()).isSameAs(response);
    }

    @Test
    void updateCategoryDelegates() {
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        AdminCategoryResponse response = new AdminCategoryResponse().slug("skate-clips");
        when(podcastClient.updateCategory(id, request)).thenReturn(response);

        assertThat(service.updateCategory(id, request)).isSameAs(response);
    }

    @Test
    void reorderCategoriesDelegates() {
        ReorderCategoriesRequest request = new ReorderCategoriesRequest();
        List<AdminCategoryResponse> response = List.of(new AdminCategoryResponse().slug("skate-clips"));
        when(podcastClient.reorderCategories(request)).thenReturn(response);

        assertThat(service.reorderCategories(request)).isSameAs(response);
    }

    @Test
    void setDefaultCategoryDelegates() {
        AdminCategoryResponse response = new AdminCategoryResponse().slug("skate-clips");
        when(podcastClient.setDefaultCategory(id)).thenReturn(response);

        assertThat(service.setDefaultCategory(id)).isSameAs(response);
    }
}
