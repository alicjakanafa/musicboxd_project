package com.example.MusicBoxd.service;

import com.example.MusicBoxd.Model.Notification;
import com.example.MusicBoxd.Repository.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(
            NotificationRepository notificationRepository
    ) {
        this.notificationRepository =
                notificationRepository;
    }

    public void createNotification(
            Long userId,
            Long actorId,
            Long relatedId,
            String type,
            String notificationText
    ) {

        // Don't notify someone about their own action
        if (userId.equals(actorId)) {
            return;
        }

        Notification notification =
                new Notification(
                        userId,
                        actorId,
                        relatedId,
                        type,
                        notificationText
                );

        notificationRepository.save(notification);
    }

    public void notifyFriendRequest(
            Long receiverId,
            Long requesterId,
            String requesterUsername
    ) {

        createNotification(
                receiverId,
                requesterId,
                requesterId,
                "FRIEND_REQUEST",
                requesterUsername + " sent you a friend request."
        );
    }

    public void notifyFriendAccepted(
            Long requesterId,
            Long accepterId,
            String accepterUsername
    ) {

        createNotification(
                requesterId,
                accepterId,
                accepterId,
                "FRIEND_ACCEPTED",
                accepterUsername + " accepted your friend request."
        );
    }

    public void notifyFriendReviewed(
            Long friendId,
            Long reviewerId,
            Long reviewId,
            String reviewerUsername
    ) {

        createNotification(
                friendId,
                reviewerId,
                reviewId,
                "ALBUM_REVIEWED",
                reviewerUsername + " posted a new review."
        );
    }
}