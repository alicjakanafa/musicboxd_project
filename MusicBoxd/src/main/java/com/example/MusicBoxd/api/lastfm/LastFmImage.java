package com.example.MusicBoxd.api.lastfm;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LastFmImage {

    @JsonProperty("#text")
    private String text;

    private String size;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }
}

