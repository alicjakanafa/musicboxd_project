package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Friend;
import com.example.MusicBoxd.Model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

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


    @Query("""
            SELECT COUNT(f) > 0
            FROM Friend f
            WHERE f.requesterId = :#{#requester.id}
            AND f.receiverId = :#{#receiver.id}
            AND f.status = :status
            """)
    boolean existsByRequesterAndReceiverAndStatus(
            @Param("requester") User requester,
            @Param("receiver") User receiver,
            @Param("status") Friend.Status status
    );


    @Query("""
            SELECT COUNT(f) > 0
            FROM Friend f
            WHERE f.receiverId = :#{#receiver.id}
            AND f.requesterId = :#{#requester.id}
            AND f.status = :status
            """)
    boolean existsByReceiverAndRequesterAndStatus(
            @Param("receiver") User receiver,
            @Param("requester") User requester,
            @Param("status") Friend.Status status
    );


    List<Friend> findByRequesterIdAndStatusOrReceiverIdAndStatus(
            Long requesterId,
            String requesterStatus,
            Long receiverId,
            String receiverStatus
    );

    long countByReceiverIdAndStatus(
            Long receiverId,
            String status
    );

    long countByRequesterIdAndStatus(
            Long requesterId,
            String status
    );
}
