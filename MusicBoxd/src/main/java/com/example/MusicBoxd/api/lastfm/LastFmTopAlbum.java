package com.example.MusicBoxd.api.lastfm;

import java.util.List;

public class LastFmTopAlbum {

    private String name;

    private String mbid;

    private String url;

    private List<LastFmImage> image;

    private LastFmArtist artist;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public LastFmArtist getArtist() {
        return artist;
    }

    public void setArtist(LastFmArtist artist) {
        this.artist = artist;
    }
}
