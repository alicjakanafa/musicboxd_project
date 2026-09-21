package com.example.MusicBoxd.api.lastfm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LastFmAlbumResponse {

    private LastFmAlbum album;

    public LastFmAlbum getAlbum() {
        return album;
    }

    public void setAlbum(LastFmAlbum album) {
        this.album = album;
    }
}