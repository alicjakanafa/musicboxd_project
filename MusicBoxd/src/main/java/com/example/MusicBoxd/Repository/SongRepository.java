package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Song;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface SongRepository extends CrudRepository<Song, Long> {

    List<Song> findByAlbumId(Long albumId);

    Optional<Song> findByExternalId(String externalId);
}