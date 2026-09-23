package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Artist;
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
class ArtistRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ArtistRepository artistRepository;

    @Test
    void savesArtistAndGeneratesId() {
        Artist artist = new Artist("Radiohead");

        Artist saved = artistRepository.save(artist);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Radiohead");
    }

    @Test
    void findByIdReturnsPersistedArtist() {
        Artist persisted = entityManager.persistFlushFind(new Artist("Tame Impala"));

        Optional<Artist> found = artistRepository.findById(persisted.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Tame Impala");
    }

    @Test
    void findAllReturnsAllPersistedArtists() {
        entityManager.persistAndFlush(new Artist("Artist A"));
        entityManager.persistAndFlush(new Artist("Artist B"));

        List<Artist> artists = (List<Artist>) artistRepository.findAll();

        assertThat(artists).hasSize(2)
                .extracting(Artist::getName)
                .containsExactlyInAnyOrder("Artist A", "Artist B");
    }

    @Test
    void deleteByIdRemovesArtist() {
        Artist persisted = entityManager.persistFlushFind(new Artist("To Delete"));

        artistRepository.deleteById(persisted.getId());

        assertThat(artistRepository.findById(persisted.getId())).isEmpty();
    }

    @Test
    void findByNameIgnoreCaseMatchesRegardlessOfCase() {
        entityManager.persistAndFlush(new Artist("Radiohead"));

        Optional<Artist> found = artistRepository.findByNameIgnoreCase("radiohead");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Radiohead");
    }

    @Test
    void findByNameIgnoreCaseReturnsEmptyWhenNoArtistMatches() {
        Optional<Artist> found = artistRepository.findByNameIgnoreCase("Unknown Artist");

        assertThat(found).isEmpty();
    }
}
