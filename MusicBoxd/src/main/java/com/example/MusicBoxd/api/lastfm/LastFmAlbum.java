package com.example.MusicBoxd.api.lastfm;

import java.util.List;

public class LastFmAlbum {

    private String name;
    private String artist;
    private String url;
    private String releasedate;
    private List<LastFmImage> image;
    private LastFmTracks tracks;

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

    public String getReleasedate() {
        return releasedate;
    }

    public void setReleasedate(String releasedate) {
        this.releasedate = releasedate;
    }

    public List<LastFmImage> getImage() {
        return image;
    }

    public void setImage(List<LastFmImage> image) {
        this.image = image;
    }

    public LastFmTracks getTracks() {
        return tracks;
    }

    public void setTracks(LastFmTracks tracks) {
        this.tracks = tracks;
    }
}
