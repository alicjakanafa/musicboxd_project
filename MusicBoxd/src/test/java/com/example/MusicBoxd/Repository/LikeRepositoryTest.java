package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Like;
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
class LikeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LikeRepository likeRepository;

    @Test
    void savesLikeAndPopulatesGeneratedIdAndCreatedAt() {
        Like like = new Like(1L, 2L, null);

        Like saved = likeRepository.save(like);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findByIdReturnsAllPersistedFields() {
        Like like = new Like(3L, 4L, null);
        Like persisted = entityManager.persistFlushFind(like);

        Optional<Like> found = likeRepository.findById(persisted.getId());

        assertThat(found).isPresent();
        Like result = found.get();
        assertThat(result.getUserId()).isEqualTo(3L);
        assertThat(result.getReviewId()).isEqualTo(4L);
        assertThat(result.getCommentId()).isNull();
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void findAllReturnsAllPersistedLikes() {
        entityManager.persistAndFlush(new Like(1L, 2L, null));
        entityManager.persistAndFlush(new Like(1L, null, 9L));

        List<Like> likes = (List<Like>) likeRepository.findAll();

        assertThat(likes).hasSize(2);
    }

    @Test
    void deleteByIdRemovesLike() {
        Like persisted = entityManager.persistFlushFind(new Like(5L, 6L, null));

        likeRepository.deleteById(persisted.getId());

        assertThat(likeRepository.findById(persisted.getId())).isEmpty();
    }

    @Test
    void supportsLikeOnCommentWithoutReview() {
        Like like = new Like(7L, null, 8L);

        Like saved = entityManager.persistFlushFind(like);

        assertThat(saved.getReviewId()).isNull();
        assertThat(saved.getCommentId()).isEqualTo(8L);
    }

    @Test
    void findByUserIdAndReviewIdReturnsMatchingLike() {
        entityManager.persistAndFlush(new Like(10L, 20L, null));

        Optional<Like> found = likeRepository.findByUserIdAndReviewId(10L, 20L);

        assertThat(found).isPresent();
    }

    @Test
    void findByUserIdAndReviewIdReturnsEmptyWhenNoMatch() {
        Optional<Like> found = likeRepository.findByUserIdAndReviewId(10L, 999L);

        assertThat(found).isEmpty();
    }

    @Test
    void countByReviewIdCountsLikesForThatReviewOnly() {
        entityManager.persistAndFlush(new Like(1L, 30L, null));
        entityManager.persistAndFlush(new Like(2L, 30L, null));
        entityManager.persistAndFlush(new Like(3L, 31L, null));

        long count = likeRepository.countByReviewId(30L);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void deleteByUserIdAndReviewIdRemovesOnlyMatchingLike() {
        entityManager.persistAndFlush(new Like(40L, 50L, null));
        entityManager.persistAndFlush(new Like(41L, 50L, null));

        likeRepository.deleteByUserIdAndReviewId(40L, 50L);

        assertThat(likeRepository.findByUserIdAndReviewId(40L, 50L)).isEmpty();
        assertThat(likeRepository.findByUserIdAndReviewId(41L, 50L)).isPresent();
    }
}
