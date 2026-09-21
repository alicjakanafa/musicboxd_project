package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Message;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends CrudRepository<Message, Long> {

    List<Message> findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByCreatedAtAsc(
            Long senderIdOne,
            Long receiverIdOne,
            Long senderIdTwo,
            Long receiverIdTwo
    );

    Optional<Message> findTopBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByCreatedAtDesc(
            Long senderIdOne,
            Long receiverIdOne,
            Long senderIdTwo,
            Long receiverIdTwo
    );

    List<Message> findBySenderIdOrReceiverIdOrderByCreatedAtDesc(
            Long senderId,
            Long receiverId
    );

    void deleteBySenderId(Long senderId);

    void deleteByReceiverId(Long receiverId);
}