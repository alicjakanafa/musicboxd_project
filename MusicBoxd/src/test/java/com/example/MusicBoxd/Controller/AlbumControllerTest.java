package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Config.SecurityConfig;
import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.Artist;
import com.example.MusicBoxd.Model.Like;
import com.example.MusicBoxd.Model.ListType;
import com.example.MusicBoxd.Model.Review;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Model.UserFavouriteAlbum;
import com.example.MusicBoxd.Repository.AlbumRepository;
import com.example.MusicBoxd.Repository.ArtistRepository;
import com.example.MusicBoxd.Repository.LikeRepository;
import com.example.MusicBoxd.Repository.ListItemRepository;
import com.example.MusicBoxd.Repository.ListRepository;
import com.example.MusicBoxd.Repository.ReviewRepository;
import com.example.MusicBoxd.Repository.UserFavouriteAlbumRepository;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.api.itunes.ItunesService;
import com.example.MusicBoxd.api.itunes.ItunesTrack;
import com.example.MusicBoxd.api.itunes.ItunesTrackResponse;
import com.example.MusicBoxd.api.lastfm.LastFmAlbum;
import com.example.MusicBoxd.api.lastfm.LastFmAlbumResponse;
import com.example.MusicBoxd.api.lastfm.LastFmService;
import com.example.MusicBoxd.service.NotificationService;
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
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
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

