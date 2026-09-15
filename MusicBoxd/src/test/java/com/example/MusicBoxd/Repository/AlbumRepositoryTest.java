package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Album;
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
class AlbumRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AlbumRepository albumRepository;

    @Test
    void savesAlbumLinkedToPersistedArtist() {
        Artist artist = entityManager.persistFlushFind(new Artist());
        Album album = new Album("ext-1", artist.getId(), "Kid A", (short) 2000, "http://art.example/kid-a.png");

        Album saved = albumRepository.save(album);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getArtistId()).isEqualTo(artist.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findByIdReturnsAllPersistedFields() {
        Artist artist = entityManager.persistFlushFind(new Artist());
        Album album = new Album("ext-2", artist.getId(), "Amnesiac", (short) 2001, "http://art.example/amnesiac.png");
        Album persisted = entityManager.persistFlushFind(album);

        Optional<Album> found = albumRepository.findById(persisted.getId());

        assertThat(found).isPresent();
        Album result = found.get();
        assertThat(result.getExternalId()).isEqualTo("ext-2");
        assertThat(result.getArtistId()).isEqualTo(artist.getId());
        assertThat(result.getTitle()).isEqualTo("Amnesiac");
        assertThat(result.getReleaseYear()).isEqualTo((short) 2001);
        assertThat(result.getArtworkUrl()).isEqualTo("http://art.example/amnesiac.png");
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void findAllReturnsAllAlbumsRegardlessOfArtist() {
        Artist artist = entityManager.persistFlushFind(new Artist());
        entityManager.persistAndFlush(new Album("ext-3", artist.getId(), "Album A", (short) 2010, null));
        entityManager.persistAndFlush(new Album("ext-4", artist.getId(), "Album B", (short) 2012, null));

        List<Album> albums = (List<Album>) albumRepository.findAll();

        assertThat(albums).hasSize(2)
                .extracting(Album::getTitle)
                .containsExactlyInAnyOrder("Album A", "Album B");
    }

    @Test
    void deleteByIdRemovesAlbum() {
        Artist artist = entityManager.persistFlushFind(new Artist());
        Album persisted = entityManager.persistFlushFind(new Album("ext-5", artist.getId(), "To Delete", (short) 2020, null));

        albumRepository.deleteById(persisted.getId());

        assertThat(albumRepository.findById(persisted.getId())).isEmpty();
    }
}
