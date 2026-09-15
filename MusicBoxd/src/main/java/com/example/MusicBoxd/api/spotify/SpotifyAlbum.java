package com.example.MusicBoxd.api.spotify;

import java.util.List;

public class SpotifyAlbum {

    private String name;

    private List<SpotifyImage> images;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SpotifyImage> getImages() {
        return images;
    }

    public void setImages(List<SpotifyImage> images) {
        this.images = images;
    }
}