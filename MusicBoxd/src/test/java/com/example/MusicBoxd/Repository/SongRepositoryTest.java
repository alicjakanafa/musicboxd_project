package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.Song;
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
class SongRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SongRepository songRepository;

    @Test
    void savesSongLinkedToPersistedAlbum() {
        Album album = entityManager.persistFlushFind(new Album());
        Song song = new Song("ext-1", album.getId(), "Idioteque", 8, "http://songs.example/idioteque", "http://art.example/idioteque.png");

        Song saved = songRepository.save(song);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAlbumId()).isEqualTo(album.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findByIdReturnsAllPersistedFields() {
        Album album = entityManager.persistFlushFind(new Album());
        Song song = new Song("ext-2", album.getId(), "Everything In Its Right Place", 1, "http://songs.example/eiirp", "http://art.example/eiirp.png");
        Song persisted = entityManager.persistFlushFind(song);

        Optional<Song> found = songRepository.findById(persisted.getId());

        assertThat(found).isPresent();
        Song result = found.get();
        assertThat(result.getExternalId()).isEqualTo("ext-2");
        assertThat(result.getAlbumId()).isEqualTo(album.getId());
        assertThat(result.getTitle()).isEqualTo("Everything In Its Right Place");
        assertThat(result.getTrackNumber()).isEqualTo(1);
        assertThat(result.getSongUrl()).isEqualTo("http://songs.example/eiirp");
        assertThat(result.getSongImageUrl()).isEqualTo("http://art.example/eiirp.png");
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void findAllReturnsAllSongsRegardlessOfAlbum() {
        Album album = entityManager.persistFlushFind(new Album());
        entityManager.persistAndFlush(new Song("ext-3", album.getId(), "Song A", 1, null, null));
        entityManager.persistAndFlush(new Song("ext-4", album.getId(), "Song B", 2, null, null));

        List<Song> songs = (List<Song>) songRepository.findAll();

        assertThat(songs).hasSize(2)
                .extracting(Song::getTitle)
                .containsExactlyInAnyOrder("Song A", "Song B");
    }

    @Test
    void deleteByIdRemovesSong() {
        Album album = entityManager.persistFlushFind(new Album());
        Song persisted = entityManager.persistFlushFind(new Song("ext-5", album.getId(), "To Delete", 1, null, null));

        songRepository.deleteById(persisted.getId());

        assertThat(songRepository.findById(persisted.getId())).isEmpty();
    }

    @Test
    void findByAlbumIdReturnsOnlyThatAlbumsSongs() {
        Album albumOne = entityManager.persistFlushFind(new Album());
        Album albumTwo = entityManager.persistFlushFind(new Album());
        entityManager.persistAndFlush(new Song("ext-6", albumOne.getId(), "Song A", 1, null, null));
        entityManager.persistAndFlush(new Song("ext-7", albumOne.getId(), "Song B", 2, null, null));
        entityManager.persistAndFlush(new Song("ext-8", albumTwo.getId(), "Other Album Song", 1, null, null));

        List<Song> songs = songRepository.findByAlbumId(albumOne.getId());

        assertThat(songs).hasSize(2)
                .extracting(Song::getTitle)
                .containsExactlyInAnyOrder("Song A", "Song B");
    }

    @Test
    void findByAlbumIdReturnsEmptyListWhenAlbumHasNoSongs() {
        List<Song> songs = songRepository.findByAlbumId(999L);

        assertThat(songs).isEmpty();
    }

    @Test
    void findByExternalIdReturnsMatchingSong() {
        Album album = entityManager.persistFlushFind(new Album());
        entityManager.persistAndFlush(new Song("unique-song-ext-id", album.getId(), "Findable Song", 1, null, null));

        Optional<Song> found = songRepository.findByExternalId("unique-song-ext-id");

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Findable Song");
    }

    @Test
    void findByExternalIdReturnsEmptyWhenNoSongMatches() {
        Optional<Song> found = songRepository.findByExternalId("does-not-exist");

        assertThat(found).isEmpty();
    }
}
