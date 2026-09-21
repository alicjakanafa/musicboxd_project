package com.example.MusicBoxd.api.spotify;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SpotifyTrack {

    private String id;

    private String name;

    private List<SpotifyArtist> artists;

    private SpotifyAlbum album;

    @JsonProperty("external_urls")
    private SpotifyExternalUrls externalUrls;

    @JsonProperty("duration_ms")
    private Long durationMs;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SpotifyArtist> getArtists() {
        return artists;
    }

    public void setArtists(List<SpotifyArtist> artists) {
        this.artists = artists;
    }

    public SpotifyAlbum getAlbum() {
        return album;
    }

    public void setAlbum(SpotifyAlbum album) {
        this.album = album;
    }

    public SpotifyExternalUrls getExternalUrls() {
        return externalUrls;
    }

    public void setExternalUrls(
            SpotifyExternalUrls externalUrls
    ) {
        this.externalUrls = externalUrls;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }
}