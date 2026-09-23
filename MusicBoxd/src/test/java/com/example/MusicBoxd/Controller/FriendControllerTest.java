package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Config.SecurityConfig;
import com.example.MusicBoxd.Model.Friend;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Repository.FriendRepository;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.service.NotificationService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(
        controllers = FriendController.class,
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
class FriendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FriendRepository friendRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private NotificationService notificationService;

    private User user(long id, String oktaId, String username) {
        User u = new User(oktaId, username, username + "@example.com", "", "");
        u.setId(id);
        return u;
    }

    private void stubCurrentUser(User currentUser) {
        when(userRepository.findByOktaUserId(currentUser.getOktaUserId()))
                .thenReturn(Optional.of(currentUser));
    }

    private void stubEmptyFriendLists() {
        when(friendRepository.findByReceiverIdAndStatus(any(), eq("PENDING")))
                .thenReturn(new ArrayList<>());
        when(friendRepository.findByRequesterIdAndStatus(any(), eq("PENDING")))
                .thenReturn(new ArrayList<>());
        when(friendRepository.findByStatus(eq("ACCEPTED")))
                .thenReturn(new ArrayList<>());
        when(userRepository.findAll()).thenReturn(new ArrayList<>());
    }

    // ---------- GET /friends ----------

    @Test
    void redirectsUnauthenticatedUserToOAuth2LoginForFriendsPage() throws Exception {
        mockMvc.perform(get("/friends"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.startsWith(
                                "/oauth2/authorization/" + TestOAuth2Config.REGISTRATION_ID
                        )
                ));
    }

    @Test
    void friendsPageShowsSuggestedUsersExcludingCurrentUserFriendsAndPendingRequests() throws Exception {
        User currentUser = user(1L, "okta-1", "me");
        User friend = user(2L, "okta-2", "friend");
        User pendingSender = user(3L, "okta-3", "pendingSender");
        User pendingReceiver = user(4L, "okta-4", "pendingReceiver");
        User suggested = user(5L, "okta-5", "suggested");

        stubCurrentUser(currentUser);

        Friend acceptedFriendship = new Friend(1L, 2L, "ACCEPTED");
        Friend receivedRequest = new Friend(3L, 1L, "PENDING");
        Friend sentRequest = new Friend(1L, 4L, "PENDING");

        when(friendRepository.findByReceiverIdAndStatus(1L, "PENDING"))
                .thenReturn(List.of(receivedRequest));
        when(friendRepository.findByRequesterIdAndStatus(1L, "PENDING"))
                .thenReturn(List.of(sentRequest));
        when(friendRepository.findByStatus("ACCEPTED"))
                .thenReturn(List.of(acceptedFriendship));
        when(userRepository.findAll()).thenReturn(
                List.of(currentUser, friend, pendingSender, pendingReceiver, suggested));
        when(userRepository.findById(3L)).thenReturn(Optional.of(pendingSender));
        when(userRepository.findById(4L)).thenReturn(Optional.of(pendingReceiver));
        when(userRepository.findById(2L)).thenReturn(Optional.of(friend));

        mockMvc.perform(get("/friends").with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("friends"))
                .andExpect(model().attribute("suggestedUsers", List.of(suggested)))
                .andExpect(model().attribute("friendIds", List.of(2L)));
    }

    @Test
    void friendsPageTreatsCurrentUserAsFriendWhenTheyAreTheReceiverOfTheAcceptedRequest() throws Exception {
        User currentUser = user(2L, "okta-2", "me");
        User friend = user(1L, "okta-1", "friend");

        stubCurrentUser(currentUser);
        stubEmptyFriendLists();

        Friend acceptedFriendship = new Friend(1L, 2L, "ACCEPTED");
        when(friendRepository.findByStatus("ACCEPTED")).thenReturn(List.of(acceptedFriendship));
        when(userRepository.findById(1L)).thenReturn(Optional.of(friend));

        mockMvc.perform(get("/friends").with(OidcTestUsers.oidcUser("okta-2", "me@example.com")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("friendIds", List.of(1L)));
    }

    @Test
    void friendsPageThrowsWhenAuthenticatedPrincipalHasNoMatchingLocalUser() throws Exception {
        when(userRepository.findByOktaUserId("okta-missing")).thenReturn(Optional.empty());

        Exception thrown = org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                mockMvc.perform(get("/friends")
                        .with(OidcTestUsers.oidcUser("okta-missing", "missing@example.com"))));

        org.assertj.core.api.Assertions.assertThat(thrown).hasRootCauseMessage("Current user not found");
    }

    // ---------- GET /friends/search ----------

    @Test
    void searchReturnsMatchingUsersExcludingCurrentUser() throws Exception {
        User currentUser = user(1L, "okta-1", "me");
        User match = user(2L, "okta-2", "bob");

        stubCurrentUser(currentUser);
        stubEmptyFriendLists();

        when(userRepository.findByUsernameContainingIgnoreCase("bob"))
                .thenReturn(new ArrayList<>(List.of(match, currentUser)));

        mockMvc.perform(get("/friends/search")
                        .param("query", "bob")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("friends"))
                .andExpect(model().attribute("searchResults", List.of(match)))
                .andExpect(model().attribute("searchQuery", "bob"));
    }

    @Test
    void searchWithBlankQueryReturnsEmptyResultsWithoutCallingRepository() throws Exception {
        User currentUser = user(1L, "okta-1", "me");

        stubCurrentUser(currentUser);
        stubEmptyFriendLists();

        mockMvc.perform(get("/friends/search")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("searchResults", List.of()));

        verify(userRepository, never()).findByUsernameContainingIgnoreCase(any());
    }

    // ---------- POST /friends/request ----------

    @Test
    void sendFriendRequestSavesRequestAndNotifiesReceiver() throws Exception {
        User currentUser = user(1L, "okta-1", "me");
        User receiver = user(2L, "okta-2", "bob");

        stubCurrentUser(currentUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(friendRepository.findByRequesterIdAndReceiverId(1L, 2L)).thenReturn(Optional.empty());
        when(friendRepository.findByReceiverIdAndRequesterId(1L, 2L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/friends/request")
                        .param("receiverId", "2")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/friends"));

        verify(friendRepository).save(argThat(f ->
                f.getRequesterId().equals(1L) && f.getReceiverId().equals(2L)
                        && f.getStatus().equals("PENDING")));
        verify(notificationService).notifyFriendRequest(2L, 1L, "me");
    }

    @Test
    void sendFriendRequestToSelfDoesNothing() throws Exception {
        User currentUser = user(1L, "okta-1", "me");
        stubCurrentUser(currentUser);

        mockMvc.perform(post("/friends/request")
                        .param("receiverId", "1")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/friends"));

        verify(friendRepository, never()).save(any());
        verify(notificationService, never()).notifyFriendRequest(any(), any(), any());
    }

    @Test
    void sendFriendRequestToMissingReceiverDoesNothing() throws Exception {
        User currentUser = user(1L, "okta-1", "me");
        stubCurrentUser(currentUser);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/friends/request")
                        .param("receiverId", "99")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/friends"));

        verify(friendRepository, never()).save(any());
    }

    @Test
    void sendFriendRequestWhenRequestAlreadyExistsDoesNotDuplicate() throws Exception {
        User currentUser = user(1L, "okta-1", "me");
        User receiver = user(2L, "okta-2", "bob");
        stubCurrentUser(currentUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(friendRepository.findByRequesterIdAndReceiverId(1L, 2L))
                .thenReturn(Optional.of(new Friend(1L, 2L, "PENDING")));

        mockMvc.perform(post("/friends/request")
                        .param("receiverId", "2")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/friends"));

        verify(friendRepository, never()).save(any());
    }

    @Test
    void sendFriendRequestWhenReverseRequestAlreadyExistsDoesNotDuplicate() throws Exception {
        User currentUser = user(1L, "okta-1", "me");
        User receiver = user(2L, "okta-2", "bob");
        stubCurrentUser(currentUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(friendRepository.findByRequesterIdAndReceiverId(1L, 2L)).thenReturn(Optional.empty());
        when(friendRepository.findByReceiverIdAndRequesterId(1L, 2L))
                .thenReturn(Optional.of(new Friend(2L, 1L, "PENDING")));

        mockMvc.perform(post("/friends/request")
                        .param("receiverId", "2")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/friends"));

        verify(friendRepository, never()).save(any());
    }

    // ---------- POST /friends/accept/{id} ----------

    @Test
    void acceptFriendRequestMarksAcceptedAndNotifiesRequester() throws Exception {
        User currentUser = user(2L, "okta-2", "receiver");
        User requester = user(1L, "okta-1", "requester");
        stubCurrentUser(currentUser);

        Friend friend = new Friend(1L, 2L, "PENDING");
        friend.setId(10L);
        when(friendRepository.findById(10L)).thenReturn(Optional.of(friend));
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));

        mockMvc.perform(post("/friends/accept/10")
                        .with(OidcTestUsers.oidcUser("okta-2", "receiver@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/friends"));

        verify(friendRepository).save(argThat(f -> f.getStatus().equals("ACCEPTED")));
        verify(notificationService).notifyFriendAccepted(1L, 2L, "receiver");
    }

    @Test
    void acceptFriendRequestWhenNotFoundDoesNothing() throws Exception {
        User currentUser = user(2L, "okta-2", "receiver");
        stubCurrentUser(currentUser);
        when(friendRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/friends/accept/999")
                        .with(OidcTestUsers.oidcUser("okta-2", "receiver@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/friends"));

        verify(friendRepository, never()).save(any());
    }

    @Test
    void acceptFriendRequestWhenCurrentUserIsNotReceiverIsRejected() throws Exception {
        User currentUser = user(3L, "okta-3", "intruder");
        stubCurrentUser(currentUser);

        Friend friend = new Friend(1L, 2L, "PENDING");
        friend.setId(10L);
        when(friendRepository.findById(10L)).thenReturn(Optional.of(friend));

        mockMvc.perform(post("/friends/accept/10")
                        .with(OidcTestUsers.oidcUser("okta-3", "intruder@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/friends"));

        verify(friendRepository, never()).save(any());
        verify(notificationService, never()).notifyFriendAccepted(any(), any(), any());
    }

    @Test
    void acceptFriendRequestWhenAlreadyProcessedIsIgnored() throws Exception {
        User currentUser = user(2L, "okta-2", "receiver");
        stubCurrentUser(currentUser);

        Friend friend = new Friend(1L, 2L, "ACCEPTED");
        friend.setId(10L);
        when(friendRepository.findById(10L)).thenReturn(Optional.of(friend));

        mockMvc.perform(post("/friends/accept/10")
                        .with(OidcTestUsers.oidcUser("okta-2", "receiver@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/friends"));

        verify(friendRepository, never()).save(any());
    }

    // ---------- POST /friends/decline/{id} ----------

    @Test
    void declineFriendRequestMarksDeclined() throws Exception {
        User currentUser = user(2L, "okta-2", "receiver");
        stubCurrentUser(currentUser);

        Friend friend = new Friend(1L, 2L, "PENDING");
        friend.setId(10L);
        when(friendRepository.findById(10L)).thenReturn(Optional.of(friend));

        mockMvc.perform(post("/friends/decline/10")
                        .with(OidcTestUsers.oidcUser("okta-2", "receiver@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/friends"));

        verify(friendRepository).save(argThat(f -> f.getStatus().equals("DECLINED")));
    }

    @Test
    void declineFriendRequestWhenCurrentUserIsNotReceiverIsRejected() throws Exception {
        User currentUser = user(3L, "okta-3", "intruder");
        stubCurrentUser(currentUser);

        Friend friend = new Friend(1L, 2L, "PENDING");
        friend.setId(10L);
        when(friendRepository.findById(10L)).thenReturn(Optional.of(friend));

        mockMvc.perform(post("/friends/decline/10")
                        .with(OidcTestUsers.oidcUser("okta-3", "intruder@example.com")))
                .andExpect(status().is3xxRedirection());

        verify(friendRepository, never()).save(any());
    }

    @Test
    void declineFriendRequestWhenMissingIsIgnored() throws Exception {
        User currentUser = user(2L, "okta-2", "receiver");
        stubCurrentUser(currentUser);
        when(friendRepository.findById(10L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/friends/decline/10")
                        .with(OidcTestUsers.oidcUser("okta-2", "receiver@example.com")))
                .andExpect(status().is3xxRedirection());

        verify(friendRepository, never()).save(any());
    }

    @Test
    void declineFriendRequestWhenAlreadyProcessedIsIgnored() throws Exception {
        User currentUser = user(2L, "okta-2", "receiver");
        stubCurrentUser(currentUser);

        Friend friend = new Friend(1L, 2L, "DECLINED");
        friend.setId(10L);
        when(friendRepository.findById(10L)).thenReturn(Optional.of(friend));

        mockMvc.perform(post("/friends/decline/10")
                        .with(OidcTestUsers.oidcUser("okta-2", "receiver@example.com")))
                .andExpect(status().is3xxRedirection());

        verify(friendRepository, never()).save(any());
    }

    // ---------- POST /friends/remove/{id} ----------

    @Test
    void removeFriendDeletesFriendshipWhenCurrentUserIsRequester() throws Exception {
        User currentUser = user(1L, "okta-1", "me");
        stubCurrentUser(currentUser);

        Friend friend = new Friend(1L, 2L, "ACCEPTED");
        friend.setId(10L);
        when(friendRepository.findById(10L)).thenReturn(Optional.of(friend));

        mockMvc.perform(post("/friends/remove/10")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/friends"));

        verify(friendRepository).delete(friend);
    }

    @Test
    void removeFriendDeletesFriendshipWhenCurrentUserIsReceiver() throws Exception {
        User currentUser = user(2L, "okta-2", "me");
        stubCurrentUser(currentUser);

        Friend friend = new Friend(1L, 2L, "ACCEPTED");
        friend.setId(10L);
        when(friendRepository.findById(10L)).thenReturn(Optional.of(friend));

        mockMvc.perform(post("/friends/remove/10")
                        .with(OidcTestUsers.oidcUser("okta-2", "me@example.com")))
                .andExpect(status().is3xxRedirection());

        verify(friendRepository).delete(friend);
    }

    @Test
    void removeFriendWhenCurrentUserIsUnrelatedDoesNotDelete() throws Exception {
        User currentUser = user(3L, "okta-3", "stranger");
        stubCurrentUser(currentUser);

        Friend friend = new Friend(1L, 2L, "ACCEPTED");
        friend.setId(10L);
        when(friendRepository.findById(10L)).thenReturn(Optional.of(friend));

        mockMvc.perform(post("/friends/remove/10")
                        .with(OidcTestUsers.oidcUser("okta-3", "stranger@example.com")))
                .andExpect(status().is3xxRedirection());

        verify(friendRepository, never()).delete(any());
    }

    @Test
    void removeFriendWhenMissingIsIgnored() throws Exception {
        User currentUser = user(1L, "okta-1", "me");
        stubCurrentUser(currentUser);
        when(friendRepository.findById(10L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/friends/remove/10")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().is3xxRedirection());

        verify(friendRepository, never()).delete(any());
    }
}
