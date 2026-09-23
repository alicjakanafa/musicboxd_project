package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Config.SecurityConfig;
import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.Artist;
import com.example.MusicBoxd.Model.Friend;
import com.example.MusicBoxd.Model.Like;
import com.example.MusicBoxd.Model.Review;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Repository.AlbumRepository;
import com.example.MusicBoxd.Repository.ArtistRepository;
import com.example.MusicBoxd.Repository.FriendRepository;
import com.example.MusicBoxd.Repository.LikeRepository;
import com.example.MusicBoxd.Repository.ReviewRepository;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.api.lastfm.LastFmAlbumResponse;
import com.example.MusicBoxd.api.lastfm.LastFmService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
        controllers = ReviewController.class,
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
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewRepository reviewRepository;

    @MockitoBean
    private AlbumRepository albumRepository;

    @MockitoBean
    private ArtistRepository artistRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private FriendRepository friendRepository;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private LastFmService lastFmService;

    @MockitoBean
    private LikeRepository likeRepository;

    private User user(long id, String oktaId, String username) {
        User user = new User(oktaId, username, username + "@example.com", "", "");
        user.setId(id);
        return user;
    }

    private Album album(long id, Long artistId, String title) {
        Album album = new Album("ext-" + id, artistId, title, (short) 2020, null);
        album.setId(id);
        return album;
    }

    // ---------------------------------------------------------------
    // GET /users/{userId}/reviews
    // ---------------------------------------------------------------

    @Test
    void redirectsUnauthenticatedRequestForUserReviewsToOAuth2Login() throws Exception {
        mockMvc.perform(get("/users/1/reviews"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.startsWith(
                                "/oauth2/authorization/" + TestOAuth2Config.REGISTRATION_ID
                        )
                ));
    }

    @Test
    void showsUserReviewsWithAssociatedAlbums() throws Exception {
        User owner = user(1L, "okta-1", "owner");
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        Review review = new Review(1L, 10L, null, null, "Great album", BigDecimal.valueOf(4.5));
        review.setId(100L);
        when(reviewRepository.findByUserId(1L)).thenReturn(List.of(review));

        Album album = album(10L, null, "Album Title");
        when(albumRepository.findById(10L)).thenReturn(Optional.of(album));

        mockMvc.perform(get("/users/1/reviews")
                        .with(OidcTestUsers.oidcUser("okta-viewer", "viewer@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("user-reviews"))
                .andExpect(model().attribute("user", owner))
                .andExpect(model().attribute("reviews", List.of(review)))
                .andExpect(model().attribute("albums", java.util.Map.of(10L, album)));
    }

    @Test
    void throwsWhenUserForReviewsDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                mockMvc.perform(get("/users/99/reviews")
                        .with(OidcTestUsers.oidcUser("okta-viewer", "viewer@example.com")))
        );
    }

    // ---------------------------------------------------------------
    // GET /reviews/{id}
    // ---------------------------------------------------------------

    /**
     * SUSPECTED PRODUCTION DEFECT: {@code ReviewController#reviewAlbum} puts the raw
     * {@link LastFmAlbumResponse} wrapper returned by {@code LastFmService#getAlbumInfo} into the
     * {@code lastFmAlbum} model attribute, instead of unwrapping it with {@code getAlbum()} the
     * way {@code ListController#getAlbum} does. The {@code reviews.html} template expects
     * {@code lastFmAlbum} to be the unwrapped {@code LastFmAlbum} and reads {@code lastFmAlbum.image}
     * directly, so whenever an album has a known artist and the Last.fm lookup succeeds, rendering
     * fails with a SpelEvaluationException ("Property or field 'image' cannot be found on object
     * of type LastFmAlbumResponse"). This test documents that current (broken) behaviour rather
     * than asserting a successful render.
     */
    @Test
    void albumReviewPageFailsToRenderWhenLastFmLookupSucceeds() {
        Album album = album(10L, 5L, "Album Title");
        when(albumRepository.findById(10L)).thenReturn(Optional.of(album));

        Artist artist = new Artist("The Artist");
        artist.setId(5L);
        when(artistRepository.findById(5L)).thenReturn(Optional.of(artist));

        LastFmAlbumResponse response = new LastFmAlbumResponse();
        when(lastFmService.getAlbumInfo("The Artist", "Album Title")).thenReturn(response);

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                mockMvc.perform(get("/reviews/10")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
        );
    }

    @Test
    void showsAlbumReviewPageWithNullArtistAndLastFmAlbumWhenAlbumHasNoArtist() throws Exception {
        Album album = album(11L, null, "No Artist Album");
        when(albumRepository.findById(11L)).thenReturn(Optional.of(album));

        mockMvc.perform(get("/reviews/11")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("reviews"))
                .andExpect(model().attribute("artist", (Object) null))
                .andExpect(model().attribute("lastFmAlbum", (Object) null));

        verify(lastFmService, never()).getAlbumInfo(any(), any());
    }

    @Test
    void showsAlbumReviewPageWithNullLastFmAlbumWhenLastFmServiceFails() throws Exception {
        Album album = album(12L, 5L, "Album Title");
        when(albumRepository.findById(12L)).thenReturn(Optional.of(album));

        Artist artist = new Artist("The Artist");
        artist.setId(5L);
        when(artistRepository.findById(5L)).thenReturn(Optional.of(artist));

        when(lastFmService.getAlbumInfo(any(), any())).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/reviews/12")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("reviews"))
                .andExpect(model().attribute("lastFmAlbum", (Object) null));
    }

    @Test
    void throwsWhenAlbumForReviewPageDoesNotExist() {
        when(albumRepository.findById(404L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                mockMvc.perform(get("/reviews/404")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
        );
    }

    // ---------------------------------------------------------------
    // POST /reviews/{id}
    // ---------------------------------------------------------------

    @Test
    void redirectsUnauthenticatedSaveReviewToOAuth2Login() throws Exception {
        mockMvc.perform(post("/reviews/10")
                        .param("rating", "4")
                        .param("content", "text"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.startsWith(
                                "/oauth2/authorization/" + TestOAuth2Config.REGISTRATION_ID
                        )
                ));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createsNewReviewAndNotifiesAcceptedFriends() throws Exception {
        Album album = album(10L, null, "Album Title");
        when(albumRepository.findById(10L)).thenReturn(Optional.of(album));

        User reviewer = user(1L, "okta-1", "reviewer");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(reviewer));

        when(reviewRepository.findByUserIdAndAlbumId(1L, 10L)).thenReturn(Optional.empty());

        Review saved = new Review(1L, 10L, null, null, "Loved it", BigDecimal.valueOf(5));
        saved.setId(500L);
        when(reviewRepository.save(any(Review.class))).thenReturn(saved);

        Friend friendshipAsRequester = new Friend(1L, 2L, "ACCEPTED");
        Friend friendshipAsReceiver = new Friend(3L, 1L, "ACCEPTED");
        Friend irrelevantFriendship = new Friend(4L, 5L, "ACCEPTED");
        when(friendRepository.findByStatus("ACCEPTED"))
                .thenReturn(List.of(friendshipAsRequester, friendshipAsReceiver, irrelevantFriendship));

        mockMvc.perform(post("/reviews/10")
                        .param("rating", "5")
                        .param("content", "Loved it")
                        .with(OidcTestUsers.oidcUser("okta-1", "reviewer@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/profile/1"));

        verify(reviewRepository).save(any(Review.class));
        verify(notificationService).notifyFriendReviewed(2L, 1L, 500L, "reviewer");
        verify(notificationService).notifyFriendReviewed(3L, 1L, 500L, "reviewer");
        verify(notificationService, times(2)).notifyFriendReviewed(anyLong(), eq(1L), eq(500L), eq("reviewer"));
    }

    @Test
    void updatesExistingReviewWithoutSendingNotifications() throws Exception {
        Album album = album(10L, null, "Album Title");
        when(albumRepository.findById(10L)).thenReturn(Optional.of(album));

        User reviewer = user(1L, "okta-1", "reviewer");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(reviewer));

        Review existing = new Review(1L, 10L, null, null, "Old content", BigDecimal.valueOf(2));
        existing.setId(500L);
        when(reviewRepository.findByUserIdAndAlbumId(1L, 10L)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/reviews/10")
                        .param("rating", "3")
                        .param("content", "Updated content")
                        .with(OidcTestUsers.oidcUser("okta-1", "reviewer@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/profile/1"));

        verify(reviewRepository).save(argThatReview(r ->
                r.getId().equals(500L)
                        && r.getContent().equals("Updated content")
                        && r.getRating().equals(BigDecimal.valueOf(3))
        ));
        verify(friendRepository, never()).findByStatus(any());
        verify(notificationService, never()).notifyFriendReviewed(any(), any(), any(), any());
    }

    private Review argThatReview(java.util.function.Predicate<Review> predicate) {
        return org.mockito.ArgumentMatchers.argThat(predicate::test);
    }

    @Test
    void throwsWhenAlbumForSaveReviewDoesNotExist() {
        when(albumRepository.findById(404L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                mockMvc.perform(post("/reviews/404")
                        .param("rating", "4")
                        .param("content", "text")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
        );
    }

    @Test
    void throwsWhenUserForSaveReviewDoesNotExist() {
        Album album = album(10L, null, "Album Title");
        when(albumRepository.findById(10L)).thenReturn(Optional.of(album));
        when(userRepository.findByOktaUserId("okta-unknown")).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                mockMvc.perform(post("/reviews/10")
                        .param("rating", "4")
                        .param("content", "text")
                        .with(OidcTestUsers.oidcUser("okta-unknown", "user@example.com")))
        );
    }

    // ---------------------------------------------------------------
    // POST /reviews/{id}/delete
    // ---------------------------------------------------------------

    @Test
    void throwsWhenUserForDeleteReviewDoesNotExist() {
        when(userRepository.findByOktaUserId("okta-unknown")).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                mockMvc.perform(post("/reviews/500/delete")
                        .with(OidcTestUsers.oidcUser("okta-unknown", "user@example.com")))
        );

        verify(reviewRepository, never()).delete(any());
    }

    @Test
    void deletesOwnReviewAndRedirectsToProfile() throws Exception {
        User owner = user(1L, "okta-1", "owner");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(owner));

        Review review = new Review(1L, 10L, null, null, "content", BigDecimal.valueOf(4));
        review.setId(500L);
        when(reviewRepository.findById(500L)).thenReturn(Optional.of(review));

        mockMvc.perform(post("/reviews/500/delete")
                        .with(OidcTestUsers.oidcUser("okta-1", "owner@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/profile/1"));

        verify(reviewRepository).delete(review);
    }

    @Test
    void refusesToDeleteAnotherUsersReview() {
        User requester = user(2L, "okta-2", "notOwner");
        when(userRepository.findByOktaUserId("okta-2")).thenReturn(Optional.of(requester));

        Review review = new Review(1L, 10L, null, null, "content", BigDecimal.valueOf(4));
        review.setId(500L);
        when(reviewRepository.findById(500L)).thenReturn(Optional.of(review));

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                mockMvc.perform(post("/reviews/500/delete")
                        .with(OidcTestUsers.oidcUser("okta-2", "notOwner@example.com")))
        );

        verify(reviewRepository, never()).delete(any());
    }

    @Test
    void throwsWhenReviewToDeleteDoesNotExist() {
        User owner = user(1L, "okta-1", "owner");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(owner));
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                mockMvc.perform(post("/reviews/999/delete")
                        .with(OidcTestUsers.oidcUser("okta-1", "owner@example.com")))
        );
    }

    // ---------------------------------------------------------------
    // GET /reviews
    // ---------------------------------------------------------------

    @Test
    void showsAllReviewsOrderedByCreatedAtWithAlbumsAndUsers() throws Exception {
        Review review = new Review(1L, 10L, null, null, "content", BigDecimal.valueOf(4));
        review.setId(500L);
        when(reviewRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(review));

        Album album = album(10L, null, "Album Title");
        when(albumRepository.findById(10L)).thenReturn(Optional.of(album));

        User reviewer = user(1L, "okta-1", "reviewer");
        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));

        mockMvc.perform(get("/reviews")
                        .with(OidcTestUsers.oidcUser("okta-viewer", "viewer@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("all-reviews"))
                .andExpect(model().attribute("reviews", List.of(review)))
                .andExpect(model().attribute("albums", java.util.Map.of(10L, album)))
                .andExpect(model().attribute("users", java.util.Map.of(1L, reviewer)));
    }

    @Test
    void showsEmptyAllReviewsPageWhenThereAreNoReviews() throws Exception {
        when(reviewRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        mockMvc.perform(get("/reviews")
                        .with(OidcTestUsers.oidcUser("okta-viewer", "viewer@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("all-reviews"))
                .andExpect(model().attribute("reviews", List.of()))
                .andExpect(model().attribute("albums", java.util.Map.of()))
                .andExpect(model().attribute("users", java.util.Map.of()));
    }

    // ---------------------------------------------------------------
    // POST /reviews/{reviewId}/like
    // ---------------------------------------------------------------

    @Test
    void likesAReviewThatHasNotBeenLikedYet() throws Exception {
        User user = user(1L, "okta-1", "liker");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(user));

        Review review = new Review(2L, 10L, null, null, "content", BigDecimal.valueOf(4));
        review.setId(500L);
        when(reviewRepository.findById(500L)).thenReturn(Optional.of(review));

        when(likeRepository.findByUserIdAndReviewId(1L, 500L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/reviews/500/like")
                        .with(OidcTestUsers.oidcUser("okta-1", "liker@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/albums/10"));

        verify(likeRepository).save(any(Like.class));
        verify(likeRepository, never()).delete(any());
    }

    @Test
    void unlikesAReviewThatWasAlreadyLiked() throws Exception {
        User user = user(1L, "okta-1", "liker");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(user));

        Review review = new Review(2L, 10L, null, null, "content", BigDecimal.valueOf(4));
        review.setId(500L);
        when(reviewRepository.findById(500L)).thenReturn(Optional.of(review));

        Like existingLike = new Like(1L, 500L, null);
        existingLike.setId(999L);
        when(likeRepository.findByUserIdAndReviewId(1L, 500L)).thenReturn(Optional.of(existingLike));

        mockMvc.perform(post("/reviews/500/like")
                        .with(OidcTestUsers.oidcUser("okta-1", "liker@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/albums/10"));

        verify(likeRepository).delete(existingLike);
        verify(likeRepository, never()).save(any());
    }

    @Test
    void throwsWhenReviewToLikeDoesNotExist() {
        User user = user(1L, "okta-1", "liker");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(user));
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                mockMvc.perform(post("/reviews/999/like")
                        .with(OidcTestUsers.oidcUser("okta-1", "liker@example.com")))
        );
    }

    @Test
    void throwsWhenUserForLikeReviewDoesNotExist() {
        when(userRepository.findByOktaUserId("okta-unknown")).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                mockMvc.perform(post("/reviews/500/like")
                        .with(OidcTestUsers.oidcUser("okta-unknown", "user@example.com")))
        );

        verify(likeRepository, never()).save(any());
        verify(likeRepository, never()).delete(any());
    }
}
