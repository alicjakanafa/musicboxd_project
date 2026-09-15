package com.example.MusicBoxd.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "MESSAGES")
@Getter @Setter @NoArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id")
    private Long senderId;

    @Column(name = "receiver_id")
    private Long receiverId;

    private String content;

    private boolean read;

    @Column(name = "song_title")
    private String songTitle;

    @Column(name = "song_artist")
    private String songArtist;

    @Column(name = "song_image_url")
    private String songImageUrl;

    @Column(name = "song_preview_url")
    private String songPreviewUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Message(
            Long senderId,
            Long receiverId,
            String content,
            String songTitle,
            String songArtist,
            String songImageUrl,
            String songPreviewUrl
    ) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.read = false;
        this.songTitle = songTitle;
        this.songArtist = songArtist;
        this.songImageUrl = songImageUrl;
        this.songPreviewUrl = songPreviewUrl;
        this.createdAt = LocalDateTime.now();
    }

}


