package com.example.MusicBoxd.api.lastfm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class LastFmService {

    private final RestTemplate restTemplate;

    private final String apiKey;

    public LastFmService(
            RestTemplate restTemplate,
             @Value("${lastfm.api.key}") String apiKey
    ) {
        this.restTemplate = restTemplate;

        this.apiKey =  apiKey;
    }


    // =========================
    // TOP 40
    // =========================

    public LastFmResponse getTopArtists() {

        String url = UriComponentsBuilder
                .fromUriString(
                        "https://ws.audioscrobbler.com/2.0/"
                )
                .queryParam("method", "chart.gettopartists")
                .queryParam("api_key", apiKey)
                .queryParam("format", "json")
                .queryParam("limit", 40)
                .build()
                .toUriString();

        return restTemplate.getForObject(
                url,
                LastFmResponse.class
        );
    }


    // =========================
    // ARTIST INFORMATION
    // =========================

    public LastFmArtistResponse getArtistInfo(
            String artistName
    ) {

        String url = UriComponentsBuilder
                .fromUriString(
                        "https://ws.audioscrobbler.com/2.0/"
                )
                .queryParam("method", "artist.getinfo")
                .queryParam("artist", artistName)
                .queryParam("api_key", apiKey)
                .queryParam("format", "json")
                .queryParam("autocorrect", 1)
                .build()
                .toUriString();

        return restTemplate.getForObject(
                url,
                LastFmArtistResponse.class
        );
    }


    // =========================
    // ARTIST ALBUMS
    // =========================

    public LastFmTopAlbumsResponse getArtistAlbums(
            String artistName
    ) {

        String url = UriComponentsBuilder
                .fromUriString(
                        "https://ws.audioscrobbler.com/2.0/"
                )
                .queryParam("method", "artist.gettopalbums")
                .queryParam("artist", artistName)
                .queryParam("api_key", apiKey)
                .queryParam("format", "json")
                .queryParam("limit", 50)
                .queryParam("autocorrect", 1)
                .build()
                .toUriString();

        return restTemplate.getForObject(
                url,
                LastFmTopAlbumsResponse.class
        );
    }


    // =========================
    // ALBUM TRACKS
    // =========================

    public LastFmAlbumResponse getAlbumInfo(
            String artistName,
            String albumName
    ) {

        String url = UriComponentsBuilder
                .fromUriString(
                        "https://ws.audioscrobbler.com/2.0/"
                )
                .queryParam("method", "album.getinfo")
                .queryParam("artist", artistName)
                .queryParam("album", albumName)
                .queryParam("api_key", apiKey)
                .queryParam("format", "json")
                .queryParam("autocorrect", 1)
                .build()
                .toUriString();

        return restTemplate.getForObject(
                url,
                LastFmAlbumResponse.class
        );
    }
}
