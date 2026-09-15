package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Song;
import org.springframework.data.repository.CrudRepository;

public interface SongRepository extends CrudRepository<Song,Long> {
}
