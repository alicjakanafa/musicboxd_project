package com.example.MusicBoxd.Model;

import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class ArtistTagId implements Serializable {

    private Long artistId;

    private Long tagId;

    public ArtistTagId() {
    }

    public ArtistTagId(Long artistId, Long tagId) {
        this.artistId = artistId;
        this.tagId = tagId;
    }

    public Long getArtistId() {
        return artistId;
    }

    public void setArtistId(Long artistId) {
        this.artistId = artistId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }
}

