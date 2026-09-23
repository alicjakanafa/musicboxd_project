package com.example.MusicBoxd.api.lastfm;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LastFmServiceTest {

    private static final String API_KEY = "test-api-key";

    private LastFmService lastFmService;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {

        RestTemplate restTemplate =
                new RestTemplate();

        lastFmService =
                new LastFmService(restTemplate, "test-api-key");

        /*
         * LastFmService normally gets the API key
         * from the LASTFM_API_KEY environment variable.
         *
         * For the test, we replace the private apiKey
         * field with our test value.
         */
        ReflectionTestUtils.setField(
                lastFmService,
                "apiKey",
                API_KEY
        );

        mockServer =
                MockRestServiceServer
                        .bindTo(restTemplate)
                        .build();
    }

    @AfterEach
    void tearDown() {
        mockServer.verify();
    }

    @Test
    void getTopArtistsBuildsExpectedRequestAndParsesResponse() {

        String responseBody = """
            {
              "artists": {
                "artist": [
                  {
                    "name": "Radiohead",
                    "stats": {
                      "playcount": "12345678",
                      "listeners": "2345678"
                    },
                    "mbid": "a74b1b7f-71a5-4011-9441-d0b5e4122711",
                    "url": "https://www.last.fm/music/Radiohead",
                    "image": [
                      {
                        "#text": "https://example.com/small.jpg",
                        "size": "small"
                      },
                      {
                        "#text": "https://example.com/large.jpg",
                        "size": "large"
                      }
                    ]
                  },
                  {
                    "name": "Taylor Swift",
                    "stats": {
                      "playcount": "98765432",
                      "listeners": "5432198"
                    },
                    "mbid": "20244d07-534f-4eff-b4d4-930878889970",
                    "url": "https://www.last.fm/music/Taylor+Swift",
                    "image": []
                  }
                ]
              }
            }
            """;

        mockServer
                .expect(
                        requestTo(
                                startsWith(
                                        "https://ws.audioscrobbler.com/2.0/"
                                )
                        )
                )
                .andExpect(
                        method(HttpMethod.GET)
                )
                .andExpect(
                        queryParam(
                                "method",
                                "chart.gettopartists"
                        )
                )
                .andExpect(
                        queryParam(
                                "api_key",
                                API_KEY
                        )
                )
                .andExpect(
                        queryParam(
                                "format",
                                "json"
                        )
                )
                .andExpect(
                        queryParam(
                                "limit",
                                "40"
                        )
                )
                .andRespond(
                        withSuccess(
                                responseBody,
                                MediaType.APPLICATION_JSON
                        )
                );

        LastFmResponse response =
                lastFmService.getTopArtists();

        assertThat(response)
                .isNotNull();

        assertThat(response.getArtists())
                .isNotNull();

        assertThat(
                response
                        .getArtists()
                        .getArtist()
        ).hasSize(2);

        LastFmArtist radiohead =
                response
                        .getArtists()
                        .getArtist()
                        .get(0);

        assertThat(radiohead.getName())
                .isEqualTo("Radiohead");

        assertThat(radiohead.getStats())
                .isNotNull();

        assertThat(
                radiohead
                        .getStats()
                        .getPlaycount()
        ).isEqualTo("12345678");

        assertThat(
                radiohead
                        .getStats()
                        .getListeners()
        ).isEqualTo("2345678");

        assertThat(radiohead.getMbid())
                .isEqualTo(
                        "a74b1b7f-71a5-4011-9441-d0b5e4122711"
                );

        assertThat(radiohead.getUrl())
                .isEqualTo(
                        "https://www.last.fm/music/Radiohead"
                );

        assertThat(radiohead.getImage())
                .hasSize(2);

        assertThat(
                radiohead
                        .getImage()
                        .get(0)
                        .getText()
        ).isEqualTo(
                "https://example.com/small.jpg"
        );

        assertThat(
                radiohead
                        .getImage()
                        .get(0)
                        .getSize()
        ).isEqualTo("small");

        assertThat(
                radiohead
                        .getImage()
                        .get(1)
                        .getSize()
        ).isEqualTo("large");

        LastFmArtist taylorSwift =
                response
                        .getArtists()
                        .getArtist()
                        .get(1);

        assertThat(taylorSwift.getName())
                .isEqualTo("Taylor Swift");

        assertThat(taylorSwift.getImage())
                .isEmpty();
    }

    @Test
    void getTopArtistsUsesInjectedApiKeyInRequest() {

        RestTemplate restTemplate =
                new RestTemplate();

        LastFmService serviceWithDifferentKey =
                new LastFmService(restTemplate, "test-api-key");

        ReflectionTestUtils.setField(
                serviceWithDifferentKey,
                "apiKey",
                "another-key"
        );

        MockRestServiceServer server =
                MockRestServiceServer
                        .bindTo(restTemplate)
                        .build();

        server
                .expect(
                        requestTo(
                                startsWith(
                                        "https://ws.audioscrobbler.com/2.0/"
                                )
                        )
                )
                .andExpect(
                        method(HttpMethod.GET)
                )
                .andExpect(
                        queryParam(
                                "api_key",
                                "another-key"
                        )
                )
                .andRespond(
                        withSuccess(
                                "{\"artists\": {\"artist\": []}}",
                                MediaType.APPLICATION_JSON
                        )
                );

        LastFmResponse response =
                serviceWithDifferentKey.getTopArtists();

        assertThat(
                response
                        .getArtists()
                        .getArtist()
        ).isEmpty();

        server.verify();
    }

    @Test
    void getArtistInfoBuildsExpectedRequestAndParsesResponse() {

        String responseBody = """
                {
                  "artist": {
                    "name": "Radiohead",
                    "mbid": "a74b1b7f-71a5-4011-9441-d0b5e4122711",
                    "url": "https://www.last.fm/music/Radiohead",
                    "listeners": "2345678",
                    "playcount": "12345678",
                    "bio": {
                      "summary": "Short bio",
                      "content": "Full bio content"
                    }
                  }
                }
                """;

        mockServer
                .expect(
                        org.springframework.test.web.client.ExpectedCount.times(2),
                        requestTo(startsWith("https://ws.audioscrobbler.com/2.0/"))
                )
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("method", "artist.getinfo"))
                .andExpect(queryParam("artist", "Radiohead"))
                .andExpect(queryParam("api_key", API_KEY))
                .andExpect(queryParam("format", "json"))
                .andExpect(queryParam("autocorrect", "1"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        LastFmArtistResponse response = lastFmService.getArtistInfo("Radiohead");

        assertThat(response).isNotNull();
        assertThat(response.getArtist()).isNotNull();
        assertThat(response.getArtist().getName()).isEqualTo("Radiohead");
        assertThat(response.getArtist().getListeners()).isEqualTo("2345678");
        assertThat(response.getArtist().getPlaycount()).isEqualTo("12345678");
        assertThat(response.getArtist().getBio()).isNotNull();
        assertThat(response.getArtist().getBio().getSummary()).isEqualTo("Short bio");
        assertThat(response.getArtist().getBio().getContent()).isEqualTo("Full bio content");
    }

    @Test
    void getArtistAlbumsBuildsExpectedRequestAndParsesResponse() {

        String responseBody = """
                {
                  "topalbums": {
                    "album": [
                      {
                        "name": "OK Computer",
                        "mbid": "b1392450-e666-3926-a536-22c65f834433",
                        "url": "https://www.last.fm/music/Radiohead/OK+Computer",
                        "image": [
                          { "#text": "https://example.com/album.jpg", "size": "large" }
                        ]
                      }
                    ]
                  }
                }
                """;

        mockServer
                .expect(requestTo(startsWith("https://ws.audioscrobbler.com/2.0/")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("method", "artist.gettopalbums"))
                .andExpect(queryParam("artist", "Radiohead"))
                .andExpect(queryParam("api_key", API_KEY))
                .andExpect(queryParam("format", "json"))
                .andExpect(queryParam("limit", "50"))
                .andExpect(queryParam("autocorrect", "1"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        LastFmTopAlbumsResponse response = lastFmService.getArtistAlbums("Radiohead");

        assertThat(response).isNotNull();
        assertThat(response.getTopalbums()).isNotNull();
        assertThat(response.getTopalbums().getAlbum()).hasSize(1);

        LastFmTopAlbum album = response.getTopalbums().getAlbum().get(0);
        assertThat(album.getName()).isEqualTo("OK Computer");
        assertThat(album.getUrl()).isEqualTo("https://www.last.fm/music/Radiohead/OK+Computer");
        assertThat(album.getImage()).hasSize(1);
        assertThat(album.getImage().get(0).getText()).isEqualTo("https://example.com/album.jpg");
    }

    @Test
    void searchAlbumsBuildsExpectedRequestAndParsesResponse() {

        String responseBody = """
                {
                  "results": {
                    "albummatches": {
                      "album": [
                        {
                          "name": "OK Computer",
                          "artist": "Radiohead",
                          "url": "https://www.last.fm/music/Radiohead/OK+Computer",
                          "image": [
                            { "#text": "https://example.com/album.jpg", "size": "large" }
                          ]
                        }
                      ]
                    }
                  }
                }
                """;

        mockServer
                .expect(requestTo(startsWith("https://ws.audioscrobbler.com/2.0/")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("method", "album.search"))
                .andExpect(queryParam("album", "OK%20Computer"))
                .andExpect(queryParam("api_key", API_KEY))
                .andExpect(queryParam("format", "json"))
                .andExpect(queryParam("limit", "20"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        LastFmSearchResponse response = lastFmService.searchAlbums("OK Computer");

        assertThat(response).isNotNull();
        assertThat(response.getResults()).isNotNull();
        assertThat(response.getResults().getAlbummatches()).isNotNull();
        assertThat(response.getResults().getAlbummatches().getAlbum()).hasSize(1);

        LastFmSearchAlbum album = response.getResults().getAlbummatches().getAlbum().get(0);
        assertThat(album.getName()).isEqualTo("OK Computer");
        assertThat(album.getArtist()).isEqualTo("Radiohead");
    }

    @Test
    void searchAlbumsReturnsNullWhenRemoteServiceReturnsServerError() {

        mockServer
                .expect(requestTo(startsWith("https://ws.audioscrobbler.com/2.0/")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        LastFmSearchResponse response = lastFmService.searchAlbums("OK Computer");

        assertThat(response).isNull();
    }

    @Test
    void getAlbumInfoBuildsExpectedRequestAndParsesResponse() {

        String responseBody = """
                {
                  "album": {
                    "name": "OK Computer",
                    "artist": "Radiohead",
                    "url": "https://www.last.fm/music/Radiohead/OK+Computer",
                    "releasedate": "16 May 1997",
                    "tracks": {
                      "track": [
                        { "name": "Airbag", "duration": "284" }
                      ]
                    }
                  }
                }
                """;

        mockServer
                .expect(requestTo(startsWith("https://ws.audioscrobbler.com/2.0/")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("method", "album.getinfo"))
                .andExpect(queryParam("artist", "Radiohead"))
                .andExpect(queryParam("album", "OK%20Computer"))
                .andExpect(queryParam("api_key", API_KEY))
                .andExpect(queryParam("format", "json"))
                .andExpect(queryParam("autocorrect", "1"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        LastFmAlbumResponse response = lastFmService.getAlbumInfo("Radiohead", "OK Computer");

        assertThat(response).isNotNull();
        assertThat(response.getAlbum()).isNotNull();
        assertThat(response.getAlbum().getName()).isEqualTo("OK Computer");
        assertThat(response.getAlbum().getArtist()).isEqualTo("Radiohead");
        assertThat(response.getAlbum().getReleasedate()).isEqualTo("16 May 1997");
        assertThat(response.getAlbum().getTracks()).isNotNull();
        assertThat(response.getAlbum().getTracks().getTrack()).hasSize(1);
        assertThat(response.getAlbum().getTracks().getTrack().get(0).getName()).isEqualTo("Airbag");
        assertThat(response.getAlbum().getTracks().getTrack().get(0).getFormattedDuration()).isEqualTo("4:44");
    }

    @Test
    void getAlbumInfoReturnsNullWhenRemoteServiceReturnsNotFound() {

        mockServer
                .expect(requestTo(startsWith("https://ws.audioscrobbler.com/2.0/")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        LastFmAlbumResponse response = lastFmService.getAlbumInfo("Radiohead", "Nonexistent Album");

        assertThat(response).isNull();
    }
}