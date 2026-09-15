package com.example.MusicBoxd.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "artist_tags")
public class ArtistTag {

    @EmbeddedId
    private ArtistTagId id;

    public ArtistTag() {
    }

    public ArtistTagId getId() {
        return id;
    }

    public void setId(ArtistTagId id) {
        this.id = id;
    }
}

