package com.example.MusicBoxd.Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "NOTIFICATIONS")
@Getter @Setter @NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "related_id")
    private Long relatedId;

    private String type;

    @Column(name = "notification_text")
    private String notificationText;

    @Column(name = "is_read")
    private boolean isRead;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Notification(
            Long userId,
            Long actorId,
            Long relatedId,
            String type,
            String notificationText
    ) {
        this.userId = userId;
        this.actorId = actorId;
        this.relatedId = relatedId;
        this.type = type;
        this.notificationText = notificationText;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }
}

