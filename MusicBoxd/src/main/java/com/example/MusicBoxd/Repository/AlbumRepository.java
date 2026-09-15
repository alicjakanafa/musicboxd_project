package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Album;
import org.springframework.data.repository.CrudRepository;

public interface AlbumRepository extends CrudRepository<Album,Long> {
}
