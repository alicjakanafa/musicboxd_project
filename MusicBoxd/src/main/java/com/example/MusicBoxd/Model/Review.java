package com.example.MusicBoxd.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "REVIEWS")
@Getter @Setter @NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "album_id")
    private Long albumId;

    @Column(name = "song_id")
    private Long songId;

    private String header;

    private String content;

    private BigDecimal rating;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Review(
            Long userId,
            Long albumId,
            Long songId,
            String header,
            String content,
            BigDecimal rating
    ) {
        this.userId = userId;
        this.albumId = albumId;
        this.songId = songId;
        this.header = header;
        this.content = content;
        this.rating = rating;
        this.createdAt = LocalDateTime.now();
    }
}
