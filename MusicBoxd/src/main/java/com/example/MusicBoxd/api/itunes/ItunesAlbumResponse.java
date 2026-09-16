package com.example.MusicBoxd.api.itunes;

import java.util.List;

public class ItunesAlbumResponse {

    private int resultCount;
    private List<ItunesAlbum> results;

    public int getResultCount() {
        return resultCount;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    public List<ItunesAlbum> getResults() {
        return results;
    }

    public void setResults(List<ItunesAlbum> results) {
        this.results = results;
    }
}
