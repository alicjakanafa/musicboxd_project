package com.example.MusicBoxd.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "top_four")
@Getter
@Setter
@NoArgsConstructor
public class UserFavouriteAlbum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "album_id")
    private Long albumId;

    private Integer position;

    public UserFavouriteAlbum(
            Long userId,
            Long albumId,
            Integer position
    ) {
        this.userId = userId;
        this.albumId = albumId;
        this.position = position;
    }
}