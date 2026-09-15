package com.example.MusicBoxd.api.itunes;

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
        assertThat(album.getArtworkUrl600()).isEqualTo("https://example.com/600x600.jpg");
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
}
