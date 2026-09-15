package com.example.MusicBoxd.Repository;

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
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesUserAndPopulatesGeneratedIdAndCreatedAt() {
        User user = new User("google-123", "listener1", "listener1@example.com", "bio text", "http://pic.example/1.png");

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findByIdReturnsAllPersistedFields() {
        User user = new User("google-456", "listener2", "listener2@example.com", "another bio", "http://pic.example/2.png");
        User persisted = entityManager.persistFlushFind(user);

        Optional<User> found = userRepository.findById(persisted.getId());

        assertThat(found).isPresent();
        User result = found.get();
        assertThat(result.getGoogleUserId()).isEqualTo("google-456");
        assertThat(result.getUsername()).isEqualTo("listener2");
        assertThat(result.getEmail()).isEqualTo("listener2@example.com");
        assertThat(result.getBio()).isEqualTo("another bio");
        assertThat(result.getProfilePictureUrl()).isEqualTo("http://pic.example/2.png");
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void savesUserWithOnlyRequiredFieldsPopulated() {
        User user = new User(null, "minimal-user", null, null, null);

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("minimal-user");
        assertThat(saved.getGoogleUserId()).isNull();
        assertThat(saved.getEmail()).isNull();
        assertThat(saved.getBio()).isNull();
        assertThat(saved.getProfilePictureUrl()).isNull();
    }

    @Test
    void findAllReturnsAllPersistedUsers() {
        entityManager.persistAndFlush(new User("g1", "user-one", "one@example.com", null, null));
        entityManager.persistAndFlush(new User("g2", "user-two", "two@example.com", null, null));

        List<User> users = (List<User>) userRepository.findAll();

        assertThat(users).hasSize(2)
                .extracting(User::getUsername)
                .containsExactlyInAnyOrder("user-one", "user-two");
    }

    @Test
    void deleteByIdRemovesUser() {
        User persisted = entityManager.persistFlushFind(new User("g3", "user-three", "three@example.com", null, null));

        userRepository.deleteById(persisted.getId());

        assertThat(userRepository.findById(persisted.getId())).isEmpty();
    }
}
