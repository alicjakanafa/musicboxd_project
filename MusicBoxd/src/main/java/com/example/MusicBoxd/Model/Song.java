package com.example.MusicBoxd.Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "SONGS")
@Getter @Setter @NoArgsConstructor
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "album_id")
    private Long albumId;

    private String title;

    @Column(name = "track_number")
    private Integer trackNumber;

    @Column(name = "song_url")
    private String songUrl;

    @Column(name = "song_image_url")
    private String songImageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Song(
            String externalId,
            Long albumId,
            String title,
            Integer trackNumber,
            String songUrl,
            String songImageUrl
    ) {
        this.externalId = externalId;
        this.albumId = albumId;
        this.title = title;
        this.trackNumber = trackNumber;
        this.songUrl = songUrl;
        this.songImageUrl = songImageUrl;
        this.createdAt = LocalDateTime.now();
    }
}
