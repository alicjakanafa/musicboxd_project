package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Config.SecurityConfig;
import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.Artist;
import com.example.MusicBoxd.Model.ListItem;
import com.example.MusicBoxd.Model.ListType;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Repository.AlbumRepository;
import com.example.MusicBoxd.Repository.ArtistRepository;
import com.example.MusicBoxd.Repository.ListItemRepository;
import com.example.MusicBoxd.Repository.ListRepository;
import com.example.MusicBoxd.Repository.UserRepository;
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
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

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

@WebMvcTest(
        controllers = ListController.class,
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
class ListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListRepository listRepository;

    @MockitoBean
    private ListItemRepository listItemRepository;

    @MockitoBean
    private AlbumRepository albumRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private LastFmService lastFmService;

    @MockitoBean
    private ArtistRepository artistRepository;

    private User user(long id, String oktaId) {
        User user = new User(oktaId, "user-" + id, oktaId + "@example.com", "", "");
        user.setId(id);
        return user;
    }

    private com.example.MusicBoxd.Model.List list(long id, long userId, ListType type) {
        com.example.MusicBoxd.Model.List list =
                new com.example.MusicBoxd.Model.List(userId, "My List", "description", type);
        list.setId(id);
        return list;
    }

    private Album album(long id, Long artistId, String title) {
        Album album = new Album("ext-" + id, artistId, title, (short) 2020, null);
        album.setId(id);
        return album;
    }

    // ---------------------------------------------------------------
    // GET /lists
    // ---------------------------------------------------------------

    @Test
    void redirectsUnauthenticatedRequestForListsToOAuth2Login() throws Exception {
        mockMvc.perform(get("/lists"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.startsWith(
                                "/oauth2/authorization/" + TestOAuth2Config.REGISTRATION_ID
                        )
                ));
    }

    @Test
    void returnsUnauthorizedWhenAuthenticatedOktaUserHasNoMatchingLocalUser() throws Exception {
        when(userRepository.findByOktaUserId("okta-orphaned")).thenReturn(Optional.empty());

        mockMvc.perform(get("/lists")
                        .with(OidcTestUsers.oidcUser("okta-orphaned", "orphan@example.com")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void showsListsOwnedByTheCurrentUser() throws Exception {
        User owner = user(1L, "okta-1");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(owner));

        com.example.MusicBoxd.Model.List list = list(10L, 1L, ListType.CUSTOM);
        when(listRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(list));

        mockMvc.perform(get("/lists")
                        .with(OidcTestUsers.oidcUser("okta-1", "owner@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("placeholder-lists"))
                .andExpect(model().attribute("lists", List.of(list)));
    }

    // ---------------------------------------------------------------
    // POST /lists
    // ---------------------------------------------------------------

    @Test
    void createsANewCustomListForTheCurrentUser() throws Exception {
        User owner = user(1L, "okta-1");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(owner));

        mockMvc.perform(post("/lists")
                        .param("title", "Favourite Albums")
                        .param("description", "My favourites")
                        .with(OidcTestUsers.oidcUser("okta-1", "owner@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/lists"));

        verify(listRepository).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getUserId().equals(1L)
                        && saved.getTitle().equals("Favourite Albums")
                        && saved.getDescription().equals("My favourites")
                        && saved.getListType() == ListType.CUSTOM
        ));
    }

    // ---------------------------------------------------------------
    // GET /lists/{id}
    // ---------------------------------------------------------------

    @Test
    void showsOwnedListWithItsAlbums() throws Exception {
        User owner = user(1L, "okta-1");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(owner));

        com.example.MusicBoxd.Model.List list = list(10L, 1L, ListType.CUSTOM);
        when(listRepository.findById(10L)).thenReturn(Optional.of(list));

        ListItem item = new ListItem(10L, 20L, null, 1);
        when(listItemRepository.findByListIdOrderByPositionAsc(10L)).thenReturn(List.of(item));

        Album album = album(20L, null, "Album Title");
        when(albumRepository.findById(20L)).thenReturn(Optional.of(album));

        mockMvc.perform(get("/lists/10")
                        .with(OidcTestUsers.oidcUser("okta-1", "owner@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("placeholder-list-items"))
                .andExpect(model().attribute("list", list))
                .andExpect(model().attribute("items", List.of(item)))
                .andExpect(model().attribute("albums", java.util.Map.of(20L, album)));
    }

    @Test
    void forbidsViewingAnotherUsersList() {
        User requester = user(2L, "okta-2");
        when(userRepository.findByOktaUserId("okta-2")).thenReturn(Optional.of(requester));

        com.example.MusicBoxd.Model.List list = list(10L, 1L, ListType.CUSTOM);
        when(listRepository.findById(10L)).thenReturn(Optional.of(list));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> mockMvc.perform(get("/lists/10")
                        .with(OidcTestUsers.oidcUser("okta-2", "requester@example.com")))
                .andExpect(status().isForbidden()));
    }

    @Test
    void throwsWhenShowingAListThatDoesNotExist() {
        User owner = user(1L, "okta-1");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(owner));
        when(listRepository.findById(999L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                mockMvc.perform(get("/lists/999")
                        .with(OidcTestUsers.oidcUser("okta-1", "owner@example.com")))
        );
    }

    // ---------------------------------------------------------------
    // GET /lists/{id}/search
    // ---------------------------------------------------------------

    @Test
    void returnsSearchResultsFragmentForOwnedList() throws Exception {
        User owner = user(1L, "okta-1");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(owner));

        com.example.MusicBoxd.Model.List list = list(10L, 1L, ListType.CUSTOM);
        when(listRepository.findById(10L)).thenReturn(Optional.of(list));

        LastFmSearchAlbum searchAlbum = new LastFmSearchAlbum();
        searchAlbum.setName("Found Album");
        searchAlbum.setArtist("Found Artist");

        LastFmSearchResponse.AlbumMatches matches = new LastFmSearchResponse.AlbumMatches();
        matches.setAlbum(List.of(searchAlbum));
        LastFmSearchResponse.Results results = new LastFmSearchResponse.Results();
        results.setAlbummatches(matches);
        LastFmSearchResponse response = new LastFmSearchResponse();
        response.setResults(results);

        when(lastFmService.searchAlbums("query")).thenReturn(response);

        mockMvc.perform(get("/lists/10/search")
                        .param("query", "query")
                        .with(OidcTestUsers.oidcUser("okta-1", "owner@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("placeholder-list-items :: searchResults"))
                .andExpect(model().attribute("listId", 10L))
                .andExpect(model().attribute("results", List.of(searchAlbum)));
    }

    @Test
    void returnsEmptySearchResultsWhenLastFmReturnsNoMatches() throws Exception {
        User owner = user(1L, "okta-1");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(owner));

        com.example.MusicBoxd.Model.List list = list(10L, 1L, ListType.CUSTOM);
        when(listRepository.findById(10L)).thenReturn(Optional.of(list));

        when(lastFmService.searchAlbums("query")).thenReturn(null);

        mockMvc.perform(get("/lists/10/search")
                        .param("query", "query")
                        .with(OidcTestUsers.oidcUser("okta-1", "owner@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("placeholder-list-items :: searchResults"))
                .andExpect(model().attribute("results", List.of()));
    }

    @Test
    void forbidsSearchingInAnotherUsersList() {
        User requester = user(2L, "okta-2");
        when(userRepository.findByOktaUserId("okta-2")).thenReturn(Optional.of(requester));

        com.example.MusicBoxd.Model.List list = list(10L, 1L, ListType.CUSTOM);
        when(listRepository.findById(10L)).thenReturn(Optional.of(list));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> mockMvc.perform(get("/lists/10/search")
                        .param("query", "query")
                        .with(OidcTestUsers.oidcUser("okta-2", "requester@example.com")))
                .andExpect(status().isForbidden()));
    }

    // ---------------------------------------------------------------
    // POST /lists/{id}/albums
    // ---------------------------------------------------------------

    @Test
    void addsNewAlbumLookedUpFromLastFmToOwnedList() throws Exception {
        User owner = user(1L, "okta-1");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(owner));

        com.example.MusicBoxd.Model.List list = list(10L, 1L, ListType.CUSTOM);
        when(listRepository.findById(10L)).thenReturn(Optional.of(list));

        when(listItemRepository.findMaxPosition(10L)).thenReturn(2);

        LastFmAlbum lastFmAlbum = new LastFmAlbum();
        lastFmAlbum.setName("Real Album Title");
        lastFmAlbum.setReleasedate("1995-01-02");
        LastFmImage image = new LastFmImage();
        image.setText("http://image.example/art.jpg");
        lastFmAlbum.setImage(List.of(image));
        LastFmAlbumResponse response = new LastFmAlbumResponse();
        response.setAlbum(lastFmAlbum);
        when(lastFmService.getAlbumInfo("Some Artist", "Some Title")).thenReturn(response);

        Artist artist = new Artist("Some Artist");
        artist.setId(30L);
        when(artistRepository.findByNameIgnoreCase("Some Artist")).thenReturn(Optional.of(artist));

        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> {
            Album a = inv.getArgument(0);
            a.setId(40L);
            return a;
        });

        mockMvc.perform(post("/lists/10/albums")
                        .param("title", "Some Title")
                        .param("artist", "Some Artist")
                        .with(OidcTestUsers.oidcUser("okta-1", "owner@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/lists/10"));

        verify(artistRepository, never()).save(any());
        verify(albumRepository).save(org.mockito.ArgumentMatchers.argThat(a ->
                a.getTitle().equals("Real Album Title")
                        && a.getArtistId().equals(30L)
                        && a.getReleaseYear() == (short) 1995
                        && a.getArtworkUrl().equals("http://image.example/art.jpg")
        ));
        verify(listItemRepository).save(org.mockito.ArgumentMatchers.argThat(item ->
                item.getListId().equals(10L)
                        && item.getAlbumId().equals(40L)
                        && item.getPosition() == 3
        ));
    }

    @Test
    void addsAlbumWithNullReleaseYearWhenLastFmReleaseDateIsUnparseable() throws Exception {
        User owner = user(1L, "okta-1");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(owner));

        com.example.MusicBoxd.Model.List list = list(10L, 1L, ListType.CUSTOM);
        when(listRepository.findById(10L)).thenReturn(Optional.of(list));

        when(listItemRepository.findMaxPosition(10L)).thenReturn(0);

        LastFmAlbum lastFmAlbum = new LastFmAlbum();
        lastFmAlbum.setName("Real Album Title");
        // Not a parseable year: NumberFormatException should be swallowed by the controller.
        lastFmAlbum.setReleasedate("abcd-01-02");
        LastFmAlbumResponse response = new LastFmAlbumResponse();
        response.setAlbum(lastFmAlbum);
        when(lastFmService.getAlbumInfo("Some Artist", "Some Title")).thenReturn(response);

        Artist artist = new Artist("Some Artist");
        artist.setId(30L);
        when(artistRepository.findByNameIgnoreCase("Some Artist")).thenReturn(Optional.of(artist));

        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> {
            Album a = inv.getArgument(0);
            a.setId(41L);
            return a;
        });

        mockMvc.perform(post("/lists/10/albums")
                        .param("title", "Some Title")
                        .param("artist", "Some Artist")
                        .with(OidcTestUsers.oidcUser("okta-1", "owner@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/lists/10"));

        verify(albumRepository).save(org.mockito.ArgumentMatchers.argThat(a ->
                a.getTitle().equals("Real Album Title")
                        && a.getReleaseYear() == null
                        && a.getArtworkUrl() == null
        ));
    }

    @Test
    void addsAlbumUsingSuppliedTitleWhenLastFmHasNoMatch() throws Exception {
        User owner = user(1L, "okta-1");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(owner));

        com.example.MusicBoxd.Model.List list = list(10L, 1L, ListType.CUSTOM);
        when(listRepository.findById(10L)).thenReturn(Optional.of(list));

        when(listItemRepository.findMaxPosition(10L)).thenReturn(0);
        when(lastFmService.getAlbumInfo("New Artist", "Manual Title")).thenReturn(null);

        when(artistRepository.findByNameIgnoreCase("New Artist")).thenReturn(Optional.empty());
        Artist createdArtist = new Artist("New Artist");
        createdArtist.setId(55L);
        when(artistRepository.save(any(Artist.class))).thenReturn(createdArtist);

        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> {
            Album a = inv.getArgument(0);
            a.setId(60L);
            return a;
        });

        mockMvc.perform(post("/lists/10/albums")
                        .param("title", "Manual Title")
                        .param("artist", "New Artist")
                        .with(OidcTestUsers.oidcUser("okta-1", "owner@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/lists/10"));

        verify(artistRepository).save(any(Artist.class));
        verify(albumRepository).save(org.mockito.ArgumentMatchers.argThat(a ->
                a.getTitle().equals("Manual Title")
                        && a.getArtistId().equals(55L)
                        && a.getReleaseYear() == null
                        && a.getArtworkUrl() == null
        ));
    }

    @Test
    void forbidsAddingAlbumToAnotherUsersList() {
        User requester = user(2L, "okta-2");
        when(userRepository.findByOktaUserId("okta-2")).thenReturn(Optional.of(requester));

        com.example.MusicBoxd.Model.List list = list(10L, 1L, ListType.CUSTOM);
        when(listRepository.findById(10L)).thenReturn(Optional.of(list));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> mockMvc.perform(post("/lists/10/albums")
                        .param("title", "Title")
                        .param("artist", "Artist")
                        .with(OidcTestUsers.oidcUser("okta-2", "requester@example.com")))
                .andExpect(status().isForbidden()));

        verify(albumRepository, never()).save(any());
        verify(listItemRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // POST /lists/{id}/songs
    // ---------------------------------------------------------------

    @Test
    void addsSongToOwnedList() throws Exception {
        User owner = user(1L, "okta-1");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(owner));

        com.example.MusicBoxd.Model.List list = list(10L, 1L, ListType.CUSTOM);
        when(listRepository.findById(10L)).thenReturn(Optional.of(list));

        when(listItemRepository.findMaxPosition(10L)).thenReturn(4);

        mockMvc.perform(post("/lists/10/songs")
                        .param("songId", "77")
                        .with(OidcTestUsers.oidcUser("okta-1", "owner@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/lists/10"));

        verify(listItemRepository).save(org.mockito.ArgumentMatchers.argThat(item ->
                item.getListId().equals(10L)
                        && item.getSongId().equals(77L)
                        && item.getAlbumId() == null
                        && item.getPosition() == 5
        ));
    }

    @Test
    void forbidsAddingSongToAnotherUsersList() {
        User requester = user(2L, "okta-2");
        when(userRepository.findByOktaUserId("okta-2")).thenReturn(Optional.of(requester));

        com.example.MusicBoxd.Model.List list = list(10L, 1L, ListType.CUSTOM);
        when(listRepository.findById(10L)).thenReturn(Optional.of(list));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> mockMvc.perform(post("/lists/10/songs")
                        .param("songId", "77")
                        .with(OidcTestUsers.oidcUser("okta-2", "requester@example.com")))
                .andExpect(status().isForbidden()));

        verify(listItemRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // GET /lists/placeholder-list-form and /lists/placeholder-lists
    // ---------------------------------------------------------------

    @Test
    void showsPlaceholderListFormWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/lists/placeholder-list-form")
                        .with(OidcTestUsers.oidcUser("okta-1", "owner@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("placeholder-list-form"));
    }

    @Test
    void redirectsPlaceholderListsToLists() throws Exception {
        mockMvc.perform(get("/lists/placeholder-lists")
                        .with(OidcTestUsers.oidcUser("okta-1", "owner@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/lists"));
    }

    // ---------------------------------------------------------------
    // GET /lists/album/{id}
    // ---------------------------------------------------------------

    @Test
    void showsAlbumProfileWithWantToListenFlagWhenAlreadyAdded() throws Exception {
        Album album = album(20L, 5L, "Album Title");
        when(albumRepository.findById(20L)).thenReturn(Optional.of(album));

        User owner = user(1L, "okta-1");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(owner));

        com.example.MusicBoxd.Model.List wantToListen = list(15L, 1L, ListType.WANT_TO_LISTEN);
        when(listRepository.findByUserIdAndListType(1L, ListType.WANT_TO_LISTEN))
                .thenReturn(Optional.of(wantToListen));
        when(listItemRepository.existsByListIdAndAlbumId(15L, 20L)).thenReturn(true);

        Artist artist = new Artist("The Artist");
        artist.setId(5L);
        when(artistRepository.findById(5L)).thenReturn(Optional.of(artist));

        LastFmAlbum lastFmAlbum = new LastFmAlbum();
        lastFmAlbum.setName("Album Title");
        LastFmAlbumResponse response = new LastFmAlbumResponse();
        response.setAlbum(lastFmAlbum);
        when(lastFmService.getAlbumInfo("The Artist", "Album Title")).thenReturn(response);

        mockMvc.perform(get("/lists/album/20")
                        .with(OidcTestUsers.oidcUser("okta-1", "owner@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("album-profile"))
                .andExpect(model().attribute("album", album))
                .andExpect(model().attribute("alreadyInWantToListen", true))
                .andExpect(model().attribute("artist", artist))
                .andExpect(model().attribute("lastFmAlbum", lastFmAlbum));
    }

    @Test
    void showsAlbumProfileWithoutWantToListenFlagWhenNotAdded() throws Exception {
        Album album = album(21L, null, "Album Title");
        when(albumRepository.findById(21L)).thenReturn(Optional.of(album));

        User owner = user(1L, "okta-1");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(owner));

        when(listRepository.findByUserIdAndListType(1L, ListType.WANT_TO_LISTEN))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/lists/album/21")
                        .with(OidcTestUsers.oidcUser("okta-1", "owner@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("album-profile"))
                .andExpect(model().attribute("alreadyInWantToListen", false));

        verify(listItemRepository, never()).existsByListIdAndAlbumId(any(), any());
    }

    @Test
    void redirectsToListsWhenAlbumForAlbumProfileDoesNotExist() throws Exception {
        when(albumRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/lists/album/999")
                        .with(OidcTestUsers.oidcUser("okta-1", "owner@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/lists"));
    }

    // ---------------------------------------------------------------
    // POST /lists/{id}/albums/{albumId}
    // ---------------------------------------------------------------

    @Test
    void addsExistingAlbumToOwnedListWhenNotAlreadyPresent() throws Exception {
        User owner = user(1L, "okta-1");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(owner));

        com.example.MusicBoxd.Model.List list = list(10L, 1L, ListType.CUSTOM);
        when(listRepository.findById(10L)).thenReturn(Optional.of(list));

        Album album = album(20L, null, "Album Title");
        when(albumRepository.findById(20L)).thenReturn(Optional.of(album));

        when(listItemRepository.existsByListIdAndAlbumId(10L, 20L)).thenReturn(false);
        when(listItemRepository.findMaxPosition(10L)).thenReturn(1);

        mockMvc.perform(post("/lists/10/albums/20")
                        .with(OidcTestUsers.oidcUser("okta-1", "owner@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/albums/20"));

        verify(listItemRepository).save(org.mockito.ArgumentMatchers.argThat(item ->
                item.getListId().equals(10L)
                        && item.getAlbumId().equals(20L)
                        && item.getPosition() == 2
        ));
    }

    @Test
    void doesNotDuplicateAlbumAlreadyInList() throws Exception {
        User owner = user(1L, "okta-1");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(owner));

        com.example.MusicBoxd.Model.List list = list(10L, 1L, ListType.CUSTOM);
        when(listRepository.findById(10L)).thenReturn(Optional.of(list));

        Album album = album(20L, null, "Album Title");
        when(albumRepository.findById(20L)).thenReturn(Optional.of(album));

        when(listItemRepository.existsByListIdAndAlbumId(10L, 20L)).thenReturn(true);

        mockMvc.perform(post("/lists/10/albums/20")
                        .with(OidcTestUsers.oidcUser("okta-1", "owner@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/albums/20"));

        verify(listItemRepository, never()).save(any());
        verify(listItemRepository, never()).findMaxPosition(any());
    }

    @Test
    void forbidsAddingExistingAlbumToAnotherUsersList() {
        User requester = user(2L, "okta-2");
        when(userRepository.findByOktaUserId("okta-2")).thenReturn(Optional.of(requester));

        com.example.MusicBoxd.Model.List list = list(10L, 1L, ListType.CUSTOM);
        when(listRepository.findById(10L)).thenReturn(Optional.of(list));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> mockMvc.perform(post("/lists/10/albums/20")
                        .with(OidcTestUsers.oidcUser("okta-2", "requester@example.com")))
                .andExpect(status().isForbidden()));

        verify(listItemRepository, never()).save(any());
    }

    @Test
    void throwsWhenAddingAlbumThatDoesNotExist() {
        User owner = user(1L, "okta-1");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(owner));

        com.example.MusicBoxd.Model.List list = list(10L, 1L, ListType.CUSTOM);
        when(listRepository.findById(10L)).thenReturn(Optional.of(list));
        when(albumRepository.findById(999L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                mockMvc.perform(post("/lists/10/albums/999")
                        .with(OidcTestUsers.oidcUser("okta-1", "owner@example.com")))
        );
    }

    // ---------------------------------------------------------------
    // POST /lists/want-to-listen/{albumId}
    // ---------------------------------------------------------------

    @Test
    void createsWantToListenListWhenNoneExistsYetAndAddsAlbum() throws Exception {
        User owner = user(1L, "okta-1");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(owner));

        Album album = album(20L, null, "Album Title");
        when(albumRepository.findById(20L)).thenReturn(Optional.of(album));

        when(listRepository.findByUserIdAndListType(1L, ListType.WANT_TO_LISTEN))
                .thenReturn(Optional.empty());

        com.example.MusicBoxd.Model.List newList = list(50L, 1L, ListType.WANT_TO_LISTEN);
        when(listRepository.save(any(com.example.MusicBoxd.Model.List.class))).thenReturn(newList);

        when(listItemRepository.existsByListIdAndAlbumId(50L, 20L)).thenReturn(false);
        when(listItemRepository.findMaxPosition(50L)).thenReturn(0);

        mockMvc.perform(post("/lists/want-to-listen/20")
                        .with(OidcTestUsers.oidcUser("okta-1", "owner@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/albums/20"));

        verify(listRepository).save(org.mockito.ArgumentMatchers.argThat(l ->
                l.getUserId().equals(1L) && l.getListType() == ListType.WANT_TO_LISTEN
        ));
        verify(listItemRepository).save(org.mockito.ArgumentMatchers.argThat(item ->
                item.getListId().equals(50L)
                        && item.getAlbumId().equals(20L)
                        && item.getPosition() == 1
        ));
    }

    @Test
    void reusesExistingWantToListenListAndSkipsDuplicateAlbum() throws Exception {
        User owner = user(1L, "okta-1");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(owner));

        Album album = album(20L, null, "Album Title");
        when(albumRepository.findById(20L)).thenReturn(Optional.of(album));

        com.example.MusicBoxd.Model.List existingList = list(50L, 1L, ListType.WANT_TO_LISTEN);
        when(listRepository.findByUserIdAndListType(1L, ListType.WANT_TO_LISTEN))
                .thenReturn(Optional.of(existingList));

        when(listItemRepository.existsByListIdAndAlbumId(50L, 20L)).thenReturn(true);

        mockMvc.perform(post("/lists/want-to-listen/20")
                        .with(OidcTestUsers.oidcUser("okta-1", "owner@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/albums/20"));

        verify(listRepository, never()).save(any());
        verify(listItemRepository, never()).save(any());
    }

    @Test
    void throwsWhenAddingToWantToListenForAlbumThatDoesNotExist() {
        User owner = user(1L, "okta-1");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(owner));
        when(albumRepository.findById(999L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                mockMvc.perform(post("/lists/want-to-listen/999")
                        .with(OidcTestUsers.oidcUser("okta-1", "owner@example.com")))
        );
    }

    @Test
    void redirectsUnauthenticatedWantToListenRequestToOAuth2Login() throws Exception {
        mockMvc.perform(post("/lists/want-to-listen/20"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.startsWith(
                                "/oauth2/authorization/" + TestOAuth2Config.REGISTRATION_ID
                        )
                ));

        verify(listRepository, never()).save(any());
    }
}
