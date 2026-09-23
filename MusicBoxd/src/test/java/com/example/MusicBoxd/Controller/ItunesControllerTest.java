package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Config.SecurityConfig;
import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.Artist;
import com.example.MusicBoxd.Model.Song;
import com.example.MusicBoxd.Repository.AlbumRepository;
import com.example.MusicBoxd.Repository.ArtistRepository;
import com.example.MusicBoxd.Repository.SongRepository;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.api.itunes.ItunesService;
import com.example.MusicBoxd.api.itunes.ItunesTrack;
import com.example.MusicBoxd.api.itunes.ItunesTrackResponse;
import com.example.MusicBoxd.api.lastfm.LastFmAlbum;
import com.example.MusicBoxd.api.lastfm.LastFmAlbumResponse;
import com.example.MusicBoxd.api.lastfm.LastFmImage;
import com.example.MusicBoxd.api.lastfm.LastFmSearchAlbum;
import com.example.MusicBoxd.api.lastfm.LastFmSearchResponse;
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

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ItunesController.class)
@Import({SecurityConfig.class, TestOAuth2Config.class})
@TestPropertySource(properties = {
        "okta.oauth2.issuer=https://test-issuer.invalid/",
        "okta.oauth2.client-id=test-client-id"
})
class ItunesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ItunesService itunesService;

    @MockitoBean
    private LastFmService lastFmService;

    @MockitoBean
    private ArtistRepository artistRepository;

    @MockitoBean
    private AlbumRepository albumRepository;

    @MockitoBean
    private SongRepository songRepository;

    @Test
    void redirectsUnauthenticatedUserToLogin() throws Exception {
        mockMvc.perform(get("/search").param("query", "Radiohead"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        startsWith("/oauth2/authorization/" + TestOAuth2Config.REGISTRATION_ID)
                ));
    }

    @Test
    void searchAlbumsReturnsMatchesWhenLastFmHasResults() throws Exception {
        LastFmSearchAlbum album = new LastFmSearchAlbum();
        album.setName("OK Computer");
        album.setArtist("Radiohead");

        LastFmSearchResponse.AlbumMatches matches = new LastFmSearchResponse.AlbumMatches();
        matches.setAlbum(List.of(album));

        LastFmSearchResponse.Results results = new LastFmSearchResponse.Results();
        results.setAlbummatches(matches);

        LastFmSearchResponse response = new LastFmSearchResponse();
        response.setResults(results);

        when(lastFmService.searchAlbums("Radiohead")).thenReturn(response);

        mockMvc.perform(get("/search")
                        .param("query", "Radiohead")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("album-search"))
                .andExpect(model().attribute("query", "Radiohead"))
                .andExpect(model().attribute("albums", List.of(album)));
    }

    @Test
    void searchAlbumsReturnsEmptyListWhenLastFmResponseIsNull() throws Exception {
        when(lastFmService.searchAlbums("unknown")).thenReturn(null);

        mockMvc.perform(get("/search")
                        .param("query", "unknown")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("album-search"))
                .andExpect(model().attribute("albums", List.of()));
    }

    @Test
    void searchTracksReturnsResultsFromItunes() throws Exception {
        ItunesTrack track = new ItunesTrack();
        track.setTrackName("Paranoid Android");

        ItunesTrackResponse response = new ItunesTrackResponse();
        response.setResults(List.of(track));

        when(itunesService.searchTracks("Radiohead")).thenReturn(response);

        mockMvc.perform(get("/search/tracks")
                        .param("query", "Radiohead")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("track-search"))
                .andExpect(model().attribute("tracks", List.of(track)))
                .andExpect(model().attribute("query", "Radiohead"));
    }

    @Test
    void saveAlbumCreatesNewArtistAndAlbumWhenNoneExist() throws Exception {
        when(artistRepository.findByNameIgnoreCase("Radiohead")).thenReturn(Optional.empty());
        when(artistRepository.save(any(Artist.class))).thenAnswer(invocation -> {
            Artist a = invocation.getArgument(0);
            a.setId(5L);
            return a;
        });
        when(albumRepository.findByExternalId("123")).thenReturn(Optional.empty());
        when(albumRepository.save(any(Album.class))).thenAnswer(invocation -> {
            Album a = invocation.getArgument(0);
            a.setId(9L);
            return a;
        });
        when(itunesService.getAlbumTracks(123L)).thenReturn(null);

        mockMvc.perform(get("/album/save")
                        .param("collectionId", "123")
                        .param("artistName", "Radiohead")
                        .param("albumName", "OK Computer")
                        .param("artworkUrl", "http://art")
                        .param("releaseDate", "1997-05-21")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/albums/9"));

        verify(artistRepository).save(any(Artist.class));
        verify(albumRepository).save(any(Album.class));
    }

    @Test
    void saveAlbumUpdatesExistingAlbumInsteadOfCreatingDuplicate() throws Exception {
        Artist existingArtist = new Artist("Radiohead");
        existingArtist.setId(5L);
        when(artistRepository.findByNameIgnoreCase("Radiohead")).thenReturn(Optional.of(existingArtist));

        Album existingAlbum = new Album("123", 5L, "Old Title", (short) 1990, null);
        existingAlbum.setId(9L);
        when(albumRepository.findByExternalId("123")).thenReturn(Optional.of(existingAlbum));
        when(albumRepository.save(any(Album.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(itunesService.getAlbumTracks(123L)).thenReturn(null);

        mockMvc.perform(get("/album/save")
                        .param("collectionId", "123")
                        .param("artistName", "Radiohead")
                        .param("albumName", "OK Computer")
                        .param("artworkUrl", "http://art")
                        .param("releaseDate", "1997-05-21")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/albums/9"));

        verify(artistRepository, never()).save(any(Artist.class));
        verify(albumRepository).save(any(Album.class));
    }

    @Test
    void saveAlbumSavesNewSongsFromItunesTracksWithoutDuplicates() throws Exception {
        Artist artist = new Artist("Radiohead");
        artist.setId(5L);
        when(artistRepository.findByNameIgnoreCase("Radiohead")).thenReturn(Optional.of(artist));

        when(albumRepository.findByExternalId("123")).thenReturn(Optional.empty());
        when(albumRepository.save(any(Album.class))).thenAnswer(invocation -> {
            Album a = invocation.getArgument(0);
            a.setId(9L);
            return a;
        });

        ItunesTrack existingTrack = new ItunesTrack();
        existingTrack.setTrackId(1L);
        existingTrack.setTrackName("Already Saved");

        ItunesTrack newTrack = new ItunesTrack();
        newTrack.setTrackId(2L);
        newTrack.setTrackName("New Track");

        ItunesTrack noIdTrack = new ItunesTrack();
        noIdTrack.setTrackId(null);

        ItunesTrackResponse trackResponse = new ItunesTrackResponse();
        trackResponse.setResults(List.of(existingTrack, newTrack, noIdTrack));

        when(itunesService.getAlbumTracks(123L)).thenReturn(trackResponse);
        when(songRepository.findByExternalId("1")).thenReturn(Optional.of(new Song()));
        when(songRepository.findByExternalId("2")).thenReturn(Optional.empty());

        mockMvc.perform(get("/album/save")
                        .param("collectionId", "123")
                        .param("artistName", "Radiohead")
                        .param("albumName", "OK Computer")
                        .param("artworkUrl", "http://art")
                        .param("releaseDate", "1997-05-21")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection());

        verify(songRepository, never()).save(argThatExternalId("1"));
        verify(songRepository, times(1)).save(any(Song.class));
    }

    private Song argThatExternalId(String externalId) {
        return org.mockito.ArgumentMatchers.argThat(song -> song != null && externalId.equals(song.getExternalId()));
    }

    @Test
    void getAlbumFromArtistReturnsExistingAlbumWhenTitleAlreadySaved() throws Exception {
        Artist artist = new Artist("Radiohead");
        artist.setId(5L);
        when(artistRepository.findByNameIgnoreCase("Radiohead")).thenReturn(Optional.of(artist));

        Album existingAlbum = new Album("ext", 5L, "OK Computer", (short) 1997, "http://existing-art");
        existingAlbum.setId(42L);
        when(albumRepository.findByArtistId(5L)).thenReturn(List.of(existingAlbum));

        mockMvc.perform(get("/album/from-artist")
                        .param("artistName", "Radiohead")
                        .param("albumName", "OK Computer")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/albums/42"));

        verify(albumRepository, never()).save(any(Album.class));
        verify(lastFmService, never()).getAlbumInfo(any(), any());
    }

    @Test
    void getAlbumFromArtistBackfillsArtworkWhenExistingAlbumHasNone() throws Exception {
        Artist artist = new Artist("Radiohead");
        artist.setId(5L);
        when(artistRepository.findByNameIgnoreCase("Radiohead")).thenReturn(Optional.of(artist));

        Album existingAlbum = new Album("ext", 5L, "OK Computer", (short) 1997, "");
        existingAlbum.setId(42L);
        when(albumRepository.findByArtistId(5L)).thenReturn(List.of(existingAlbum));

        LastFmImage image = new LastFmImage();
        image.setText("http://lastfm-art");
        LastFmAlbum lastFmAlbum = new LastFmAlbum();
        lastFmAlbum.setImage(List.of(image));
        LastFmAlbumResponse lastFmResponse = new LastFmAlbumResponse();
        lastFmResponse.setAlbum(lastFmAlbum);

        when(lastFmService.getAlbumInfo("Radiohead", "OK Computer")).thenReturn(lastFmResponse);
        when(albumRepository.save(any(Album.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(get("/album/from-artist")
                        .param("artistName", "Radiohead")
                        .param("albumName", "OK Computer")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/albums/42"));

        verify(albumRepository).save(argThat(a -> "http://lastfm-art".equals(a.getArtworkUrl())));
    }

    private Album argThat(java.util.function.Predicate<Album> predicate) {
        return org.mockito.ArgumentMatchers.argThat(predicate::test);
    }

    @Test
    void saveAlbumIgnoresUnparseableReleaseDate() throws Exception {
        Artist radiohead = new Artist("Radiohead");
        radiohead.setId(4L);
        when(artistRepository.findByNameIgnoreCase("Radiohead")).thenReturn(Optional.of(radiohead));
        when(albumRepository.findByExternalId("123")).thenReturn(Optional.empty());
        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> {
            Album a = inv.getArgument(0);
            a.setId(9L);
            return a;
        });
        when(itunesService.getAlbumTracks(123L)).thenReturn(null);

        mockMvc.perform(get("/album/save")
                        .param("collectionId", "123")
                        .param("artistName", "Radiohead")
                        .param("albumName", "OK Computer")
                        .param("artworkUrl", "http://art")
                        .param("releaseDate", "abcd")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection());

        verify(albumRepository).save(org.mockito.ArgumentMatchers.argThat(a -> a.getReleaseYear() == null));
    }

    @Test
    void getAlbumFromArtistBackfillsArtworkFromLastAvailableImageWhenEarlierEntriesAreBlank() throws Exception {
        Artist artist = new Artist("Radiohead");
        artist.setId(5L);
        when(artistRepository.findByNameIgnoreCase("Radiohead")).thenReturn(Optional.of(artist));

        Album existingAlbum = new Album("ext", 5L, "OK Computer", (short) 1997, null);
        existingAlbum.setId(42L);
        when(albumRepository.findByArtistId(5L)).thenReturn(List.of(existingAlbum));

        LastFmImage blankImage = new LastFmImage();
        blankImage.setText("   ");
        LastFmImage goodImage = new LastFmImage();
        goodImage.setText("http://lastfm-art");
        LastFmAlbum lastFmAlbum = new LastFmAlbum();
        lastFmAlbum.setImage(List.of(blankImage, goodImage));
        LastFmAlbumResponse lastFmResponse = new LastFmAlbumResponse();
        lastFmResponse.setAlbum(lastFmAlbum);

        when(lastFmService.getAlbumInfo("Radiohead", "OK Computer")).thenReturn(lastFmResponse);
        when(albumRepository.save(any(Album.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(get("/album/from-artist")
                        .param("artistName", "Radiohead")
                        .param("albumName", "OK Computer")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/albums/42"));

        verify(albumRepository).save(argThat(a -> "http://lastfm-art".equals(a.getArtworkUrl())));
    }

    @Test
    void getAlbumFromArtistLeavesArtworkNullWhenLastFmAlbumHasNoImages() throws Exception {
        Artist artist = new Artist("Radiohead");
        artist.setId(5L);
        when(artistRepository.findByNameIgnoreCase("Radiohead")).thenReturn(Optional.of(artist));

        Album existingAlbum = new Album("ext", 5L, "OK Computer", (short) 1997, null);
        existingAlbum.setId(42L);
        when(albumRepository.findByArtistId(5L)).thenReturn(List.of(existingAlbum));

        LastFmAlbum lastFmAlbum = new LastFmAlbum();
        lastFmAlbum.setImage(List.of());
        LastFmAlbumResponse lastFmResponse = new LastFmAlbumResponse();
        lastFmResponse.setAlbum(lastFmAlbum);
        when(lastFmService.getAlbumInfo("Radiohead", "OK Computer")).thenReturn(lastFmResponse);

        mockMvc.perform(get("/album/from-artist")
                        .param("artistName", "Radiohead")
                        .param("albumName", "OK Computer")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/albums/42"));

        verify(albumRepository, never()).save(any(Album.class));
    }

    @Test
    void getAlbumFromArtistSwallowsExceptionsWhileFetchingArtworkAndStillRedirects() throws Exception {
        Artist artist = new Artist("Radiohead");
        artist.setId(5L);
        when(artistRepository.findByNameIgnoreCase("Radiohead")).thenReturn(Optional.of(artist));

        when(albumRepository.findByArtistId(5L)).thenReturn(List.of());
        when(lastFmService.getAlbumInfo("Radiohead", "Boom")).thenThrow(new RuntimeException("last.fm down"));

        Album savedAlbum = new Album("lastfm:Radiohead:Boom", 5L, "Boom", null, null);
        savedAlbum.setId(77L);
        when(albumRepository.save(any(Album.class))).thenReturn(savedAlbum);

        mockMvc.perform(get("/album/from-artist")
                        .param("artistName", "Radiohead")
                        .param("albumName", "Boom")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/albums/77"));
    }

    @Test
    void getAlbumFromArtistCreatesNewAlbumWhenNotFound() throws Exception {
        when(artistRepository.findByNameIgnoreCase("New Artist")).thenReturn(Optional.empty());
        Artist newArtist = new Artist("New Artist");
        newArtist.setId(7L);
        when(artistRepository.save(any(Artist.class))).thenReturn(newArtist);

        when(albumRepository.findByArtistId(7L)).thenReturn(List.of());
        when(lastFmService.getAlbumInfo("New Artist", "Debut")).thenReturn(null);

        Album savedAlbum = new Album("lastfm:New Artist:Debut", 7L, "Debut", null, null);
        savedAlbum.setId(55L);
        when(albumRepository.save(any(Album.class))).thenReturn(savedAlbum);

        mockMvc.perform(get("/album/from-artist")
                        .param("artistName", "New Artist")
                        .param("albumName", "Debut")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/albums/55"));
    }
}
