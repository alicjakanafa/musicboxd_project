package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Friend;
import com.example.MusicBoxd.Model.User;
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
class FriendRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FriendRepository friendRepository;

    @Test
    void savesFriendAndPopulatesGeneratedIdAndCreatedAt() {
        Friend friend = new Friend(1L, 2L, "PENDING");

        Friend saved = friendRepository.save(friend);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findByIdReturnsAllPersistedFields() {
        Friend friend = new Friend(3L, 4L, "PENDING");
        Friend persisted = entityManager.persistFlushFind(friend);

        Optional<Friend> found = friendRepository.findById(persisted.getId());

        assertThat(found).isPresent();
        Friend result = found.get();
        assertThat(result.getRequesterId()).isEqualTo(3L);
        assertThat(result.getReceiverId()).isEqualTo(4L);
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void findAllReturnsAllPersistedFriends() {
        entityManager.persistAndFlush(new Friend(1L, 2L, "PENDING"));
        entityManager.persistAndFlush(new Friend(5L, 6L, "ACCEPTED"));

        List<Friend> friends = (List<Friend>) friendRepository.findAll();

        assertThat(friends).hasSize(2)
                .extracting(Friend::getStatus)
                .containsExactlyInAnyOrder("PENDING", "ACCEPTED");
    }

    @Test
    void deleteByIdRemovesFriend() {
        Friend persisted = entityManager.persistFlushFind(new Friend(7L, 8L, "PENDING"));

        friendRepository.deleteById(persisted.getId());

        assertThat(friendRepository.findById(persisted.getId())).isEmpty();
    }

    @Test
    void persistsFriendStatusTransitionToAccepted() {
        Friend persisted = entityManager.persistFlushFind(new Friend(9L, 10L, "PENDING"));

        persisted.setStatus("ACCEPTED");
        Friend updated = entityManager.persistFlushFind(persisted);

        assertThat(updated.getStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void findByRequesterIdAndReceiverIdReturnsMatchingFriendRequest() {
        entityManager.persistAndFlush(new Friend(11L, 12L, "PENDING"));

        Optional<Friend> found = friendRepository.findByRequesterIdAndReceiverId(11L, 12L);

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void findByRequesterIdAndReceiverIdReturnsEmptyWhenNoMatch() {
        Optional<Friend> found = friendRepository.findByRequesterIdAndReceiverId(11L, 999L);

        assertThat(found).isEmpty();
    }

    @Test
    void findByReceiverIdAndRequesterIdReturnsMatchingFriendRequest() {
        entityManager.persistAndFlush(new Friend(13L, 14L, "PENDING"));

        Optional<Friend> found = friendRepository.findByReceiverIdAndRequesterId(14L, 13L);

        assertThat(found).isPresent();
        assertThat(found.get().getRequesterId()).isEqualTo(13L);
    }

    @Test
    void findByReceiverIdAndStatusReturnsOnlyMatchingFriendships() {
        entityManager.persistAndFlush(new Friend(15L, 16L, "PENDING"));
        entityManager.persistAndFlush(new Friend(17L, 16L, "ACCEPTED"));
        entityManager.persistAndFlush(new Friend(18L, 19L, "PENDING"));

        List<Friend> found = friendRepository.findByReceiverIdAndStatus(16L, "PENDING");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getRequesterId()).isEqualTo(15L);
    }

    @Test
    void findByRequesterIdAndStatusReturnsOnlyMatchingFriendships() {
        entityManager.persistAndFlush(new Friend(20L, 21L, "PENDING"));
        entityManager.persistAndFlush(new Friend(20L, 22L, "ACCEPTED"));

        List<Friend> found = friendRepository.findByRequesterIdAndStatus(20L, "ACCEPTED");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getReceiverId()).isEqualTo(22L);
    }

    @Test
    void findByStatusReturnsAllFriendshipsWithThatStatus() {
        entityManager.persistAndFlush(new Friend(23L, 24L, "REJECTED"));
        entityManager.persistAndFlush(new Friend(25L, 26L, "PENDING"));
        entityManager.persistAndFlush(new Friend(27L, 28L, "REJECTED"));

        List<Friend> found = friendRepository.findByStatus("REJECTED");

        assertThat(found).hasSize(2)
                .extracting(Friend::getRequesterId)
                .containsExactlyInAnyOrder(23L, 27L);
    }

    @Test
    void findByStatusReturnsEmptyListWhenNoneMatch() {
        List<Friend> found = friendRepository.findByStatus("REJECTED");

        assertThat(found).isEmpty();
    }

    @Test
    void existsByRequesterAndReceiverAndStatusMatchesOnlyTheGivenDirectionAndStatus() {
        User requester = entityManager.persistFlushFind(new User("req-1", "requester", "requester@example.com", null, null));
        User receiver = entityManager.persistFlushFind(new User("rec-1", "receiver", "receiver@example.com", null, null));
        entityManager.persistAndFlush(new Friend(requester.getId(), receiver.getId(), "PENDING"));

        assertThat(friendRepository.existsByRequesterAndReceiverAndStatus(requester, receiver, Friend.Status.PENDING)).isTrue();
        assertThat(friendRepository.existsByRequesterAndReceiverAndStatus(requester, receiver, Friend.Status.ACCEPTED)).isFalse();
        assertThat(friendRepository.existsByRequesterAndReceiverAndStatus(receiver, requester, Friend.Status.PENDING)).isFalse();
    }

    @Test
    void existsByReceiverAndRequesterAndStatusMatchesOnlyTheGivenDirectionAndStatus() {
        User requester = entityManager.persistFlushFind(new User("req-3", "requester3", "requester3@example.com", null, null));
        User receiver = entityManager.persistFlushFind(new User("rec-3", "receiver3", "receiver3@example.com", null, null));
        entityManager.persistAndFlush(new Friend(requester.getId(), receiver.getId(), "ACCEPTED"));

        assertThat(friendRepository.existsByReceiverAndRequesterAndStatus(receiver, requester, Friend.Status.ACCEPTED)).isTrue();
        assertThat(friendRepository.existsByReceiverAndRequesterAndStatus(receiver, requester, Friend.Status.PENDING)).isFalse();
        assertThat(friendRepository.existsByReceiverAndRequesterAndStatus(requester, receiver, Friend.Status.ACCEPTED)).isFalse();
    }

    @Test
    void findByRequesterIdAndStatusOrReceiverIdAndStatusReturnsFriendshipsOnEitherSide() {
        entityManager.persistAndFlush(new Friend(30L, 31L, "ACCEPTED"));
        entityManager.persistAndFlush(new Friend(32L, 30L, "ACCEPTED"));
        entityManager.persistAndFlush(new Friend(30L, 33L, "PENDING"));
        entityManager.persistAndFlush(new Friend(34L, 35L, "ACCEPTED"));

        List<Friend> found = friendRepository.findByRequesterIdAndStatusOrReceiverIdAndStatus(
                30L, "ACCEPTED", 30L, "ACCEPTED");

        assertThat(found).hasSize(2);
    }

    @Test
    void countByReceiverIdAndStatusCountsOnlyMatchingFriendships() {
        entityManager.persistAndFlush(new Friend(40L, 41L, "PENDING"));
        entityManager.persistAndFlush(new Friend(42L, 41L, "PENDING"));
        entityManager.persistAndFlush(new Friend(43L, 41L, "ACCEPTED"));

        long count = friendRepository.countByReceiverIdAndStatus(41L, "PENDING");

        assertThat(count).isEqualTo(2);
    }

    @Test
    void countByRequesterIdAndStatusCountsOnlyMatchingFriendships() {
        entityManager.persistAndFlush(new Friend(50L, 51L, "PENDING"));
        entityManager.persistAndFlush(new Friend(50L, 52L, "ACCEPTED"));

        long count = friendRepository.countByRequesterIdAndStatus(50L, "ACCEPTED");

        assertThat(count).isEqualTo(1);
    }
}
