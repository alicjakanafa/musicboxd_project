package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Notification;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface NotificationRepository
        extends CrudRepository<Notification, Long> {

    List<Notification>
    findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification>
    findTop6ByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification>
    findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFalse(Long userId);
}