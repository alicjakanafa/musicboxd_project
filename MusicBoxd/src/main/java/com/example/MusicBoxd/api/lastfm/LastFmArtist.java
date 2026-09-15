package com.example.MusicBoxd.api.lastfm;

import java.util.List;

public class LastFmArtist {

    private String name;
    private String playcount;
    private String listeners;
    private String mbid;
    private String url;
    private List<LastFmImage> image;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlaycount() {
        return playcount;
    }

    public void setPlaycount(String playcount) {
        this.playcount = playcount;
    }

    public String getListeners() {
        return listeners;
    }

    public void setListeners(String listeners) {
        this.listeners = listeners;
    }

    public String getMbid() {
        return mbid;
    }

    public void setMbid(String mbid) {
        this.mbid = mbid;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<LastFmImage> getImage() {
        return image;
    }

    public void setImage(List<LastFmImage> image) {
        this.image = image;
    }
}

