package com.example.MusicBoxd.api.itunes;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class ItunesTrackResponse {

    private int resultCount;
    private List<ItunesTrack> results;
}
