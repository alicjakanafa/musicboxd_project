package com.example.MusicBoxd.Model;

import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class ListTagId implements Serializable {

    private Long listId;

    private Long tagId;

    public ListTagId() {
    }

    public ListTagId(Long listId, Long tagId) {
        this.listId = listId;
        this.tagId = tagId;
    }

    public Long getListId() {
        return listId;
    }

    public void setListId(Long listId) {
        this.listId = listId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }
}

