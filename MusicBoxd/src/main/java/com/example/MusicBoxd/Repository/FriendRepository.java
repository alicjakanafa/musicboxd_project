package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Friend;
import com.example.MusicBoxd.Model.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRepository
        extends CrudRepository<Friend, Long> {

    Optional<Friend> findByRequesterIdAndReceiverId(
            Long requesterId,
            Long receiverId
    );

    Optional<Friend> findByReceiverIdAndRequesterId(
            Long receiverId,
            Long requesterId
    );

    List<Friend> findByReceiverIdAndStatus(
            Long receiverId,
            String status
    );

    List<Friend> findByRequesterIdAndStatus(
            Long requesterId,
            String status
    );

    List<Friend> findByStatus(
            String status
    );

    boolean existsByReceiverAndRequesterAndStatus(
            User receiver,
            User requester,
            Friend.Status status
    );

    boolean existsByRequesterAndReceiverAndStatus(
            User requester,
            User receiver,
            Friend.Status status
    );
}
