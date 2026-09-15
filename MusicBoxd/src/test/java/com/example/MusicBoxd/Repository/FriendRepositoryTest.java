package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Friend;
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
}
