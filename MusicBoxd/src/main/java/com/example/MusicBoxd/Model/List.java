package com.example.MusicBoxd.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "LISTS")
@Getter @Setter @NoArgsConstructor
public class List {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    private String title;

    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "list_type")
    private ListType listType;

    public List(
            Long userId,
            String title,
            String description,
            ListType type
    ) {
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.listType = type;
        this.createdAt = LocalDateTime.now();
    }

}

