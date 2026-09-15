package com.example.MusicBoxd.api.lastfm;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LastFmServiceTest {

    private static final String API_KEY = "test-api-key";

    private LastFmService lastFmService;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        lastFmService = new LastFmService(API_KEY);
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(lastFmService, "restTemplate");
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
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
                        "playcount": "12345678",
                        "listeners": "2345678",
                        "mbid": "a74b1b7f-71a5-4011-9441-d0b5e4122711",
                        "url": "https://www.last.fm/music/Radiohead",
                        "image": [
                          { "text": "https://example.com/small.jpg", "size": "small" },
                          { "text": "https://example.com/large.jpg", "size": "large" }
                        ]
                      },
                      {
                        "name": "Taylor Swift",
                        "playcount": "98765432",
                        "listeners": "5432198",
                        "mbid": "20244d07-534f-4eff-b4d4-930878889970",
                        "url": "https://www.last.fm/music/Taylor+Swift",
                        "image": []
                      }
                    ]
                  }
                }
                """;

        mockServer.expect(requestTo(startsWith("https://ws.audioscrobbler.com/2.0/")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("method", "chart.gettopartists"))
                .andExpect(queryParam("api_key", API_KEY))
                .andExpect(queryParam("format", "json"))
                .andExpect(queryParam("limit", "40"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        LastFmResponse response = lastFmService.getTopArtists();

        assertThat(response).isNotNull();
        assertThat(response.getArtists()).isNotNull();
        assertThat(response.getArtists().getArtist()).hasSize(2);

        LastFmArtist radiohead = response.getArtists().getArtist().get(0);
        assertThat(radiohead.getName()).isEqualTo("Radiohead");
        assertThat(radiohead.getPlaycount()).isEqualTo("12345678");
        assertThat(radiohead.getListeners()).isEqualTo("2345678");
        assertThat(radiohead.getMbid()).isEqualTo("a74b1b7f-71a5-4011-9441-d0b5e4122711");
        assertThat(radiohead.getUrl()).isEqualTo("https://www.last.fm/music/Radiohead");
        assertThat(radiohead.getImage()).hasSize(2);
        assertThat(radiohead.getImage().get(0).getText()).isEqualTo("https://example.com/small.jpg");
        assertThat(radiohead.getImage().get(0).getSize()).isEqualTo("small");
        assertThat(radiohead.getImage().get(1).getSize()).isEqualTo("large");

        LastFmArtist taylorSwift = response.getArtists().getArtist().get(1);
        assertThat(taylorSwift.getName()).isEqualTo("Taylor Swift");
        assertThat(taylorSwift.getImage()).isEmpty();
    }

    @Test
    void getTopArtistsUsesInjectedApiKeyInRequest() {
        LastFmService serviceWithDifferentKey = new LastFmService("another-key");
        RestTemplate restTemplate =
                (RestTemplate) ReflectionTestUtils.getField(serviceWithDifferentKey, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        server.expect(requestTo(startsWith("https://ws.audioscrobbler.com/2.0/")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("api_key", "another-key"))
                .andRespond(withSuccess("{\"artists\": {\"artist\": []}}", MediaType.APPLICATION_JSON));

        LastFmResponse response = serviceWithDifferentKey.getTopArtists();

        assertThat(response.getArtists().getArtist()).isEmpty();
        server.verify();
    }
}
