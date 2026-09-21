package com.example.MusicBoxd.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "USER_FAVOURITE_ARTISTS")
@Getter
@Setter
@NoArgsConstructor
public class UserFavouriteArtist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "artist_id", nullable = false)
    private Long artistId;

    public UserFavouriteArtist(
            Long userId,
            Long artistId
    ) {
        this.userId = userId;
        this.artistId = artistId;
    }
}