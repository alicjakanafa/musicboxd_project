package com.example.MusicBoxd.api.lastfm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class LastFmService {

    private final RestTemplate restTemplate;
    private final String apiKey;

    public LastFmService(@Value("${lastfm.api.key}") String apiKey) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
    }

    public LastFmResponse getTopArtists() {

        String url = UriComponentsBuilder
                .fromUriString("https://ws.audioscrobbler.com/2.0/")
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
}
