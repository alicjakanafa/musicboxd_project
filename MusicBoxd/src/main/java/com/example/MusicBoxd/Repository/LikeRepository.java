package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Like;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface LikeRepository extends CrudRepository<Like, Long> {

    Optional<Like> findByUserIdAndReviewId(
            Long userId,
            Long reviewId
    );

    long countByReviewId(
            Long reviewId
    );

    void deleteByUserIdAndReviewId(
            Long userId,
            Long reviewId
    );
}