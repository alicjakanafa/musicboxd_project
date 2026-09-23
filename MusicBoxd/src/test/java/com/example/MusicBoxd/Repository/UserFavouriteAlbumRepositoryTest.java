package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.UserFavouriteAlbum;
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
class UserFavouriteAlbumRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserFavouriteAlbumRepository userFavouriteAlbumRepository;

    @Test
    void savesUserFavouriteAlbumAndGeneratesId() {
        UserFavouriteAlbum favourite = new UserFavouriteAlbum(1L, 2L, 1);

        UserFavouriteAlbum saved = userFavouriteAlbumRepository.save(favourite);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getAlbumId()).isEqualTo(2L);
        assertThat(saved.getPosition()).isEqualTo(1);
    }

    @Test
    void findByIdReturnsAllPersistedFields() {
        UserFavouriteAlbum favourite = new UserFavouriteAlbum(3L, 4L, 2);
        UserFavouriteAlbum persisted = entityManager.persistFlushFind(favourite);

        Optional<UserFavouriteAlbum> found = userFavouriteAlbumRepository.findById(persisted.getId());

        assertThat(found).isPresent();
        UserFavouriteAlbum result = found.get();
        assertThat(result.getUserId()).isEqualTo(3L);
        assertThat(result.getAlbumId()).isEqualTo(4L);
        assertThat(result.getPosition()).isEqualTo(2);
    }

    @Test
    void deleteByIdRemovesUserFavouriteAlbum() {
        UserFavouriteAlbum persisted = entityManager.persistFlushFind(new UserFavouriteAlbum(5L, 6L, 1));

        userFavouriteAlbumRepository.deleteById(persisted.getId());

        assertThat(userFavouriteAlbumRepository.findById(persisted.getId())).isEmpty();
    }

    @Test
    void findByUserIdOrderByPositionAscReturnsOnlyThatUsersFavouritesInPositionOrder() {
        entityManager.persistAndFlush(new UserFavouriteAlbum(10L, 100L, 3));
        entityManager.persistAndFlush(new UserFavouriteAlbum(10L, 101L, 1));
        entityManager.persistAndFlush(new UserFavouriteAlbum(10L, 102L, 2));
        entityManager.persistAndFlush(new UserFavouriteAlbum(20L, 200L, 1));

        List<UserFavouriteAlbum> favourites = userFavouriteAlbumRepository.findByUserIdOrderByPositionAsc(10L);

        assertThat(favourites).extracting(UserFavouriteAlbum::getAlbumId)
                .containsExactly(101L, 102L, 100L);
    }

    @Test
    void findByUserIdOrderByPositionAscReturnsEmptyListWhenUserHasNoFavourites() {
        List<UserFavouriteAlbum> favourites = userFavouriteAlbumRepository.findByUserIdOrderByPositionAsc(999L);

        assertThat(favourites).isEmpty();
    }

    @Test
    void findByUserIdAndAlbumIdReturnsMatchingFavourite() {
        entityManager.persistAndFlush(new UserFavouriteAlbum(30L, 300L, 1));

        Optional<UserFavouriteAlbum> found = userFavouriteAlbumRepository.findByUserIdAndAlbumId(30L, 300L);

        assertThat(found).isPresent();
        assertThat(found.get().getPosition()).isEqualTo(1);
    }

    @Test
    void findByUserIdAndAlbumIdReturnsEmptyWhenNoMatch() {
        entityManager.persistAndFlush(new UserFavouriteAlbum(30L, 300L, 1));

        Optional<UserFavouriteAlbum> found = userFavouriteAlbumRepository.findByUserIdAndAlbumId(30L, 999L);

        assertThat(found).isEmpty();
    }
}
