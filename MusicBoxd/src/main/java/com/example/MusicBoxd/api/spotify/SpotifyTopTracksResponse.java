package com.example.MusicBoxd.api.spotify;

import java.util.List;

public class SpotifyTopTracksResponse {

    private List<SpotifyTrack> items;

    public List<SpotifyTrack> getItems() {
        return items;
    }

    public void setItems(List<SpotifyTrack> items) {
        this.items = items;
    }
}
