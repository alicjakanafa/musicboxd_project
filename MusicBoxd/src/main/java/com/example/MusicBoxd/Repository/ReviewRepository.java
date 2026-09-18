package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Review;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ReviewRepository
        extends CrudRepository<Review, Long> {


    List<Review> findByUserId(Long userId);


    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Review> findByAlbumIdOrderByCreatedAtDesc(Long albumId);
}
