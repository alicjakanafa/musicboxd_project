package com.example.MusicBoxd.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "list_tags")
public class ListTag {

    @EmbeddedId
    private ListTagId id;

    public ListTag() {
    }

    public ListTagId getId() {
        return id;
    }

    public void setId(ListTagId id) {
        this.id = id;
    }
}

