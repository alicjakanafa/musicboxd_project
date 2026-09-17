package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.UserFavouriteAlbum;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface UserFavouriteAlbumRepository
        extends CrudRepository<UserFavouriteAlbum, Long> {

    List<UserFavouriteAlbum>
    findByUserIdOrderByPositionAsc(Long userId);

    Optional<UserFavouriteAlbum>
    findByUserIdAndAlbumId(Long userId, Long albumId);
}