@WebMvcTest(AlbumController.class)
@Import({SecurityConfig.class, TestOAuth2Config.class})
@TestPropertySource(properties = {
        "okta.oauth2.issuer=https://test-issuer.invalid/",
        "okta.oauth2.client-id=test-client-id"
})
class AlbumControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlbumRepository albumRepository;

    @MockitoBean
    private ArtistRepository artistRepository;

    @MockitoBean
    private UserFavouriteAlbumRepository favouriteAlbumRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private LastFmService lastFmService;

    @MockitoBean
    private ItunesService itunesService;

    @MockitoBean
    private ReviewRepository reviewRepository;

    @MockitoBean
    private LikeRepository likeRepository;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private ListRepository listRepository;

    @MockitoBean
    private ListItemRepository listItemRepository;

    private Artist artist(long id, String name) {
        Artist a = new Artist(name);
        a.setId(id);
        return a;
    }

    private Album album(long id, long artistId, String title, String externalId) {
        Album a = new Album(externalId, artistId, title, (short) 2000, "art");
        a.setId(id);
        return a;
    }

    @Test
    void redirectsUnauthenticatedUserToLogin() throws Exception {
        mockMvc.perform(get("/albums/save")
                        .param("collectionId", "1")
                        .param("artistName", "A")
                        .param("albumName", "B")
                        .param("artworkUrl", "art")
                        .param("releaseDate", "2000-01-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        startsWith("/oauth2/authorization/" + TestOAuth2Config.REGISTRATION_ID)
                ));
    }

    @Test
    void saveItunesAlbumRedirectsToExistingAlbumWhenExternalIdAlreadySaved() throws Exception {
        Album existing = album(10L, 1L, "OK Computer", "123");
        when(albumRepository.findByExternalId("123")).thenReturn(Optional.of(existing));

        mockMvc.perform(get("/albums/save")
                        .param("collectionId", "123")
                        .param("artistName", "Radiohead")
                        .param("albumName", "OK Computer")
                        .param("artworkUrl", "art")
                        .param("releaseDate", "1997-05-21")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/albums/10"));

        verify(albumRepository, never()).save(any(Album.class));
    }

    @Test
    void saveItunesAlbumCreatesArtistAndAlbumWhenNew() throws Exception {
        when(albumRepository.findByExternalId("123")).thenReturn(Optional.empty());
        when(artistRepository.findByNameIgnoreCase("Radiohead")).thenReturn(Optional.empty());
        when(artistRepository.save(any(Artist.class))).thenAnswer(inv -> {
            Artist a = inv.getArgument(0);
            a.setId(4L);
            return a;
        });
        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> {
            Album a = inv.getArgument(0);
            a.setId(99L);
            return a;
        });

        mockMvc.perform(get("/albums/save")
                        .param("collectionId", "123")
                        .param("artistName", "Radiohead")
                        .param("albumName", "OK Computer")
                        .param("artworkUrl", "art")
                        .param("releaseDate", "1997-05-21")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/albums/99"));

        verify(artistRepository).save(any(Artist.class));
        verify(albumRepository).save(argThatReleaseYear((short) 1997));
    }

    @Test
    void saveItunesAlbumHandlesBlankReleaseDate() throws Exception {
        when(albumRepository.findByExternalId("123")).thenReturn(Optional.empty());
        when(artistRepository.findByNameIgnoreCase("Radiohead")).thenReturn(Optional.of(artist(4L, "Radiohead")));
        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> {
            Album a = inv.getArgument(0);
            a.setId(99L);
            return a;
        });

        mockMvc.perform(get("/albums/save")
                        .param("collectionId", "123")
                        .param("artistName", "Radiohead")
                        .param("albumName", "OK Computer")
                        .param("artworkUrl", "art")
                        .param("releaseDate", "")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection());

        verify(albumRepository).save(argThatReleaseYear(null));
    }

    private Album argThatReleaseYear(Short year) {
        return org.mockito.ArgumentMatchers.argThat(a -> a != null
                && (year == null ? a.getReleaseYear() == null : year.equals(a.getReleaseYear())));
    }

    @Test
    void showAlbumRedirectsHomeWhenAlbumNotFound() throws Exception {
        when(albumRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/albums/1")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/"));
    }

    @Test
    void showAlbumRedirectsHomeWhenArtistIdMissing() throws Exception {
        Album album = album(1L, 2L, "Title", "ext");
        album.setArtistId(null);
        when(albumRepository.findById(1L)).thenReturn(Optional.of(album));

        mockMvc.perform(get("/albums/1")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/"));
    }

    @Test
    void showAlbumRedirectsHomeWhenArtistNotFound() throws Exception {
        Album album = album(1L, 2L, "Title", "ext");
        when(albumRepository.findById(1L)).thenReturn(Optional.of(album));
        when(artistRepository.findById(2L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/albums/1")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/"));
    }

    @Test
    void showAlbumRendersDetailsWithoutTrackPreviewsWhenNoUserRecordExists() throws Exception {
        Album album = album(1L, 2L, "OK Computer", "");
        Artist artistEntity = artist(2L, "Radiohead");

        when(albumRepository.findById(1L)).thenReturn(Optional.of(album));
        when(artistRepository.findById(2L)).thenReturn(Optional.of(artistEntity));
        when(lastFmService.getAlbumInfo("Radiohead", "OK Computer")).thenReturn(null);
        when(reviewRepository.findByAlbumIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/albums/1")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("album-profile"))
                .andExpect(model().attribute("album", album))
                .andExpect(model().attribute("artist", artistEntity))
                .andExpect(model().attribute("lastFmAlbum", (Object) null))
                .andExpect(model().attribute("alreadyInWantToListen", false))
                .andExpect(model().attribute("trackPreviews", java.util.Map.of()))
                .andExpect(model().attribute("currentUser", (Object) null));
    }

    @Test
    void showAlbumIncludesLastFmAlbumAndTrackPreviewsAndWantToListenFlagForSignedInUser() throws Exception {
        Album album = album(1L, 2L, "OK Computer", "123");
        Artist artistEntity = artist(2L, "Radiohead");

        when(albumRepository.findById(1L)).thenReturn(Optional.of(album));
        when(artistRepository.findById(2L)).thenReturn(Optional.of(artistEntity));

        LastFmAlbum lastFmAlbum = new LastFmAlbum();
        lastFmAlbum.setName("OK Computer");
        LastFmAlbumResponse lastFmResponse = new LastFmAlbumResponse();
        lastFmResponse.setAlbum(lastFmAlbum);
        when(lastFmService.getAlbumInfo("Radiohead", "OK Computer")).thenReturn(lastFmResponse);

        User user = new User("okta-1", "user", "user@example.com", "", "");
        user.setId(50L);
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(user));

        when(listRepository.findByUserIdOrderByCreatedAtDesc(50L)).thenReturn(List.of());
        com.example.MusicBoxd.Model.List wantToListenList =
                new com.example.MusicBoxd.Model.List(50L, "Want to Listen", "", ListType.WANT_TO_LISTEN);
        wantToListenList.setId(77L);
        when(listRepository.findByUserIdAndListType(50L, ListType.WANT_TO_LISTEN))
                .thenReturn(Optional.of(wantToListenList));
        when(listItemRepository.findByListIdAndAlbumId(77L, 1L))
                .thenReturn(Optional.of(new com.example.MusicBoxd.Model.ListItem()));

        ItunesTrack track = new ItunesTrack();
        track.setTrackName("Airbag");
        track.setPreviewUrl("http://preview");
        ItunesTrackResponse trackResponse = new ItunesTrackResponse();
        trackResponse.setResults(List.of(track));
        when(itunesService.getAlbumTracks(123L)).thenReturn(trackResponse);

        Review review = new Review(50L, 1L, null, "Great", "Loved it", BigDecimal.valueOf(4.5));
        review.setId(200L);
        when(reviewRepository.findByAlbumIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(review));
        when(userRepository.findByIdIn(List.of(50L))).thenReturn(List.of(user));
        when(likeRepository.countByReviewId(200L)).thenReturn(3L);
        when(likeRepository.findByUserIdAndReviewId(50L, 200L)).thenReturn(Optional.of(new Like()));

        mockMvc.perform(get("/albums/1")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("album-profile"))
                .andExpect(model().attribute("lastFmAlbum", lastFmAlbum))
                .andExpect(model().attribute("alreadyInWantToListen", true))
                .andExpect(model().attribute("trackPreviews", java.util.Map.of("Airbag", "http://preview")))
                .andExpect(model().attribute("currentUser", user));
    }

    @Test
    void toggleReviewLikeAddsLikeAndNotifiesReviewOwner() throws Exception {
        User currentUser = new User("okta-1", "liker", "liker@example.com", "", "");
        currentUser.setId(50L);
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(currentUser));

        Review review = new Review(60L, 5L, null, "H", "C", BigDecimal.ONE);
        review.setId(200L);
        when(reviewRepository.findById(200L)).thenReturn(Optional.of(review));
        when(likeRepository.findByUserIdAndReviewId(50L, 200L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/albums/reviews/200/like")
                        .with(OidcTestUsers.oidcUser("okta-1", "liker@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/albums/5"));

        verify(likeRepository).save(any(Like.class));
        verify(notificationService).notifyReviewLiked(eq(60L), eq(50L), eq(200L), eq("liker"));
    }

    @Test
    void toggleReviewLikeRemovesExistingLikeAndSkipsNotificationForOwnReview() throws Exception {
        User currentUser = new User("okta-1", "author", "author@example.com", "", "");
        currentUser.setId(60L);
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(currentUser));

        Review review = new Review(60L, 5L, null, "H", "C", BigDecimal.ONE);
        review.setId(200L);
        when(reviewRepository.findById(200L)).thenReturn(Optional.of(review));

        Like existingLike = new Like(60L, 200L, null);
        when(likeRepository.findByUserIdAndReviewId(60L, 200L)).thenReturn(Optional.of(existingLike));

        mockMvc.perform(post("/albums/reviews/200/like")
                        .with(OidcTestUsers.oidcUser("okta-1", "author@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/albums/5"));

        verify(likeRepository).delete(existingLike);
        verify(likeRepository, never()).save(any(Like.class));
        verify(notificationService, never()).notifyReviewLiked(any(), any(), any(), any());
    }

    @Test
    void addFavouriteAlbumReplacesExistingFavouriteAtSamePosition() throws Exception {
        User user = new User("okta-1", "user", "user@example.com", "", "");
        user.setId(50L);
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(user));

        when(favouriteAlbumRepository.findByUserIdAndAlbumId(50L, 1L)).thenReturn(Optional.empty());

        UserFavouriteAlbum other = new UserFavouriteAlbum(50L, 2L, 1);
        other.setId(300L);
        when(favouriteAlbumRepository.findByUserIdOrderByPositionAsc(50L)).thenReturn(List.of(other));

        mockMvc.perform(post("/albums/1/favourite")
                        .param("position", "1")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/profile/50"));

        verify(favouriteAlbumRepository).delete(other);
        verify(favouriteAlbumRepository).save(any(UserFavouriteAlbum.class));
    }

    @Test
    void removeFavouriteAlbumDeletesExistingFavourite() throws Exception {
        User user = new User("okta-1", "user", "user@example.com", "", "");
        user.setId(50L);
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(user));

        UserFavouriteAlbum favourite = new UserFavouriteAlbum(50L, 1L, 0);
        when(favouriteAlbumRepository.findByUserIdAndAlbumId(50L, 1L)).thenReturn(Optional.of(favourite));

        mockMvc.perform(post("/albums/1/favourite/remove")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/profile/50"));

        verify(favouriteAlbumRepository).delete(favourite);
    }
}
