package com.example.MusicBoxd.api.itunes;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ItunesAlbum {

    private Long collectionId;
    private String collectionName;
    private String artistName;
    private String artworkUrl100;
    private String releaseDate;
    private String primaryGenreName;

    public String getHighResolutionArtworkUrl() {

        if (artworkUrl100 == null) {
            return null;
        }

        return artworkUrl100.replace(
                "100x100bb",
                "600x600bb"
        );
    }
}