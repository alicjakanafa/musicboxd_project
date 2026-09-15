package com.example.MusicBoxd.api.spotify;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SpotifyRecentlyPlayedResponse {

    private List<SpotifyRecentlyPlayedItem> items;

    public List<SpotifyRecentlyPlayedItem> getItems() {
        return items;
    }

    public void setItems(List<SpotifyRecentlyPlayedItem> items) {
        this.items = items;
    }

    public static class SpotifyRecentlyPlayedItem {

        private SpotifyTrack track;

        @JsonProperty("played_at")
        private String playedAt;

        public SpotifyTrack getTrack() {
            return track;
        }

        public void setTrack(SpotifyTrack track) {
            this.track = track;
        }

        public String getPlayedAt() {
            return playedAt;
        }

        public void setPlayedAt(String playedAt) {
            this.playedAt = playedAt;
        }
    }
}