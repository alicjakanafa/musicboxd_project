package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Config.SecurityConfig;
import com.example.MusicBoxd.Repository.ArtistRepository;
import com.example.MusicBoxd.Repository.UserRepository;
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

import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(lastFmController.class)
@Import({SecurityConfig.class, TestOAuth2Config.class})
@TestPropertySource(properties = {
        "okta.oauth2.issuer=https://test-issuer.invalid/",
        "okta.oauth2.client-id=test-client-id"
})
class LastFmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ArtistRepository artistRepository;

    @MockitoBean
    private LastFmService lastFmService;

    @Test
    void redirectsUnauthenticatedUserToLogin() throws Exception {
        mockMvc.perform(get("/top-40"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        startsWith("/oauth2/authorization/" + TestOAuth2Config.REGISTRATION_ID)
                ));
    }

    @Test
    void showsTopArtistsForAuthenticatedUser() throws Exception {
        LastFmArtist artist = new LastFmArtist();
        artist.setName("Radiohead");

        LastFmArtists artists = new LastFmArtists();
        artists.setArtist(List.of(artist));

        LastFmResponse response = new LastFmResponse();
        response.setArtists(artists);

        when(lastFmService.getTopArtists()).thenReturn(response);

        mockMvc.perform(get("/top-40")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("top-40"))
                .andExpect(model().attribute("artists", List.of(artist)));
    }
}
