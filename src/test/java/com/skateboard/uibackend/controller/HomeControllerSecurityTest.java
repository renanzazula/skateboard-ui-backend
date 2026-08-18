package com.skateboard.uibackend.controller;

import com.skateboard.uibackend.config.SecurityConfig;
import com.skateboard.uibackend.dto.HomeVideoResponse;
import com.skateboard.uibackend.service.HomeService;
import com.skateboard.uibackend.web.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.BDDMockito.given;

/**
 * Verifies the BFF's own authentication/authorization gate — not
 * skateboard-app-config-be's or skateboard-podcast-be's, which are separate
 * services exercised via their clients (mocked out here through HomeService).
 */
@WebMvcTest(controllers = HomeController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
class HomeControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HomeService homeService;

    @Test
    void rejectsRequestsWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/home/videos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void rejectsATokenMissingTheRequiredAuthority() throws Exception {
        mockMvc.perform(get("/api/home/videos").with(jwt().authorities(() -> "FUNC_SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void allowsATokenWithTheRequiredAuthority() throws Exception {
        given(homeService.getVideos())
                .willReturn(List.of(new HomeVideoResponse(UUID.randomUUID(), "title", "Title", "cover.png", 1280, 720, null, "podcasts")));

        mockMvc.perform(get("/api/home/videos").with(jwt().authorities(() -> "FUNC_TAB_HOME")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Title"));
    }
}
