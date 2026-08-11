package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.client.podcast.generated.model.FeedPageResponse;
import com.skateboard.uibackend.config.SecurityConfig;
import com.skateboard.uibackend.service.PodcastService;
import com.skateboard.uibackend.web.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the BFF's own authentication/authorization gate — not
 * skateboard-podcast-be's, which is a separate service exercised via
 * PodcastClient (mocked out here through PodcastService).
 */
@WebMvcTest(controllers = PodcastController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
class PodcastControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PodcastService podcastService;

    @Test
    void rejectsRequestsWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/podcast"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void rejectsATokenMissingTheRequiredAuthority() throws Exception {
        mockMvc.perform(get("/api/podcast").with(jwt().authorities(() -> "FUNC_SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsATokenWithTheRequiredAuthority() throws Exception {
        given(podcastService.getFeed(any(), any())).willReturn(new FeedPageResponse().page(0).size(10).total(0L));

        mockMvc.perform(get("/api/podcast").with(jwt().authorities(() -> "FUNC_TAB_PODCAST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0));
    }
}
