package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Album;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface AlbumRepository extends CrudRepository<Album, Long> {

    List<Album> findByArtistId(Long artistId);

    Optional<Album> findByExternalId(String externalId);
}