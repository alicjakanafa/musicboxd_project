package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Config.RestTemplateConfig;
import com.example.MusicBoxd.Config.SecurityConfig;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.support.OidcTestUsers;
import com.example.MusicBoxd.support.TestOAuth2Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = SpotifyController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import({SecurityConfig.class, TestOAuth2Config.class, RestTemplateConfig.class})
@TestPropertySource(properties = {
        "okta.oauth2.issuer=https://test-issuer.invalid/",
        "okta.oauth2.client-id=test-client-id",
        "spotify.client.id=test-spotify-client-id",
        "spotify.client.secret=test-spotify-client-secret",
        "spotify.redirect.uri=http://127.0.0.1:8080/spotify/callback"
})
class SpotifyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    @MockitoBean
    private UserRepository userRepository;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @AfterEach
    void tearDown() {
        mockServer.verify();
    }

    private MockHttpSession sessionWithToken(String token) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("spotifyAccessToken", token);
        return session;
    }

    // ---------- /spotify/login ----------

    @Test
    void loginRedirectsToSpotifyAuthorizeUrlWithExpectedQueryParams() throws Exception {
        mockMvc.perform(get("/spotify/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith("https://accounts.spotify.com/authorize?")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("client_id=test-spotify-client-id")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("response_type=code")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("redirect_uri=http")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("scope=user-read-recently-played")));
    }

    @Test
    void loginIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/spotify/login"))
                .andExpect(status().is3xxRedirection());
    }

    // ---------- /spotify/callback ----------

    @Test
    void callbackExchangesCodeForTokenStoresItInSessionAndRedirectsHome() throws Exception {
        String tokenResponse = """
                {
                  "access_token": "abc123token",
                  "token_type": "Bearer",
                  "expires_in": 3600,
                  "refresh_token": "refresh-xyz",
                  "scope": "user-read-recently-played"
                }
                """;

        mockServer.expect(requestTo("https://accounts.spotify.com/api/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("Authorization", startsWith("Basic ")))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content()
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("grant_type=authorization_code")))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("code=auth-code-123")))
                .andRespond(withSuccess(tokenResponse, MediaType.APPLICATION_JSON));

        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(get("/spotify/callback")
                        .param("code", "auth-code-123")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/"));

        org.assertj.core.api.Assertions.assertThat(session.getAttribute("spotifyAccessToken"))
                .isEqualTo("abc123token");
    }

    @Test
    void callbackThrowsWhenSpotifyReturnsNoTokenBody() {
        mockServer.expect(requestTo("https://accounts.spotify.com/api/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withNoContent());

        MockHttpSession session = new MockHttpSession();

        org.junit.jupiter.api.function.Executable callAttempt = () ->
                mockMvc.perform(get("/spotify/callback")
                        .param("code", "auth-code-123")
                        .session(session));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            try {
                callAttempt.execute();
            } catch (Exception e) {
                throw e;
            }
        }).hasCauseInstanceOf(IllegalStateException.class);
    }

    // ---------- /spotify/player/current ----------

    @Test
    void currentReturnsUnauthorizedWhenNoTokenInSession() throws Exception {
        mockMvc.perform(get("/spotify/player/current")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void currentRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/spotify/player/current"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith("/oauth2/authorization/")));
    }

    @Test
    void currentReturnsNoContentWhenNothingIsPlaying() throws Exception {
        mockServer.expect(requestTo("https://api.spotify.com/v1/me/player"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("Authorization", "Bearer token-1"))
                .andRespond(withNoContent());

        mockMvc.perform(get("/spotify/player/current")
                        .session(sessionWithToken("token-1"))
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isNoContent());
    }

    @Test
    void currentReturnsNoContentWhenResponseHasNoItem() throws Exception {
        mockServer.expect(requestTo("https://api.spotify.com/v1/me/player"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{ \"is_playing\": false }", MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/spotify/player/current")
                        .session(sessionWithToken("token-1"))
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isNoContent());
    }

    @Test
    void currentReturnsCurrentlyPlayingTrackWhenAvailable() throws Exception {
        String body = """
                {
                  "is_playing": true,
                  "progress_ms": 15000,
                  "item": {
                    "id": "track-1",
                    "name": "Karma Police",
                    "duration_ms": 261000,
                    "artists": [ { "name": "Radiohead" } ],
                    "album": { "name": "OK Computer" },
                    "external_urls": { "spotify": "https://open.spotify.com/track/track-1" }
                  }
                }
                """;

        mockServer.expect(requestTo("https://api.spotify.com/v1/me/player"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/spotify/player/current")
                        .session(sessionWithToken("token-1"))
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_playing").value(true))
                .andExpect(jsonPath("$.progress_ms").value(15000))
                .andExpect(jsonPath("$.item.name").value("Karma Police"))
                .andExpect(jsonPath("$.item.artists[0].name").value("Radiohead"))
                .andExpect(jsonPath("$.item.album.name").value("OK Computer"));
    }

    @Test
    void currentPropagatesSpotifyServerErrorAsUnhandledException() {
        mockServer.expect(requestTo("https://api.spotify.com/v1/me/player"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        // The controller does not catch RestTemplate exceptions, so an upstream Spotify
        // failure currently surfaces as an unhandled HttpServerErrorException rather than a
        // controlled error response to the client.
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        mockMvc.perform(get("/spotify/player/current")
                                .session(sessionWithToken("token-1"))
                                .with(OidcTestUsers.oidcUser("okta-1", "user@example.com"))))
                .hasCauseInstanceOf(org.springframework.web.client.HttpServerErrorException.class);
    }

    // ---------- player control endpoints ----------

    @Test
    void playReturnsUnauthorizedWhenNoTokenInSession() throws Exception {
        mockMvc.perform(post("/spotify/player/play")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void playSendsPutRequestWithBearerTokenToSpotify() throws Exception {
        mockServer.expect(requestTo("https://api.spotify.com/v1/me/player/play"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("Authorization", "Bearer token-1"))
                .andRespond(withNoContent());

        mockMvc.perform(post("/spotify/player/play")
                        .session(sessionWithToken("token-1"))
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isNoContent());
    }

    @Test
    void pauseSendsPutRequestToSpotify() throws Exception {
        mockServer.expect(requestTo("https://api.spotify.com/v1/me/player/pause"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withNoContent());

        mockMvc.perform(post("/spotify/player/pause")
                        .session(sessionWithToken("token-1"))
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isNoContent());
    }

    @Test
    void nextSendsPostRequestToSpotify() throws Exception {
        mockServer.expect(requestTo("https://api.spotify.com/v1/me/player/next"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withNoContent());

        mockMvc.perform(post("/spotify/player/next")
                        .session(sessionWithToken("token-1"))
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isNoContent());
    }

    @Test
    void previousSendsPostRequestToSpotify() throws Exception {
        mockServer.expect(requestTo("https://api.spotify.com/v1/me/player/previous"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withNoContent());

        mockMvc.perform(post("/spotify/player/previous")
                        .session(sessionWithToken("token-1"))
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isNoContent());
    }

    @Test
    void previousPropagatesSpotifyUnauthorizedResponseAsUnhandledException() {
        mockServer.expect(requestTo("https://api.spotify.com/v1/me/player/previous"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withUnauthorizedRequest());

        // As above: an expired/invalid Spotify token results in an unhandled
        // HttpClientErrorException rather than the controller returning 401 to the client.
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        mockMvc.perform(post("/spotify/player/previous")
                                .session(sessionWithToken("expired-token"))
                                .with(OidcTestUsers.oidcUser("okta-1", "user@example.com"))))
                .hasCauseInstanceOf(org.springframework.web.client.HttpClientErrorException.class);
    }
}
