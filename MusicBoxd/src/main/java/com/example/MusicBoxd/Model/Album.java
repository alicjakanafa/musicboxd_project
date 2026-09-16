package com.example.MusicBoxd.Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ALBUMS")
@Getter
@Setter
@NoArgsConstructor
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "artist_id")
    private Long artistId;

    private String title;

    @Column(name = "release_year")
    private Short releaseYear;

    @Column(name = "artwork_url")
    private String artworkUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Album(
            String externalId,
            Long artistId,
            String title,
            Short releaseYear,
            String artworkUrl
    ) {
        this.externalId = externalId;
        this.artistId = artistId;
        this.title = title;
        this.releaseYear = releaseYear;
        this.artworkUrl = artworkUrl;
        this.createdAt = LocalDateTime.now();
    }
}
