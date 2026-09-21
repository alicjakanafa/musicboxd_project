package com.example.MusicBoxd.api.spotify;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SpotifyCurrentlyPlayingResponse {

    private SpotifyTrack item;

    @JsonProperty("is_playing")
    private boolean isPlaying;

    @JsonProperty("progress_ms")
    private Integer progressMs;

    public SpotifyTrack getItem() {
        return item;
    }

    public void setItem(
            SpotifyTrack item
    ) {
        this.item = item;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(
            boolean playing
    ) {
        isPlaying = playing;
    }

    public Integer getProgressMs() {
        return progressMs;
    }

    public void setProgressMs(
            Integer progressMs
    ) {
        this.progressMs = progressMs;
    }
}