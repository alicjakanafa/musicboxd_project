package com.example.MusicBoxd.api.itunes;

import java.util.List;

public class ItunesTrackResponse {

    private int resultCount;
    private List<ItunesTrack> results;

    public int getResultCount() {
        return resultCount;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    public List<ItunesTrack> getResults() {
        return results;
    }

    public void setResults(List<ItunesTrack> results) {
        this.results = results;
    }
}