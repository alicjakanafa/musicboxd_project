package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.WantToListen;
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
class WantToListenRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private WantToListenRepository wantToListenRepository;

    @Test
    void savesWantToListenAndPopulatesGeneratedIdAndCreatedAt() {
        WantToListen wantToListen = new WantToListen(1L, 2L, null);

        WantToListen saved = wantToListenRepository.save(wantToListen);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findByIdReturnsAllPersistedFields() {
        WantToListen wantToListen = new WantToListen(3L, null, 4L);
        WantToListen persisted = entityManager.persistFlushFind(wantToListen);

        Optional<WantToListen> found = wantToListenRepository.findById(persisted.getId().longValue());

        assertThat(found).isPresent();
        WantToListen result = found.get();
        assertThat(result.getUserId()).isEqualTo(3L);
        assertThat(result.getSongId()).isNull();
        assertThat(result.getAlbumId()).isEqualTo(4L);
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void findAllReturnsAllPersistedWantToListenEntries() {
        entityManager.persistAndFlush(new WantToListen(1L, 2L, null));
        entityManager.persistAndFlush(new WantToListen(1L, null, 5L));

        List<WantToListen> entries = (List<WantToListen>) wantToListenRepository.findAll();

        assertThat(entries).hasSize(2);
    }

    @Test
    void deleteByIdRemovesWantToListenEntry() {
        WantToListen persisted = entityManager.persistFlushFind(new WantToListen(1L, 2L, null));

        wantToListenRepository.deleteById(persisted.getId().longValue());

        assertThat(wantToListenRepository.findById(persisted.getId().longValue())).isEmpty();
    }

    @Test
    void supportsWantToListenEntryForSongWithoutAlbum() {
        WantToListen wantToListen = new WantToListen(6L, 7L, null);

        WantToListen saved = entityManager.persistFlushFind(wantToListen);

        assertThat(saved.getSongId()).isEqualTo(7L);
        assertThat(saved.getAlbumId()).isNull();
    }

    @Test
    void findAllByUserIdOrderByCreatedAtDescReturnsTheSingleEntryForThatUser() {
        WantToListen persisted = entityManager.persistFlushFind(new WantToListen(8L, 9L, null));
        entityManager.persistAndFlush(new WantToListen(99L, 11L, null));

        Optional<WantToListen> found = wantToListenRepository.findAllByUserIdOrderByCreatedAtDesc(8L);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(persisted.getId());
    }

    @Test
    void findAllByUserIdOrderByCreatedAtDescReturnsEmptyWhenUserHasNoEntries() {
        Optional<WantToListen> found = wantToListenRepository.findAllByUserIdOrderByCreatedAtDesc(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void findAllByUserIdOrderByCreatedAtDescThrowsWhenUserHasMoreThanOneEntry() {
        // NOTE: despite its "findAllBy" name and "OrderByCreatedAtDesc" clause (which
        // suggest it should return every want-to-listen entry for a user, most recent
        // first), this method is declared to return a single Optional<WantToListen>
        // with no LIMIT applied. Spring Data therefore executes it as a single-result
        // query, which blows up with NonUniqueResultException as soon as a user has
        // more than one entry. This method is not currently called from production
        // code (the WANT_TO_LISTEN table itself was dropped in
        // V23__delete_want_to_listen_table.sql), so this test documents the latent
        // defect rather than exercising a real code path.
        entityManager.persistFlushFind(new WantToListen(8L, 9L, null));
        entityManager.persistFlushFind(new WantToListen(8L, 10L, null));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> wantToListenRepository.findAllByUserIdOrderByCreatedAtDesc(8L))
                .isInstanceOf(org.springframework.dao.IncorrectResultSizeDataAccessException.class);
    }
}
