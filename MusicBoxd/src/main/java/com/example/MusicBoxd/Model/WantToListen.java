package com.example.MusicBoxd.Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "WANT_TO_LISTEN")
@Getter @Setter @NoArgsConstructor
public class WantToListen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "song_id")
    private Long songId;

    @Column(name = "album_id")
    private Long albumId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public WantToListen(
            Long userId,
            Long songId,
            Long albumId
    ) {
        this.userId = userId;
        this.songId = songId;
        this.albumId = albumId;
        this.createdAt = LocalDateTime.now();
    }
}
