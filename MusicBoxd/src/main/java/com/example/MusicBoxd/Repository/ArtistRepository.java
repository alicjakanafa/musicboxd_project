package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Artist;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ArtistRepository extends CrudRepository<Artist, Long> {

    Optional<Artist> findByNameIgnoreCase(String name);
}
