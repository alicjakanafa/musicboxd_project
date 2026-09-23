package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Config.SecurityConfig;
import com.example.MusicBoxd.Model.Notification;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Repository.NotificationRepository;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.support.OidcTestUsers;
import com.example.MusicBoxd.support.TestOAuth2Config;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(
        controllers = NotificationController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import({SecurityConfig.class, TestOAuth2Config.class})
@TestPropertySource(properties = {
        "okta.oauth2.issuer=https://test-issuer.invalid/",
        "okta.oauth2.client-id=test-client-id"
})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private UserRepository userRepository;

    private User user(long id, String oktaId, String username) {
        User u = new User(oktaId, username, username + "@example.com", "", "");
        u.setId(id);
        return u;
    }

    private void stubCurrentUser(User currentUser) {
        when(userRepository.findByOktaUserId(currentUser.getOktaUserId()))
                .thenReturn(Optional.of(currentUser));
    }

    // ---------- GET /notifications ----------

    @Test
    void redirectsUnauthenticatedUserToOAuth2LoginForNotificationsPage() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.startsWith(
                                "/oauth2/authorization/" + TestOAuth2Config.REGISTRATION_ID
                        )
                ));
    }

    @Test
    void notificationsPageShowsListAndUnreadCountForCurrentUser() throws Exception {
        User currentUser = user(1L, "okta-1", "me");
        stubCurrentUser(currentUser);

        Notification notification = new Notification(1L, 2L, 3L, "FRIEND_REQUEST", "someone sent a request");
        notification.setId(5L);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(notification));
        when(notificationRepository.countByUserIdAndReadFalse(1L)).thenReturn(1L);

        mockMvc.perform(get("/notifications")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("notifications"))
                .andExpect(model().attribute("notifications", List.of(notification)))
                .andExpect(model().attribute("unreadNotificationCount", 1L));
    }

    @Test
    void notificationsPageRedirectsHomeWhenCurrentUserCannotBeFound() throws Exception {
        when(userRepository.findByOktaUserId("okta-missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/notifications")
                        .with(OidcTestUsers.oidcUser("okta-missing", "missing@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/"));

        verify(notificationRepository, never()).findByUserIdOrderByCreatedAtDesc(any());
    }

    @Test
    void openingNotificationRedirectsHomeWhenCurrentUserCannotBeFound() throws Exception {
        when(userRepository.findByOktaUserId("okta-missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/notifications/5/open")
                        .with(OidcTestUsers.oidcUser("okta-missing", "missing@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/"));

        verify(notificationRepository, never()).findById(any());
    }

    // ---------- GET /notifications/{id}/open ----------

    @Test
    void openingFriendRequestNotificationMarksReadAndRedirectsToActorProfile() throws Exception {
        User currentUser = user(1L, "okta-1", "me");
        stubCurrentUser(currentUser);

        Notification notification = new Notification(1L, 2L, 3L, "FRIEND_REQUEST", "text");
        notification.setId(5L);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(notification));

        mockMvc.perform(get("/notifications/5/open")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/profile/2"));

        verify(notificationRepository).save(argThat(Notification::isRead));
    }

    @Test
    void openingFriendAcceptedNotificationRedirectsToActorProfile() throws Exception {
        User currentUser = user(1L, "okta-1", "me");
        stubCurrentUser(currentUser);

        Notification notification = new Notification(1L, 2L, 3L, "FRIEND_ACCEPTED", "text");
        notification.setId(5L);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(notification));

        mockMvc.perform(get("/notifications/5/open")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/profile/2"));
    }

    @Test
    void openingAlbumReviewedNotificationRedirectsHome() throws Exception {
        User currentUser = user(1L, "okta-1", "me");
        stubCurrentUser(currentUser);

        Notification notification = new Notification(1L, 2L, 3L, "ALBUM_REVIEWED", "text");
        notification.setId(5L);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(notification));

        mockMvc.perform(get("/notifications/5/open")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/"));
    }

    @Test
    void openingUnknownNotificationTypeRedirectsToNotificationsList() throws Exception {
        User currentUser = user(1L, "okta-1", "me");
        stubCurrentUser(currentUser);

        Notification notification = new Notification(1L, 2L, 3L, "OTHER", "text");
        notification.setId(5L);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(notification));

        mockMvc.perform(get("/notifications/5/open")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/notifications"));
    }

    @Test
    void openingNotificationThatBelongsToAnotherUserThrows() throws Exception {
        User currentUser = user(1L, "okta-1", "me");
        stubCurrentUser(currentUser);

        Notification notification = new Notification(99L, 2L, 3L, "FRIEND_REQUEST", "text");
        notification.setId(5L);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(notification));

        Exception thrown = org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                mockMvc.perform(get("/notifications/5/open")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com"))));
        assertThat(thrown).hasRootCauseMessage("You cannot open another user's notification");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void openingMissingNotificationThrows() {
        User currentUser = user(1L, "okta-1", "me");
        stubCurrentUser(currentUser);
        when(notificationRepository.findById(5L)).thenReturn(Optional.empty());

        Exception thrown = org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                mockMvc.perform(get("/notifications/5/open")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com"))));
        assertThat(thrown).hasRootCauseMessage("Notification not found");
    }
}
