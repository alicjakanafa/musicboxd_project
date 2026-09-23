package com.example.MusicBoxd.api.itunes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ItunesServiceTest {

    private ItunesService itunesService;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        itunesService = new ItunesService();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(itunesService, "restTemplate");
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @AfterEach
    void tearDown() {
        mockServer.verify();
    }

    @Test
    void searchAlbumsBuildsExpectedRequestAndParsesResponse() {
        String responseBody = """
                {
                  "resultCount": 1,
                  "results": [
                    {
                      "collectionId": 1440935467,
                      "collectionName": "1989 (Taylor's Version)",
                      "artistName": "Taylor Swift",
                      "artworkUrl100": "https://example.com/100x100.jpg",
                      "artworkUrl600": "https://example.com/600x600.jpg",
                      "releaseDate": "2023-10-27T07:00:00Z",
                      "primaryGenreName": "Pop"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(startsWith("https://itunes.apple.com/search")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("term", "1989"))
                .andExpect(queryParam("media", "music"))
                .andExpect(queryParam("entity", "album"))
                .andExpect(queryParam("limit", "20"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        ItunesAlbumResponse response = itunesService.searchAlbums("1989");

        assertThat(response).isNotNull();
        assertThat(response.getResultCount()).isEqualTo(1);
        assertThat(response.getResults()).hasSize(1);

        ItunesAlbum album = response.getResults().get(0);
        assertThat(album.getCollectionId()).isEqualTo(1440935467L);
        assertThat(album.getCollectionName()).isEqualTo("1989 (Taylor's Version)");
        assertThat(album.getArtistName()).isEqualTo("Taylor Swift");
        assertThat(album.getArtworkUrl100()).isEqualTo("https://example.com/100x100.jpg");
        assertThat(album.getReleaseDate()).isEqualTo("2023-10-27T07:00:00Z");
        assertThat(album.getPrimaryGenreName()).isEqualTo("Pop");
    }

    @Test
    void searchAlbumsParsesResponseServedAsTextJavascriptContentType() {
        String responseBody = """
                {
                  "resultCount": 1,
                  "results": [
                    {
                      "collectionId": 42,
                      "collectionName": "OK Computer",
                      "artistName": "Radiohead"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(startsWith("https://itunes.apple.com/search")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("term", "radiohead"))
                .andRespond(withSuccess(responseBody, MediaType.valueOf("text/javascript")));

        ItunesAlbumResponse response = itunesService.searchAlbums("radiohead");

        assertThat(response).isNotNull();
        assertThat(response.getResultCount()).isEqualTo(1);
        assertThat(response.getResults().get(0).getCollectionName()).isEqualTo("OK Computer");
        assertThat(response.getResults().get(0).getArtistName()).isEqualTo("Radiohead");
    }

    @Test
    void searchTracksBuildsExpectedRequestAndParsesResponse() {
        String responseBody = """
                {
                  "resultCount": 1,
                  "results": [
                    {
                      "trackId": 999,
                      "trackName": "Karma Police",
                      "artistName": "Radiohead",
                      "collectionName": "OK Computer",
                      "artworkUrl100": "https://example.com/track100.jpg",
                      "previewUrl": "https://example.com/preview.m4a",
                      "trackNumber": 6,
                      "trackTimeMillis": 261000
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(startsWith("https://itunes.apple.com/search")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("term", "karma%20police"))
                .andExpect(queryParam("media", "music"))
                .andExpect(queryParam("entity", "song"))
                .andExpect(queryParam("limit", "20"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        ItunesTrackResponse response = itunesService.searchTracks("karma police");

        assertThat(response).isNotNull();
        assertThat(response.getResultCount()).isEqualTo(1);
        assertThat(response.getResults()).hasSize(1);

        ItunesTrack track = response.getResults().get(0);
        assertThat(track.getTrackId()).isEqualTo(999L);
        assertThat(track.getTrackName()).isEqualTo("Karma Police");
        assertThat(track.getArtistName()).isEqualTo("Radiohead");
        assertThat(track.getCollectionName()).isEqualTo("OK Computer");
        assertThat(track.getArtworkUrl100()).isEqualTo("https://example.com/track100.jpg");
        assertThat(track.getPreviewUrl()).isEqualTo("https://example.com/preview.m4a");
        assertThat(track.getTrackNumber()).isEqualTo(6);
        assertThat(track.getTrackTimeMillis()).isEqualTo(261000L);
    }

    @Test
    void searchAlbumsReturnsEmptyResultsWhenNoMatchesFound() {
        String responseBody = """
                {
                  "resultCount": 0,
                  "results": []
                }
                """;

        mockServer.expect(requestTo(startsWith("https://itunes.apple.com/search")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("term", "zzzznonexistentzzzz"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        ItunesAlbumResponse response = itunesService.searchAlbums("zzzznonexistentzzzz");

        assertThat(response).isNotNull();
        assertThat(response.getResultCount()).isEqualTo(0);
        assertThat(response.getResults()).isEmpty();
    }

    @Test
    void searchAlbumsByArtistBuildsExpectedRequestWithHighResultLimit() {
        String responseBody = """
                {
                  "resultCount": 1,
                  "results": [
                    {
                      "collectionId": 111,
                      "collectionName": "In Rainbows",
                      "artistName": "Radiohead"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(startsWith("https://itunes.apple.com/search")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("term", "Radiohead"))
                .andExpect(queryParam("media", "music"))
                .andExpect(queryParam("entity", "album"))
                .andExpect(queryParam("limit", "200"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        ItunesAlbumResponse response = itunesService.searchAlbumsByArtist("Radiohead");

        assertThat(response).isNotNull();
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getCollectionName()).isEqualTo("In Rainbows");
    }

    @Test
    void getAlbumTracksBuildsLookupRequestWithCollectionIdAndSongEntity() {
        String responseBody = """
                {
                  "resultCount": 1,
                  "results": [
                    {
                      "trackId": 55,
                      "trackName": "15 Step",
                      "artistName": "Radiohead",
                      "collectionName": "In Rainbows"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(startsWith("https://itunes.apple.com/lookup")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("id", "111"))
                .andExpect(queryParam("entity", "song"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        ItunesTrackResponse response = itunesService.getAlbumTracks(111L);

        assertThat(response).isNotNull();
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getTrackName()).isEqualTo("15 Step");
    }

    @Test
    void getDailyAlbumReturnsNullWhenSearchResultsAreEmpty() {
        mockServer.expect(requestTo(startsWith("https://itunes.apple.com/search")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("entity", "album"))
                .andExpect(queryParam("limit", "50"))
                .andRespond(withSuccess(
                        "{ \"resultCount\": 0, \"results\": [] }",
                        MediaType.APPLICATION_JSON));

        ItunesAlbum album = itunesService.getDailyAlbum(42L);

        assertThat(album).isNull();
    }

    @Test
    void getDailyAlbumReturnsNullWhenResultsFieldIsExplicitlyNull() {
        mockServer.expect(requestTo(startsWith("https://itunes.apple.com/search")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{ \"resultCount\": 0, \"results\": null }",
                        MediaType.APPLICATION_JSON));

        ItunesAlbum album = itunesService.getDailyAlbum(42L);

        assertThat(album).isNull();
    }

    @Test
    void getDailyAlbumReturnsNullWhenRemoteResponseBodyIsEmpty() {
        mockServer.expect(requestTo(startsWith("https://itunes.apple.com/search")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        ItunesAlbum album = itunesService.getDailyAlbum(42L);

        assertThat(album).isNull();
    }

    @Test
    void getDailyAlbumReturnsAnAlbumFromTheSearchResultsForAGivenUser() {
        String responseBody = """
                {
                  "resultCount": 3,
                  "results": [
                    { "collectionId": 1, "collectionName": "Album One", "artistName": "Artist One" },
                    { "collectionId": 2, "collectionName": "Album Two", "artistName": "Artist Two" },
                    { "collectionId": 3, "collectionName": "Album Three", "artistName": "Artist Three" }
                  ]
                }
                """;

        mockServer.expect(requestTo(startsWith("https://itunes.apple.com/search")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("media", "music"))
                .andExpect(queryParam("entity", "album"))
                .andExpect(queryParam("limit", "50"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        ItunesAlbum album = itunesService.getDailyAlbum(7L);

        assertThat(album).isNotNull();
        assertThat(album.getCollectionId()).isIn(1L, 2L, 3L);
        assertThat(List.of("Album One", "Album Two", "Album Three"))
                .contains(album.getCollectionName());
    }

    @Test
    void getDailyAlbumReturnsSameAlbumForSameUserOnSameDay() {
        String responseBody = """
                {
                  "resultCount": 3,
                  "results": [
                    { "collectionId": 1, "collectionName": "Album One", "artistName": "Artist One" },
                    { "collectionId": 2, "collectionName": "Album Two", "artistName": "Artist Two" },
                    { "collectionId": 3, "collectionName": "Album Three", "artistName": "Artist Three" }
                  ]
                }
                """;

        mockServer.expect(ExpectedCount.times(2), requestTo(startsWith("https://itunes.apple.com/search")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        ItunesAlbum firstCall = itunesService.getDailyAlbum(99L);
        ItunesAlbum secondCall = itunesService.getDailyAlbum(99L);

        assertThat(firstCall).isNotNull();
        assertThat(secondCall).isNotNull();
        assertThat(secondCall.getCollectionId()).isEqualTo(firstCall.getCollectionId());
        assertThat(secondCall.getCollectionName()).isEqualTo(firstCall.getCollectionName());
    }

    @Test
    void getDailyAlbumWorksWithoutAUserIdUsingEpochDaySeed() {
        String responseBody = """
                {
                  "resultCount": 2,
                  "results": [
                    { "collectionId": 10, "collectionName": "Fallback Album One", "artistName": "Artist One" },
                    { "collectionId": 20, "collectionName": "Fallback Album Two", "artistName": "Artist Two" }
                  ]
                }
                """;

        mockServer.expect(requestTo(startsWith("https://itunes.apple.com/search")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        ItunesAlbum album = itunesService.getDailyAlbum(null);

        assertThat(album).isNotNull();
        assertThat(album.getCollectionId()).isIn(10L, 20L);
    }
}
