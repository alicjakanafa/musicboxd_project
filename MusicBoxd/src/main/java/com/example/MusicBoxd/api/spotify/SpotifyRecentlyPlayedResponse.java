package com.example.MusicBoxd.api.spotify;

import java.util.List;

public class SpotifyRecentlyPlayedResponse {

    private List<RecentlyPlayedItem> items;

    public List<RecentlyPlayedItem> getItems() {
        return items;
    }

    public void setItems(
            List<RecentlyPlayedItem> items
    ) {
        this.items = items;
    }

    public static class RecentlyPlayedItem {

        private SpotifyTrack track;

        public SpotifyTrack getTrack() {
            return track;
        }

        public void setTrack(
                SpotifyTrack track
        ) {
            this.track = track;
        }
    }
}