package com.example.MusicBoxd.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "LIKES")
@Getter @Setter @NoArgsConstructor
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "review_id")
    private Long reviewId;

    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Like(
            Long userId,
            Long reviewId,
            Long commentId
    ) {
        this.userId = userId;
        this.reviewId = reviewId;
        this.commentId = commentId;
        this.createdAt = LocalDateTime.now();
    }


}

