package com.example.MusicBoxd.service;

import com.example.MusicBoxd.Model.Notification;
import com.example.MusicBoxd.Repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        notificationService = new NotificationService(notificationRepository);
    }

    @Test
    void createNotificationSavesNotificationWithGivenFields() {
        notificationService.createNotification(1L, 2L, 3L, "COMMENT", "commented on your review");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getActorId()).isEqualTo(2L);
        assertThat(saved.getRelatedId()).isEqualTo(3L);
        assertThat(saved.getType()).isEqualTo("COMMENT");
        assertThat(saved.getNotificationText()).isEqualTo("commented on your review");
    }

    @Test
    void createNotificationDoesNotSaveWhenUserIsActingOnThemselves() {
        notificationService.createNotification(1L, 1L, 3L, "COMMENT", "commented on your own review");

        verify(notificationRepository, never()).save(Mockito.any());
    }

    @Test
    void notifyFriendRequestCreatesNotificationWithRequesterAsActorAndRelatedId() {
        notificationService.notifyFriendRequest(10L, 20L, "alice");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(10L);
        assertThat(saved.getActorId()).isEqualTo(20L);
        assertThat(saved.getRelatedId()).isEqualTo(20L);
        assertThat(saved.getType()).isEqualTo("FRIEND_REQUEST");
        assertThat(saved.getNotificationText()).isEqualTo("alice sent you a friend request.");
    }

    @Test
    void notifyFriendRequestDoesNothingWhenReceiverIsRequester() {
        notificationService.notifyFriendRequest(10L, 10L, "alice");

        verify(notificationRepository, never()).save(Mockito.any());
    }

    @Test
    void notifyFriendAcceptedCreatesNotificationWithAccepterAsActorAndRelatedId() {
        notificationService.notifyFriendAccepted(30L, 40L, "bob");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(30L);
        assertThat(saved.getActorId()).isEqualTo(40L);
        assertThat(saved.getRelatedId()).isEqualTo(40L);
        assertThat(saved.getType()).isEqualTo("FRIEND_ACCEPTED");
        assertThat(saved.getNotificationText()).isEqualTo("bob accepted your friend request.");
    }

    @Test
    void notifyFriendReviewedCreatesNotificationReferencingReview() {
        notificationService.notifyFriendReviewed(50L, 60L, 70L, "carol");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(50L);
        assertThat(saved.getActorId()).isEqualTo(60L);
        assertThat(saved.getRelatedId()).isEqualTo(70L);
        assertThat(saved.getType()).isEqualTo("ALBUM_REVIEWED");
        assertThat(saved.getNotificationText()).isEqualTo("carol posted a new review.");
    }

    @Test
    void notifyFriendReviewedDoesNothingWhenReviewerIsFriend() {
        notificationService.notifyFriendReviewed(50L, 50L, 70L, "carol");

        verify(notificationRepository, never()).save(Mockito.any());
    }

    @Test
    void notifyReviewLikedCreatesNotificationReferencingReview() {
        notificationService.notifyReviewLiked(80L, 90L, 100L, "dave");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(80L);
        assertThat(saved.getActorId()).isEqualTo(90L);
        assertThat(saved.getRelatedId()).isEqualTo(100L);
        assertThat(saved.getType()).isEqualTo("REVIEW_LIKED");
        assertThat(saved.getNotificationText()).isEqualTo("dave liked your review.");
    }

    @Test
    void notifyReviewLikedDoesNothingWhenOwnerLikesTheirOwnReview() {
        notificationService.notifyReviewLiked(80L, 80L, 100L, "dave");

        verify(notificationRepository, never()).save(Mockito.any());
    }
}
