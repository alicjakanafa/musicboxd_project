package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Config.SecurityConfig;
import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.Friend;
import com.example.MusicBoxd.Model.Notification;
import com.example.MusicBoxd.Model.Review;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Repository.AlbumRepository;
import com.example.MusicBoxd.Repository.FriendRepository;
import com.example.MusicBoxd.Repository.NotificationRepository;
import com.example.MusicBoxd.Repository.ReviewRepository;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.api.itunes.ItunesAlbum;
import com.example.MusicBoxd.api.itunes.ItunesService;
import com.example.MusicBoxd.api.lastfm.LastFmArtist;
import com.example.MusicBoxd.api.lastfm.LastFmArtists;
import com.example.MusicBoxd.api.lastfm.LastFmResponse;
import com.example.MusicBoxd.api.lastfm.LastFmService;
import com.example.MusicBoxd.support.OidcTestUsers;
import com.example.MusicBoxd.support.TestOAuth2Config;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HomeController.class)
@Import({SecurityConfig.class, TestOAuth2Config.class})
@TestPropertySource(properties = {
        "okta.oauth2.issuer=https://test-issuer.invalid/",
        "okta.oauth2.client-id=test-client-id"
})
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LastFmService lastFmService;

    @MockitoBean
    private SpotifyController spotifyController;

    @MockitoBean
    private ItunesService itunesService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private FriendRepository friendRepository;

    @MockitoBean
    private ReviewRepository reviewRepository;

    @MockitoBean
    private AlbumRepository albumRepository;

    private LastFmResponse topArtistsResponse(int count) {
        List<LastFmArtist> artists = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            LastFmArtist a = new LastFmArtist();
            a.setName("Artist " + i);
            artists.add(a);
        }
        LastFmArtists lastFmArtists = new LastFmArtists();
        lastFmArtists.setArtist(artists);
        LastFmResponse response = new LastFmResponse();
        response.setArtists(lastFmArtists);
        return response;
    }

    @Test
    void redirectsUnauthenticatedUserToLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        startsWith("/oauth2/authorization/" + TestOAuth2Config.REGISTRATION_ID)
                ));
    }

    @Test
    void indexShowsTopTenArtistsAndSuggestedAlbumForAnonymousLookupWhenUserMissing() throws Exception {
        when(lastFmService.getTopArtists()).thenReturn(topArtistsResponse(15));
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.empty());

        ItunesAlbum dailyAlbum = new ItunesAlbum();
        dailyAlbum.setCollectionName("Daily Pick");
        when(itunesService.getDailyAlbum(null)).thenReturn(dailyAlbum);

        mockMvc.perform(get("/")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("suggestedAlbum", dailyAlbum))
                .andExpect(model().attribute("friendReviews", List.of()))
                .andExpect(model().attributeDoesNotExist("notifications"));
    }

    @Test
    void indexShowsNotificationsAndFriendActivityForSignedInUser() throws Exception {
        when(lastFmService.getTopArtists()).thenReturn(topArtistsResponse(10));

        User currentUser = new User("okta-1", "me", "me@example.com", "", "");
        currentUser.setId(1L);
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(currentUser));

        ItunesAlbum dailyAlbum = new ItunesAlbum();
        when(itunesService.getDailyAlbum(1L)).thenReturn(dailyAlbum);

        Notification notification = new Notification(1L, 2L, 5L, "FRIEND_REQUEST", "hi");
        when(notificationRepository.findTop6ByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(notification));
        when(notificationRepository.countByUserIdAndReadFalse(1L)).thenReturn(2L);

        Friend friendship = new Friend(1L, 2L, "ACCEPTED");
        when(friendRepository.findByStatus("ACCEPTED")).thenReturn(List.of(friendship));

        User friend = new User("okta-2", "friend", "friend@example.com", "", "");
        friend.setId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(friend));

        Review friendReview = new Review(2L, 8L, null, "Nice", "Body", BigDecimal.ONE);
        friendReview.setId(500L);
        friendReview.setCreatedAt(LocalDateTime.now());
        when(reviewRepository.findByUserIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(friendReview));

        Album friendAlbum = new Album("ext", 3L, "Friend Album", (short) 2020, "http://art");
        friendAlbum.setId(8L);
        when(albumRepository.findById(8L)).thenReturn(Optional.of(friendAlbum));

        mockMvc.perform(get("/")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("notifications", List.of(notification)))
                .andExpect(model().attribute("unreadNotificationCount", 2L))
                .andExpect(model().attribute("currentUser", currentUser))
                .andExpect(model().attribute("friendReviews", List.of(friendReview)))
                .andExpect(model().attribute("friendReviewUsers", java.util.Map.of(2L, friend)))
                .andExpect(model().attribute("friendReviewAlbums", java.util.Map.of(8L, friendAlbum)))
                .andExpect(model().attribute("friendReviewArtwork", java.util.Map.of(8L, "http://art")));
    }

    @Test
    void indexIncludesSpotifyDataWhenSessionHasAnAccessToken() throws Exception {
        when(lastFmService.getTopArtists()).thenReturn(topArtistsResponse(10));
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.empty());
        when(itunesService.getDailyAlbum(null)).thenReturn(new ItunesAlbum());

        jakarta.servlet.http.HttpSession session =
                mockMvc.perform(get("/")
                                .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                        .andReturn().getRequest().getSession();
        session.setAttribute("spotifyAccessToken", "token-123");

        mockMvc.perform(get("/")
                        .session((org.springframework.mock.web.MockHttpSession) session)
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));

        org.mockito.Mockito.verify(spotifyController)
                .getSpotifyData(org.mockito.ArgumentMatchers.eq("token-123"), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void indexTruncatesFriendReviewsToTenAndSkipsReviewsWithMissingAlbums() throws Exception {
        when(lastFmService.getTopArtists()).thenReturn(topArtistsResponse(10));

        User currentUser = new User("okta-1", "me", "me@example.com", "", "");
        currentUser.setId(1L);
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(currentUser));
        when(itunesService.getDailyAlbum(1L)).thenReturn(new ItunesAlbum());
        when(notificationRepository.findTop6ByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(notificationRepository.countByUserIdAndReadFalse(1L)).thenReturn(0L);

        // currentUser is the receiver in this friendship, exercising the "else if" branch.
        Friend friendship = new Friend(2L, 1L, "ACCEPTED");
        when(friendRepository.findByStatus("ACCEPTED")).thenReturn(List.of(friendship));

        List<Review> reviews = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            // Every review points at an album id that the repository does not know about,
            // exercising the "album not found -> skip" branch for each entry.
            Review review = new Review(2L, 900L + i, null, "H" + i, "C" + i, BigDecimal.ONE);
            review.setId((long) (600 + i));
            review.setCreatedAt(LocalDateTime.now().minusMinutes(i));
            reviews.add(review);
            when(albumRepository.findById(900L + i)).thenReturn(Optional.empty());
        }

        when(reviewRepository.findByUserIdOrderByCreatedAtDesc(2L)).thenReturn(reviews);
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("friendReviews", org.hamcrest.Matchers.hasSize(10)))
                .andExpect(model().attribute("friendReviewAlbums", java.util.Map.of()));
    }

    @Test
    void profileThrowsWhenAuthenticatedPrincipalHasNoMatchingUserRecord() {
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.empty());

        jakarta.servlet.ServletException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        jakarta.servlet.ServletException.class,
                        () -> mockMvc.perform(get("/profile")
                                .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                );

        org.junit.jupiter.api.Assertions.assertTrue(
                exception.getCause() instanceof RuntimeException
                        && "User not found".equals(exception.getCause().getMessage())
        );
    }

    @Test
    void profileRedirectsToOwnProfilePageWhenSignedIn() throws Exception {
        User currentUser = new User("okta-1", "me", "me@example.com", "", "");
        currentUser.setId(42L);
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(currentUser));

        mockMvc.perform(get("/profile")
                        .with(OidcTestUsers.oidcUser("okta-1", "me@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/profile/42"));
    }
}
