package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Notification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("datajpatest")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void savesNotificationAndPopulatesGeneratedIdAndCreatedAt() {
        Notification notification = new Notification(1L, 2L, 3L, "LIKE", "liked your review");

        Notification saved = notificationRepository.save(notification);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findByIdReturnsAllPersistedFields() {
        Notification notification = new Notification(4L, 5L, 6L, "COMMENT", "commented on your review");
        Notification persisted = entityManager.persistFlushFind(notification);

        Optional<Notification> found = notificationRepository.findById(persisted.getId());

        assertThat(found).isPresent();
        Notification result = found.get();
        assertThat(result.getUserId()).isEqualTo(4L);
        assertThat(result.getActorId()).isEqualTo(5L);
        assertThat(result.getRelatedId()).isEqualTo(6L);
        assertThat(result.getType()).isEqualTo("COMMENT");
        assertThat(result.getNotificationText()).isEqualTo("commented on your review");
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void findAllReturnsAllPersistedNotifications() {
        entityManager.persistAndFlush(new Notification(1L, 2L, 3L, "LIKE", "liked your review"));
        entityManager.persistAndFlush(new Notification(1L, 3L, 4L, "FRIEND_REQUEST", "sent you a friend request"));

        List<Notification> notifications = (List<Notification>) notificationRepository.findAll();

        assertThat(notifications).hasSize(2)
                .extracting(Notification::getType)
                .containsExactlyInAnyOrder("LIKE", "FRIEND_REQUEST");
    }

    @Test
    void deleteByIdRemovesNotification() {
        Notification persisted = entityManager.persistFlushFind(
                new Notification(1L, 2L, 3L, "LIKE", "liked your review"));

        notificationRepository.deleteById(persisted.getId());

        assertThat(notificationRepository.findById(persisted.getId())).isEmpty();
    }

    @Test
    void newNotificationDefaultsToUnread() {
        Notification notification = new Notification(1L, 2L, 3L, "LIKE", "liked your review");

        Notification saved = entityManager.persistFlushFind(notification);

        assertThat(saved.isRead()).isFalse();
    }
}
