package com.example.MusicBoxd.api.itunes;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ItunesTrack {

    private Long trackId;
    private String trackName;
    private String artistName;
    private String collectionName;
    private String artworkUrl100;
    private String previewUrl;
    private Integer trackNumber;
    private Long trackTimeMillis;

    public String getHighResolutionArtworkUrl() {
        if (artworkUrl100 == null) {
            return null;
        }

        return artworkUrl100.replace("100x100bb", "600x600bb");
    }
}