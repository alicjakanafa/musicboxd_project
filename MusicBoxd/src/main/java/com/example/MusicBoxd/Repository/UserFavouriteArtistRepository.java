package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.UserFavouriteArtist;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface UserFavouriteArtistRepository
        extends CrudRepository<UserFavouriteArtist, Long> {

    List<UserFavouriteArtist> findByUserIdOrderByIdAsc(
            Long userId
    );

    Optional<UserFavouriteArtist> findByUserIdAndArtistId(
            Long userId,
            Long artistId
    );

    boolean existsByUserIdAndArtistId(
            Long userId,
            Long artistId
    );

    void deleteByUserIdAndArtistId(
            Long userId,
            Long artistId
    );
}