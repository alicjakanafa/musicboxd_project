package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.UserFavouriteArtist;
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
class UserFavouriteArtistRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserFavouriteArtistRepository userFavouriteArtistRepository;

    @Test
    void savesUserFavouriteArtistAndGeneratesId() {
        UserFavouriteArtist favourite = new UserFavouriteArtist(1L, 2L);

        UserFavouriteArtist saved = userFavouriteArtistRepository.save(favourite);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getArtistId()).isEqualTo(2L);
    }

    @Test
    void findByIdReturnsAllPersistedFields() {
        UserFavouriteArtist favourite = new UserFavouriteArtist(3L, 4L);
        UserFavouriteArtist persisted = entityManager.persistFlushFind(favourite);

        Optional<UserFavouriteArtist> found = userFavouriteArtistRepository.findById(persisted.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(3L);
        assertThat(found.get().getArtistId()).isEqualTo(4L);
    }

    @Test
    void deleteByIdRemovesUserFavouriteArtist() {
        UserFavouriteArtist persisted = entityManager.persistFlushFind(new UserFavouriteArtist(5L, 6L));

        userFavouriteArtistRepository.deleteById(persisted.getId());

        assertThat(userFavouriteArtistRepository.findById(persisted.getId())).isEmpty();
    }

    @Test
    void findByUserIdOrderByIdAscReturnsOnlyThatUsersFavouritesInInsertionOrder() {
        UserFavouriteArtist first = entityManager.persistFlushFind(new UserFavouriteArtist(10L, 100L));
        UserFavouriteArtist second = entityManager.persistFlushFind(new UserFavouriteArtist(10L, 101L));
        entityManager.persistAndFlush(new UserFavouriteArtist(20L, 200L));

        List<UserFavouriteArtist> favourites = userFavouriteArtistRepository.findByUserIdOrderByIdAsc(10L);

        assertThat(favourites).extracting(UserFavouriteArtist::getId)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    void findByUserIdOrderByIdAscReturnsEmptyListWhenUserHasNoFavourites() {
        List<UserFavouriteArtist> favourites = userFavouriteArtistRepository.findByUserIdOrderByIdAsc(999L);

        assertThat(favourites).isEmpty();
    }

    @Test
    void findByUserIdAndArtistIdReturnsMatchingFavourite() {
        entityManager.persistAndFlush(new UserFavouriteArtist(30L, 300L));

        Optional<UserFavouriteArtist> found = userFavouriteArtistRepository.findByUserIdAndArtistId(30L, 300L);

        assertThat(found).isPresent();
    }

    @Test
    void findByUserIdAndArtistIdReturnsEmptyWhenNoMatch() {
        entityManager.persistAndFlush(new UserFavouriteArtist(30L, 300L));

        Optional<UserFavouriteArtist> found = userFavouriteArtistRepository.findByUserIdAndArtistId(30L, 999L);

        assertThat(found).isEmpty();
    }

    @Test
    void existsByUserIdAndArtistIdReturnsTrueWhenFavouriteExists() {
        entityManager.persistAndFlush(new UserFavouriteArtist(40L, 400L));

        boolean exists = userFavouriteArtistRepository.existsByUserIdAndArtistId(40L, 400L);

        assertThat(exists).isTrue();
    }

    @Test
    void existsByUserIdAndArtistIdReturnsFalseWhenFavouriteDoesNotExist() {
        boolean exists = userFavouriteArtistRepository.existsByUserIdAndArtistId(40L, 999L);

        assertThat(exists).isFalse();
    }

    @Test
    void deleteByUserIdAndArtistIdRemovesOnlyMatchingFavourite() {
        entityManager.persistAndFlush(new UserFavouriteArtist(50L, 500L));
        entityManager.persistAndFlush(new UserFavouriteArtist(50L, 501L));

        userFavouriteArtistRepository.deleteByUserIdAndArtistId(50L, 500L);

        List<UserFavouriteArtist> remaining = userFavouriteArtistRepository.findByUserIdOrderByIdAsc(50L);
        assertThat(remaining).extracting(UserFavouriteArtist::getArtistId).containsExactly(501L);
    }
}
