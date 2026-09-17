package com.example.MusicBoxd.api.lastfm;

import java.util.List;

public class LastFmSearchAlbum {

    private String name;
    private String artist;
    private String url;
    private List<LastFmImage> image;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
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