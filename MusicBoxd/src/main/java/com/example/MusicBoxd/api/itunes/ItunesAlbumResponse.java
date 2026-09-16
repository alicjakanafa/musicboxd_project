package com.example.MusicBoxd.api.itunes;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ItunesAlbumResponse {

    private int resultCount;
    private List<ItunesAlbum> results;

}
