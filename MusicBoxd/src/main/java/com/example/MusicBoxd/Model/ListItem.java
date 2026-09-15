package com.example.MusicBoxd.Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "LIST_ITEMS")
@Getter @Setter @NoArgsConstructor
public class ListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "list_id")
    private Long listId;

    @Column(name = "album_id")
    private Long albumId;

    @Column(name = "song_id")
    private Long songId;

    private Integer position;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public ListItem(
            Long listId,
            Long albumId,
            Long songId,
            Integer position
    ) {
        this.listId = listId;
        this.albumId = albumId;
        this.songId = songId;
        this.position = position;
        this.createdAt = LocalDateTime.now();
    }
}
