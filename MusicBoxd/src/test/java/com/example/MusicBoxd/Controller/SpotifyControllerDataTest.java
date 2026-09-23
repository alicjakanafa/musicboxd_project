package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.api.spotify.SpotifyTopArtistsResponse;
import com.example.MusicBoxd.api.spotify.SpotifyTopTracksResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Exercises {@link SpotifyController}'s data-fetching methods ({@code getSpotifyData},
 * {@code getTopArtists}, {@code getTopTracks}) directly, since none of the current MVC
 * mappings expose them and they are instead called by other controllers
 * (e.g. {@code HomeController}, {@code ProfileController}) that inject this bean.
 */
class SpotifyControllerDataTest {

    private SpotifyController controller;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        controller = new SpotifyController(restTemplate);
    }

    @AfterEach
    void tearDown() {
        mockServer.verify();
    }

    private void expectRecentlyPlayed(String body) {
        mockServer.expect(requestTo("https://api.spotify.com/v1/me/player/recently-played?limit=50"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-1"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void expectTopTracks(String timeRange, String body) {
        mockServer.expect(requestTo(
                        "https://api.spotify.com/v1/me/top/tracks?time_range=" + timeRange + "&limit=6"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void expectCurrentlyPlaying(String body) {
        mockServer.expect(requestTo("https://api.spotify.com/v1/me/player"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    // ---------- getSpotifyData ----------

    @Test
    void getSpotifyDataPopulatesModelWithListeningStatsFromRecentlyPlayedItems() {
        String recentlyPlayedBody = """
                {
                  "items": [
                    {
                      "track": {
                        "id": "t1",
                        "name": "Karma Police",
                        "duration_ms": 200000,
                        "artists": [
                          {
                            "name": "Radiohead",
                            "images": [
                              { "url": "https://example.com/radiohead.jpg", "width": 640, "height": 640 }
                            ]
                          }
                        ],
                        "album": {
                          "name": "OK Computer",
                          "images": [
                            { "url": "https://example.com/ok-computer.jpg", "width": 300, "height": 300 }
                          ]
                        }
                      }
                    },
                    {
                      "track": {
                        "id": "t1",
                        "name": "Karma Police (dup play)",
                        "duration_ms": 100000,
                        "artists": [ { "name": "Thom Yorke" } ],
                        "album": { "name": "In Rainbows" }
                      }
                    },
                    { "track": null },
                    {
                      "track": {
                        "id": null,
                        "name": "Unknown Track",
                        "artists": null,
                        "album": null
                      }
                    }
                  ]
                }
                """;
        expectRecentlyPlayed(recentlyPlayedBody);

        expectTopTracks("medium_term", """
                {
                  "items": [
                    { "id": "top1", "name": "Idioteque" }
                  ]
                }
                """);

        expectCurrentlyPlaying("""
                {
                  "is_playing": true,
                  "item": { "id": "t1", "name": "Karma Police" }
                }
                """);

        Model model = new ExtendedModelMap();

        controller.getSpotifyData("token-1", model, "medium_term");

        assertThat(model.getAttribute("recentTracksCount")).isEqualTo(4);
        assertThat(model.getAttribute("uniqueTracksCount")).isEqualTo(1L);
        assertThat(model.getAttribute("uniqueArtistsCount")).isEqualTo(2L);
        assertThat(model.getAttribute("uniqueAlbumsCount")).isEqualTo(2L);
        assertThat(model.getAttribute("listeningMinutes")).isEqualTo(5L);
        assertThat((java.util.List<?>) model.getAttribute("recentlyPlayed")).hasSize(4);
        assertThat(model.getAttribute("recentTrack")).isNotNull();

        assertThat((java.util.List<?>) model.getAttribute("topTracks")).hasSize(1);
        assertThat(model.getAttribute("currentlyPlaying")).isNotNull();

        com.example.MusicBoxd.api.spotify.SpotifyRecentlyPlayedResponse.RecentlyPlayedItem firstItem =
                (com.example.MusicBoxd.api.spotify.SpotifyRecentlyPlayedResponse.RecentlyPlayedItem)
                        model.getAttribute("recentTrack");
        com.example.MusicBoxd.api.spotify.SpotifyTrack firstTrack = firstItem.getTrack();
        assertThat(firstTrack.getArtists().get(0).getImages())
                .extracting(
                        com.example.MusicBoxd.api.spotify.SpotifyImage::getUrl,
                        com.example.MusicBoxd.api.spotify.SpotifyImage::getWidth,
                        com.example.MusicBoxd.api.spotify.SpotifyImage::getHeight)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(
                        "https://example.com/radiohead.jpg", 640, 640));
        assertThat(firstTrack.getAlbum().getImages())
                .extracting(com.example.MusicBoxd.api.spotify.SpotifyImage::getUrl)
                .containsExactly("https://example.com/ok-computer.jpg");
    }

    @Test
    void getSpotifyDataTwoArgOverloadDefaultsToMediumTermTimeRange() {
        expectRecentlyPlayed("{ \"items\": [] }");
        expectTopTracks("medium_term", "{ \"items\": [] }");
        expectCurrentlyPlaying("{ \"is_playing\": false }");

        Model model = new ExtendedModelMap();

        controller.getSpotifyData("token-1", model);

        assertThat(model.getAttribute("recentTracksCount")).isEqualTo(0);
        assertThat((java.util.List<?>) model.getAttribute("topTracks")).isEmpty();
        assertThat(model.getAttribute("currentlyPlaying")).isNull();
    }

    @Test
    void getSpotifyDataFallsBackToMediumTermForAnInvalidTimeRange() {
        expectRecentlyPlayed("{ \"items\": [] }");
        expectTopTracks("medium_term", "{ \"items\": [] }");
        expectCurrentlyPlaying("{ \"is_playing\": false }");

        Model model = new ExtendedModelMap();

        controller.getSpotifyData("token-1", model, "not-a-real-range");

        // No assertion failure means the mock server's expected medium_term URL was hit,
        // proving validateTimeRange() substituted the invalid value.
        assertThat(model.getAttribute("recentTracksCount")).isEqualTo(0);
    }

    @Test
    void getSpotifyDataSetsZeroedStatsWhenRecentlyPlayedResponseBodyIsAbsent() {
        mockServer.expect(requestTo("https://api.spotify.com/v1/me/player/recently-played?limit=50"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withNoContent());
        expectTopTracks("medium_term", "{ \"items\": null }");
        expectCurrentlyPlaying("{ \"is_playing\": false }");

        Model model = new ExtendedModelMap();

        controller.getSpotifyData("token-1", model, "medium_term");

        assertThat(model.getAttribute("recentTracksCount")).isEqualTo(0);
        assertThat(model.getAttribute("uniqueTracksCount")).isEqualTo(0);
        assertThat(model.getAttribute("uniqueArtistsCount")).isEqualTo(0);
        assertThat(model.getAttribute("uniqueAlbumsCount")).isEqualTo(0);
        assertThat(model.getAttribute("listeningMinutes")).isEqualTo(0);
        assertThat((java.util.List<?>) model.getAttribute("recentlyPlayed")).isEmpty();
        assertThat(model.getAttribute("recentTrack")).isNull();
        assertThat((java.util.List<?>) model.getAttribute("topTracks")).isEmpty();
        assertThat(model.getAttribute("currentlyPlaying")).isNull();
    }

    @Test
    void getSpotifyDataTreatsAnEmptyRecentlyPlayedItemsListAsNoRecentTrack() {
        expectRecentlyPlayed("{ \"items\": [] }");
        expectTopTracks("short_term", "{ \"items\": [] }");
        expectCurrentlyPlaying("{ \"is_playing\": false }");

        Model model = new ExtendedModelMap();

        controller.getSpotifyData("token-1", model, "short_term");

        assertThat(model.getAttribute("recentTracksCount")).isEqualTo(0);
        assertThat((java.util.List<?>) model.getAttribute("recentlyPlayed")).isEmpty();
        assertThat(model.getAttribute("recentTrack")).isNull();
    }

    // ---------- getTopArtists ----------

    @Test
    void getTopArtistsBuildsRequestWithGivenTimeRangeAndParsesArtists() {
        mockServer.expect(requestTo(
                        "https://api.spotify.com/v1/me/top/artists?time_range=long_term&limit=6"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-1"))
                .andRespond(withSuccess("""
                        {
                          "items": [
                            { "name": "Radiohead" },
                            { "name": "Portishead" }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        SpotifyTopArtistsResponse response = controller.getTopArtists("token-1", "long_term");

        assertThat(response).isNotNull();
        assertThat(response.getItems()).extracting("name")
                .containsExactly("Radiohead", "Portishead");
    }

    @Test
    void getTopArtistsFallsBackToMediumTermWhenTimeRangeIsNull() {
        mockServer.expect(requestTo(
                        "https://api.spotify.com/v1/me/top/artists?time_range=medium_term&limit=6"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{ \"items\": [] }", MediaType.APPLICATION_JSON));

        SpotifyTopArtistsResponse response = controller.getTopArtists("token-1", null);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).isEmpty();
    }

    // ---------- getTopTracks ----------

    @Test
    void getTopTracksBuildsRequestWithGivenTimeRangeAndParsesTracks() {
        mockServer.expect(requestTo(
                        "https://api.spotify.com/v1/me/top/tracks?time_range=short_term&limit=6"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("time_range", "short_term"))
                .andExpect(header("Authorization", "Bearer token-1"))
                .andRespond(withSuccess("""
                        {
                          "items": [
                            { "id": "tr1", "name": "Idioteque" }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        SpotifyTopTracksResponse response = controller.getTopTracks("token-1", "short_term");

        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getName()).isEqualTo("Idioteque");
    }

    @Test
    void getTopTracksFallsBackToMediumTermWhenTimeRangeIsInvalid() {
        mockServer.expect(requestTo(
                        "https://api.spotify.com/v1/me/top/tracks?time_range=medium_term&limit=6"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{ \"items\": [] }", MediaType.APPLICATION_JSON));

        SpotifyTopTracksResponse response = controller.getTopTracks("token-1", "bogus");

        assertThat(response).isNotNull();
        assertThat(response.getItems()).isEmpty();
    }
}
