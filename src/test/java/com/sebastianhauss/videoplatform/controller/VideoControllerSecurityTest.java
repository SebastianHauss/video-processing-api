package com.sebastianhauss.videoplatform.controller;

import com.sebastianhauss.videoplatform.auth.AppUserPrincipal;
import com.sebastianhauss.videoplatform.domain.user.User;
import com.sebastianhauss.videoplatform.domain.user.UserRole;
import com.sebastianhauss.videoplatform.service.VideoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the HTTP-level authorization wiring on the video endpoints: admin-only
 * listing, self-scoped {@code /mine}, and rejection of unauthenticated access.
 * The full security chain (JwtFilter + method security) runs; {@link VideoService}
 * is mocked so no DB/storage is required.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VideoControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VideoService videoService;

    private AppUserPrincipal principal(UUID id, UserRole role) {
        User user = User.builder()
                .id(id)
                .username("user-" + id)
                .passwordHash("hash")
                .email(id + "@example.com")
                .role(role)
                .build();
        return new AppUserPrincipal(user);
    }

    @Test
    void getAllVideos_asUser_isForbidden() throws Exception {
        mockMvc.perform(get("/api/videos")
                        .with(user(principal(UUID.randomUUID(), UserRole.USER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllVideos_asAdmin_isOk() throws Exception {
        when(videoService.getAllVideos()).thenReturn(List.of());

        mockMvc.perform(get("/api/videos")
                        .with(user(principal(UUID.randomUUID(), UserRole.ADMIN))))
                .andExpect(status().isOk());
    }

    @Test
    void getVideosOfUser_asUser_isForbidden() throws Exception {
        mockMvc.perform(get("/api/videos/users/" + UUID.randomUUID())
                        .with(user(principal(UUID.randomUUID(), UserRole.USER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyVideos_isScopedToAuthenticatedPrincipal() throws Exception {
        UUID callerId = UUID.randomUUID();
        when(videoService.getVideosOfUser(callerId)).thenReturn(List.of());

        mockMvc.perform(get("/api/videos/mine")
                        .with(user(principal(callerId, UserRole.USER))))
                .andExpect(status().isOk());

        // The listing must use the caller's own id from the JWT, not a client-supplied one.
        verify(videoService).getVideosOfUser(callerId);
    }

    @Test
    void statusEndpoint_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/videos/" + UUID.randomUUID() + "/status"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void feed_isPubliclyAccessible_withoutAuth() throws Exception {
        when(videoService.getFeed(0, 20)).thenReturn(List.of());

        mockMvc.perform(get("/api/videos/feed"))
                .andExpect(status().isOk());
    }

    @Test
    void mine_unauthenticated_isRejected() throws Exception {
        // Guards the matcher ordering: /mine must stay authenticated even though
        // the catch-all GET /api/videos/* is permitAll for public watching.
        mockMvc.perform(get("/api/videos/mine"))
                .andExpect(status().is4xxClientError());
    }
}
