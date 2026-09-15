package com.example.MusicBoxd.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "COMMENTS")
@Getter @Setter @NoArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "review_id")
    private Long reviewId;

    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Comment(
            Long userId,
            Long reviewId,
            String content
    ) {
        this.userId = userId;
        this.reviewId = reviewId;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
}
