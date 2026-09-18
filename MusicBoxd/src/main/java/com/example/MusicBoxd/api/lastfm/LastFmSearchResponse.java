package com.example.MusicBoxd.api.lastfm;

import java.util.List;

public class LastFmSearchResponse {

    private Results results;

    public Results getResults() {
        return results;
    }

    public void setResults(Results results) {
        this.results = results;
    }

    public static class Results {

        private AlbumMatches albummatches;

        public AlbumMatches getAlbummatches() {
            return albummatches;
        }

        public void setAlbummatches(
                AlbumMatches albummatches
        ) {
            this.albummatches = albummatches;
        }
    }

    public static class AlbumMatches {

        private List<LastFmSearchAlbum> album;

        public List<LastFmSearchAlbum> getAlbum() {
            return album;
        }

        public void setAlbum(
                List<LastFmSearchAlbum> album
        ) {
            this.album = album;
        }
    }
}