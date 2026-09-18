package com.example.MusicBoxd.api.lastfm;

import java.util.List;

public class LastFmArtist {

    private String name;

    private String mbid;

    private String url;

    private List<LastFmImage> image;

    private String listeners;

    private String playcount;

    private LastFmStats stats;

    private LastFmBio bio;


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


    public String getListeners() {
        return listeners;
    }

    public void setListeners(String listeners) {
        this.listeners = listeners;
    }


    public String getPlaycount() {
        return playcount;
    }

    public void setPlaycount(String playcount) {
        this.playcount = playcount;
    }


    public LastFmStats getStats() {
        return stats;
    }

    public void setStats(LastFmStats stats) {
        this.stats = stats;
    }


    public LastFmBio getBio() {
        return bio;
    }

    public void setBio(LastFmBio bio) {
        this.bio = bio;
    }
}