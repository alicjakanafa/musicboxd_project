package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Config.SecurityConfig;
import com.example.MusicBoxd.Model.Artist;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Model.UserFavouriteArtist;
import com.example.MusicBoxd.Repository.ArtistRepository;
import com.example.MusicBoxd.Repository.UserFavouriteArtistRepository;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.api.itunes.ItunesService;
import com.example.MusicBoxd.api.itunes.ItunesTrack;
import com.example.MusicBoxd.api.itunes.ItunesTrackResponse;
import com.example.MusicBoxd.api.lastfm.LastFmArtist;
import com.example.MusicBoxd.api.lastfm.LastFmArtistResponse;
import com.example.MusicBoxd.api.lastfm.LastFmService;
import com.example.MusicBoxd.api.lastfm.LastFmStats;
import com.example.MusicBoxd.api.lastfm.LastFmTopAlbum;
import com.example.MusicBoxd.api.lastfm.LastFmTopAlbums;
import com.example.MusicBoxd.api.lastfm.LastFmTopAlbumsResponse;
import com.example.MusicBoxd.api.ticketmaster.Concert;
import com.example.MusicBoxd.api.ticketmaster.TicketmasterService;
import com.example.MusicBoxd.support.OidcTestUsers;
import com.example.MusicBoxd.support.TestOAuth2Config;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ArtistController.class)
@Import({SecurityConfig.class, TestOAuth2Config.class})
@TestPropertySource(properties = {
        "okta.oauth2.issuer=https://test-issuer.invalid/",
        "okta.oauth2.client-id=test-client-id"
})
class ArtistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistRepository artistRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserFavouriteArtistRepository favouriteArtistRepository;

    @MockitoBean
    private LastFmService lastFmService;

    @MockitoBean
    private TicketmasterService ticketmasterService;

    @MockitoBean
    private ItunesService itunesService;

    private Artist artist(long id, String name) {
        Artist a = new Artist(name);
        a.setId(id);
        return a;
    }

    @Test
    void redirectsUnauthenticatedUserToLogin() throws Exception {
        mockMvc.perform(get("/artists/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        startsWith("/oauth2/authorization/" + TestOAuth2Config.REGISTRATION_ID)
                ));
    }

    @Test
    void showArtistRedirectsHomeWhenArtistNotFound() throws Exception {
        when(artistRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/artists/1")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/"));
    }

    @Test
    void showArtistRendersProfileWithListenersAlbumsAndConcertsForAnonymousUser() throws Exception {
        Artist radiohead = artist(1L, "Radiohead");
        when(artistRepository.findById(1L)).thenReturn(Optional.of(radiohead));

        LastFmArtist lastFmArtist = new LastFmArtist();
        lastFmArtist.setName("Radiohead");
        LastFmStats stats = new LastFmStats();
        stats.setListeners("123456");
        lastFmArtist.setStats(stats);
        LastFmArtistResponse artistResponse = new LastFmArtistResponse();
        artistResponse.setArtist(lastFmArtist);
        when(lastFmService.getArtistInfo("Radiohead")).thenReturn(artistResponse);

        LastFmTopAlbum topAlbum = new LastFmTopAlbum();
        topAlbum.setName("OK Computer");
        LastFmTopAlbums topAlbums = new LastFmTopAlbums();
        topAlbums.setAlbum(List.of(topAlbum));
        LastFmTopAlbumsResponse albumsResponse = new LastFmTopAlbumsResponse();
        albumsResponse.setTopalbums(topAlbums);
        when(lastFmService.getArtistAlbums("Radiohead")).thenReturn(albumsResponse);

        ItunesTrack track = new ItunesTrack();
        track.setPreviewUrl("http://preview");
        ItunesTrackResponse trackResponse = new ItunesTrackResponse();
        trackResponse.setResults(List.of(track));
        when(itunesService.searchTracks("Radiohead")).thenReturn(trackResponse);

        when(ticketmasterService.getAttractionId("Radiohead")).thenReturn("attraction-1");
        Concert concert = new Concert("Radiohead Live", "2026-01-01", "Venue", "City", "http://tickets");
        when(ticketmasterService.getShowsByAttractionId("attraction-1")).thenReturn(List.of(concert));

        mockMvc.perform(get("/artists/1")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("artist-profile"))
                .andExpect(model().attribute("artist", radiohead))
                .andExpect(model().attribute("isFavourite", false))
                .andExpect(model().attribute("listeners", "123456"))
                .andExpect(model().attribute("albums", List.of(topAlbum)))
                .andExpect(model().attribute("popularSingles", List.of(track)))
                .andExpect(model().attribute("concerts", List.of(concert)));
    }

    @Test
    void showArtistMarksAsFavouriteWhenUserHasFavourited() throws Exception {
        Artist radiohead = artist(1L, "Radiohead");
        when(artistRepository.findById(1L)).thenReturn(Optional.of(radiohead));

        User user = new User("okta-1", "user", "user@example.com", "", "");
        user.setId(9L);
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(user));
        when(favouriteArtistRepository.existsByUserIdAndArtistId(9L, 1L)).thenReturn(true);

        when(lastFmService.getArtistInfo("Radiohead")).thenReturn(null);
        when(lastFmService.getArtistAlbums("Radiohead")).thenReturn(null);
        when(itunesService.searchTracks("Radiohead")).thenReturn(null);
        when(ticketmasterService.getAttractionId("Radiohead")).thenReturn(null);

        mockMvc.perform(get("/artists/1")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("isFavourite", true))
                .andExpect(model().attribute("listeners", (Object) null))
                .andExpect(model().attribute("albums", List.of()))
                .andExpect(model().attribute("popularSingles", List.of()))
                .andExpect(model().attribute("concerts", List.of()));

        verify(ticketmasterService, never()).getShowsByAttractionId(any());
    }

    @Test
    void favouriteArtistSavesFavouriteWhenNotAlreadyFavourited() throws Exception {
        User user = new User("okta-1", "user", "user@example.com", "", "");
        user.setId(9L);
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(user));

        Artist radiohead = artist(1L, "Radiohead");
        when(artistRepository.findById(1L)).thenReturn(Optional.of(radiohead));
        when(favouriteArtistRepository.existsByUserIdAndArtistId(9L, 1L)).thenReturn(false);

        mockMvc.perform(post("/artists/1/favourite")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/artists/1"));

        verify(favouriteArtistRepository).save(any(UserFavouriteArtist.class));
    }

    @Test
    void favouriteArtistDoesNotDuplicateWhenAlreadyFavourited() throws Exception {
        User user = new User("okta-1", "user", "user@example.com", "", "");
        user.setId(9L);
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(user));

        Artist radiohead = artist(1L, "Radiohead");
        when(artistRepository.findById(1L)).thenReturn(Optional.of(radiohead));
        when(favouriteArtistRepository.existsByUserIdAndArtistId(9L, 1L)).thenReturn(true);

        mockMvc.perform(post("/artists/1/favourite")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/artists/1"));

        verify(favouriteArtistRepository, never()).save(any(UserFavouriteArtist.class));
    }

    @Test
    void unfavouriteArtistDeletesTheFavourite() throws Exception {
        User user = new User("okta-1", "user", "user@example.com", "", "");
        user.setId(9L);
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/artists/1/unfavourite")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/artists/1"));

        verify(favouriteArtistRepository).deleteByUserIdAndArtistId(9L, 1L);
    }

    @Test
    void showArtistByNameRedirectsToExistingArtist() throws Exception {
        Artist radiohead = artist(1L, "Radiohead");
        when(artistRepository.findByNameIgnoreCase("Radiohead")).thenReturn(Optional.of(radiohead));

        mockMvc.perform(get("/artists/from-name")
                        .param("name", "Radiohead")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/artists/1"));

        verify(artistRepository, never()).save(any(Artist.class));
    }

    @Test
    void showArtistByNameCreatesArtistWhenNotFound() throws Exception {
        when(artistRepository.findByNameIgnoreCase("New Artist")).thenReturn(Optional.empty());
        Artist saved = artist(2L, "New Artist");
        when(artistRepository.save(any(Artist.class))).thenReturn(saved);

        mockMvc.perform(get("/artists/from-name")
                        .param("name", "New Artist")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/artists/2"));
    }

    @Test
    void showAllAlbumsRedirectsHomeWhenArtistNotFound() throws Exception {
        when(artistRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/artists/1/albums")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/"));
    }

    @Test
    void showAllAlbumsRendersAlbumListFromLastFm() throws Exception {
        Artist radiohead = artist(1L, "Radiohead");
        when(artistRepository.findById(1L)).thenReturn(Optional.of(radiohead));

        LastFmTopAlbum topAlbum = new LastFmTopAlbum();
        topAlbum.setName("OK Computer");
        LastFmTopAlbums topAlbums = new LastFmTopAlbums();
        topAlbums.setAlbum(List.of(topAlbum));
        LastFmTopAlbumsResponse response = new LastFmTopAlbumsResponse();
        response.setTopalbums(topAlbums);
        when(lastFmService.getArtistAlbums("Radiohead")).thenReturn(response);

        mockMvc.perform(get("/artists/1/albums")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("artist-albums"))
                .andExpect(model().attribute("artist", radiohead))
                .andExpect(model().attribute("albums", List.of(topAlbum)));
    }

    @Test
    void showAllAlbumsReturnsEmptyListWhenLastFmResponseIsNull() throws Exception {
        Artist radiohead = artist(1L, "Radiohead");
        when(artistRepository.findById(1L)).thenReturn(Optional.of(radiohead));
        when(lastFmService.getArtistAlbums("Radiohead")).thenReturn(null);

        mockMvc.perform(get("/artists/1/albums")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("albums", List.of()));
    }
}
