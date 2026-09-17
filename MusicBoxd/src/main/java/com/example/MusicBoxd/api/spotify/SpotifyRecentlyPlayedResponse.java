package com.example.MusicBoxd.api.spotify;

import java.util.List;

public class SpotifyRecentlyPlayedResponse {

    private List<SpotifyRecentlyPlayedItem> items;

    public List<SpotifyRecentlyPlayedItem> getItems() {
        return items;
    }

    public void setItems(List<SpotifyRecentlyPlayedItem> items) {
        this.items = items;
    }

    public static class SpotifyRecentlyPlayedItem {

        private SpotifyTrack track;

        private String played_at;

        public SpotifyTrack getTrack() {
            return track;
        }

        public void setTrack(SpotifyTrack track) {
            this.track = track;
        }

        public String getPlayed_at() {
            return played_at;
        }

        public void setPlayed_at(String played_at) {
            this.played_at = played_at;
        }
    }

    public static class SpotifyTrack {

        private String id;

        private String name;

        private int duration_ms;

        private List<SpotifyArtist> artists;

        private SpotifyAlbum album;

        private SpotifyExternalUrls externalUrls;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getDuration_ms() {
            return duration_ms;
        }

        public void setDuration_ms(int duration_ms) {
            this.duration_ms = duration_ms;
        }

        public List<SpotifyArtist> getArtists() {
            return artists;
        }

        public void setArtists(List<SpotifyArtist> artists) {
            this.artists = artists;
        }

        public SpotifyAlbum getAlbum() {
            return album;
        }

        public void setAlbum(SpotifyAlbum album) {
            this.album = album;
        }

        public SpotifyExternalUrls getExternalUrls() {
            return externalUrls;
        }

        public void setExternalUrls(SpotifyExternalUrls externalUrls) {
            this.externalUrls = externalUrls;
        }
    }

    public static class SpotifyAlbum {

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

    public static class SpotifyImage {

        private String url;

        private int height;

        private int width;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }
    }

    public static class SpotifyExternalUrls {

        private String spotify;

        public String getSpotify() {
            return spotify;
        }

        public void setSpotify(String spotify) {
            this.spotify = spotify;
        }
    }
}