package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Config.SecurityConfig;
import com.example.MusicBoxd.Model.Friend;
import com.example.MusicBoxd.Model.Message;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Repository.FriendRepository;
import com.example.MusicBoxd.Repository.MessageRepository;
import com.example.MusicBoxd.Repository.NotificationRepository;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.support.OidcTestUsers;
import com.example.MusicBoxd.support.TestOAuth2Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MessageController.class,
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
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MessageController messageController;

    @MockitoBean
    private MessageRepository messageRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private FriendRepository friendRepository;

    private User user(long id, String oktaId, String username) {
        User u = new User(oktaId, username, username + "@example.com", "", "");
        u.setId(id);
        return u;
    }

    private void stubCurrentUser(User currentUser) {
        when(userRepository.findByOktaUserId(currentUser.getOktaUserId()))
                .thenReturn(Optional.of(currentUser));
    }

    /**
     * MessageController reads its principal from {@code SecurityContextHolder} directly
     * (rather than an injected {@code Authentication}), and its two {@code GET} handlers render
     * Thymeleaf templates ({@code messages/list}, {@code messages/index}) that do not exist in
     * {@code src/main/resources/templates}. Driving those handlers through {@code MockMvc} would
     * fail on view resolution rather than exercising the controller's own logic, so these tests
     * invoke the controller bean directly (still wired with the same mocked repositories) and
     * authenticate by populating the security context, matching what the real OIDC login flow
     * would place there.
     */
    private void authenticateAs(String sub) {
        OidcIdToken idToken = new OidcIdToken(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of(IdTokenClaimNames.SUB, sub)
        );
        DefaultOidcUser principal = new DefaultOidcUser(List.of(), idToken);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal, null)
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------- GET /messages ----------

    @Test
    void redirectsUnauthenticatedUserToOAuth2LoginForMessageList() throws Exception {
        mockMvc.perform(get("/messages"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.startsWith(
                                "/oauth2/authorization/" + TestOAuth2Config.REGISTRATION_ID
                        )
                ));
    }

    @Test
    void messageListShowsOneConversationPerOtherUserWithLatestMessage() {
        User currentUser = user(1L, "okta-1", "me");
        User friend = user(2L, "okta-2", "friend");
        stubCurrentUser(currentUser);
        authenticateAs("okta-1");

        Message older = new Message(2L, 1L, "hi", null, null, null, null);
        older.setId(1L);
        Message latest = new Message(1L, 2L, "hey there", null, null, null, null);
        latest.setId(2L);

        when(messageRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(1L, 1L))
                .thenReturn(List.of(latest, older));
        when(userRepository.findById(2L)).thenReturn(Optional.of(friend));

        Model model = new ExtendedModelMap();
        String view = messageController.messages(model);

        assertThat(view).isEqualTo("messages/list");
        assertThat(model.getAttribute("conversationUsers")).isEqualTo(List.of(friend));
        @SuppressWarnings("unchecked")
        Map<Long, Message> latestMessages = (Map<Long, Message>) model.getAttribute("latestMessages");
        assertThat(latestMessages).containsEntry(2L, latest);
    }

    @Test
    void messageListThrowsWhenCurrentUserHasNoMatchingLocalRecord() {
        when(userRepository.findByOktaUserId("okta-missing")).thenReturn(Optional.empty());
        authenticateAs("okta-missing");

        Model model = new ExtendedModelMap();

        Exception thrown = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> messageController.messages(model)
        );
        assertThat(thrown).hasMessage("User not found in local database");
    }

    // ---------- GET /messages/{receiverId} ----------

    @Test
    void conversationThrowsWhenReceiverDoesNotExist() {
        User currentUser = user(1L, "okta-1", "me");
        stubCurrentUser(currentUser);
        authenticateAs("okta-1");
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Model model = new ExtendedModelMap();

        Exception thrown = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> messageController.index(model, 99L)
        );
        assertThat(thrown).hasMessage("Receiver not found");
    }

    @Test
    void conversationIsShownWhenUsersAreFriends() {
        User currentUser = user(1L, "okta-1", "me");
        User receiver = user(2L, "okta-2", "friend");
        stubCurrentUser(currentUser);
        authenticateAs("okta-1");
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(friendRepository.existsByRequesterAndReceiverAndStatus(currentUser, receiver, Friend.Status.ACCEPTED))
                .thenReturn(true);

        Message message = new Message(1L, 2L, "hello", null, null, null, null);
        when(messageRepository.findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByCreatedAtAsc(
                1L, 2L, 2L, 1L)).thenReturn(List.of(message));

        Model model = new ExtendedModelMap();
        String view = messageController.index(model, 2L);

        assertThat(view).isEqualTo("messages/index");
        assertThat(model.getAttribute("messages")).isEqualTo(List.of(message));
        assertThat(model.getAttribute("receiver")).isEqualTo(receiver);
        assertThat(model.getAttribute("submittedMessage")).isInstanceOf(Message.class);
    }

    @Test
    void conversationRedirectsToFriendsWithPendingMessageWhenRequestIsPending() throws Exception {
        User currentUser = user(1L, "okta-1", "me");
        User receiver = user(2L, "okta-2", "notYetFriend");
        stubCurrentUser(currentUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(friendRepository.existsByRequesterAndReceiverAndStatus(currentUser, receiver, Friend.Status.ACCEPTED))
                .thenReturn(false);
        when(friendRepository.existsByReceiverAndRequesterAndStatus(currentUser, receiver, Friend.Status.ACCEPTED))
                .thenReturn(false);
        when(friendRepository.existsByRequesterAndReceiverAndStatus(currentUser, receiver, Friend.Status.PENDING))
                .thenReturn(true);

        mockMvc.perform(get("/messages/2")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/friends?message=pending"));

        verify(messageRepository, never())
                .findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByCreatedAtAsc(any(), any(), any(), any());
    }

    @Test
    void conversationRedirectsToFriendsWhenUsersAreNotFriendsAtAll() throws Exception {
        User currentUser = user(1L, "okta-1", "me");
        User receiver = user(2L, "okta-2", "stranger");
        stubCurrentUser(currentUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(friendRepository.existsByRequesterAndReceiverAndStatus(any(), any(), any())).thenReturn(false);
        when(friendRepository.existsByReceiverAndRequesterAndStatus(any(), any(), any())).thenReturn(false);

        mockMvc.perform(get("/messages/2")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/friends"));
    }

    // ---------- POST /messages/{receiverId} ----------

    @Test
    void createThrowsWhenReceiverDoesNotExist() {
        User currentUser = user(1L, "okta-1", "me");
        stubCurrentUser(currentUser);
        authenticateAs("okta-1");
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Message submitted = new Message();
        submitted.setContent("hi");

        Exception thrown = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> messageController.create(99L, submitted)
        );
        assertThat(thrown).hasMessage("Receiver not found");
        verify(messageRepository, never()).save(any());
    }

    @Test
    void createSavesMessageAndNotificationWhenUsersAreFriends() throws Exception {
        User currentUser = user(1L, "okta-1", "me");
        User receiver = user(2L, "okta-2", "friend");
        stubCurrentUser(currentUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(friendRepository.existsByRequesterAndReceiverAndStatus(currentUser, receiver, Friend.Status.ACCEPTED))
                .thenReturn(true);

        Message saved = new Message(1L, 2L, "hi there", null, null, null, null);
        saved.setId(42L);
        when(messageRepository.save(any(Message.class))).thenReturn(saved);

        mockMvc.perform(post("/messages/2")
                        .param("content", "hi there")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/messages/2"));

        verify(messageRepository).save(argThat(m ->
                m.getSenderId().equals(1L) && m.getReceiverId().equals(2L)
                        && m.getContent().equals("hi there") && !m.isRead()));
        verify(notificationRepository).save(argThat(n ->
                n.getUserId().equals(2L) && n.getActorId().equals(1L)
                        && n.getType().equals("NEW_MESSAGE")
                        && n.getRelatedId().equals(42L)));
    }

    @Test
    void createRedirectsToFriendsWhenUsersAreNotFriends() throws Exception {
        User currentUser = user(1L, "okta-1", "me");
        User receiver = user(2L, "okta-2", "stranger");
        stubCurrentUser(currentUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(friendRepository.existsByRequesterAndReceiverAndStatus(any(), any(), any())).thenReturn(false);
        when(friendRepository.existsByReceiverAndRequesterAndStatus(any(), any(), any())).thenReturn(false);

        mockMvc.perform(post("/messages/2")
                        .param("content", "hi there")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/friends"));

        verify(messageRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createRedirectsBackToConversationWhenMessageAndSongAreBothEmpty() throws Exception {
        User currentUser = user(1L, "okta-1", "me");
        User receiver = user(2L, "okta-2", "friend");
        stubCurrentUser(currentUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(friendRepository.existsByRequesterAndReceiverAndStatus(currentUser, receiver, Friend.Status.ACCEPTED))
                .thenReturn(true);

        mockMvc.perform(post("/messages/2")
                        .param("content", "   ")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/messages/2"));

        verify(messageRepository, never()).save(any());
    }

    @Test
    void createSavesSongDetailsWithBlankContentWhenSongIsShared() throws Exception {
        User currentUser = user(1L, "okta-1", "me");
        User receiver = user(2L, "okta-2", "friend");
        stubCurrentUser(currentUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(friendRepository.existsByReceiverAndRequesterAndStatus(currentUser, receiver, Friend.Status.ACCEPTED))
                .thenReturn(true);

        Message saved = new Message(1L, 2L, "", "Song", "Artist", "img", "preview");
        saved.setId(43L);
        when(messageRepository.save(any(Message.class))).thenReturn(saved);

        mockMvc.perform(post("/messages/2")
                        .param("songTitle", "Song")
                        .param("songArtist", "Artist")
                        .param("songImageUrl", "img")
                        .param("songPreviewUrl", "preview")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/messages/2"));

        verify(messageRepository).save(argThat(m ->
                m.getContent().isEmpty()
                        && m.getSongTitle().equals("Song")
                        && m.getSongArtist().equals("Artist")));
    }
}
