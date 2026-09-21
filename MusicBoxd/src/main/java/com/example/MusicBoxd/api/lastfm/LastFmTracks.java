package com.example.MusicBoxd.api.lastfm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LastFmTracks {

    private List<LastFmTrack> track;

    public List<LastFmTrack> getTrack() {
        return track;
    }

    public void setTrack(List<LastFmTrack> track) {
        this.track = track;
    }
}