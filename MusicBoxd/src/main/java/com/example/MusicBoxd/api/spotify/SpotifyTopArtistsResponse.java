package com.example.MusicBoxd.api.spotify;

import java.util.List;

public class SpotifyTopArtistsResponse {

    private List<SpotifyArtist> items;

    public List<SpotifyArtist> getItems() {
        return items;
    }

    public void setItems(List<SpotifyArtist> items) {
        this.items = items;
    }
}