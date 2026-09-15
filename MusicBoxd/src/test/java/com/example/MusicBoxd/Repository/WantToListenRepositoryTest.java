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
}
