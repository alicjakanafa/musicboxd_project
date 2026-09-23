package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.Review;
import com.example.MusicBoxd.Model.Song;
import com.example.MusicBoxd.Model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("datajpatest")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReviewRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ReviewRepository reviewRepository;

    @Test
    void savesReviewLinkedToUserAlbumAndSong() {
        User user = entityManager.persistFlushFind(new User("g1", "reviewer", "reviewer@example.com", null, null));
        Album album = entityManager.persistFlushFind(new Album());
        Song song = entityManager.persistFlushFind(new Song("ext-1", album.getId(), "Track", 1, null, null));

        Review review = new Review(user.getId(), album.getId(), song.getId(), "Great album", "Loved every track.", new BigDecimal("4.5"));

        Review saved = reviewRepository.save(review);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(user.getId());
        assertThat(saved.getAlbumId()).isEqualTo(album.getId());
        assertThat(saved.getSongId()).isEqualTo(song.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findByIdReturnsAllPersistedFieldsIncludingRating() {
        User user = entityManager.persistFlushFind(new User("g2", "reviewer2", "reviewer2@example.com", null, null));
        Album album = entityManager.persistFlushFind(new Album());
        Review review = new Review(user.getId(), album.getId(), null, "Solid record", "Consistent and well produced.", new BigDecimal("3.5"));
        Review persisted = entityManager.persistFlushFind(review);

        Optional<Review> found = reviewRepository.findById(persisted.getId());

        assertThat(found).isPresent();
        Review result = found.get();
        assertThat(result.getUserId()).isEqualTo(user.getId());
        assertThat(result.getAlbumId()).isEqualTo(album.getId());
        assertThat(result.getSongId()).isNull();
        assertThat(result.getHeader()).isEqualTo("Solid record");
        assertThat(result.getContent()).isEqualTo("Consistent and well produced.");
        assertThat(result.getRating()).isEqualByComparingTo(new BigDecimal("3.5"));
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void findAllReturnsAllPersistedReviews() {
        User user = entityManager.persistFlushFind(new User("g3", "reviewer3", "reviewer3@example.com", null, null));
        Album album = entityManager.persistFlushFind(new Album());
        entityManager.persistAndFlush(new Review(user.getId(), album.getId(), null, "Header A", "Content A", new BigDecimal("2.0")));
        entityManager.persistAndFlush(new Review(user.getId(), album.getId(), null, "Header B", "Content B", new BigDecimal("5.0")));

        List<Review> reviews = (List<Review>) reviewRepository.findAll();

        assertThat(reviews).hasSize(2)
                .extracting(Review::getHeader)
                .containsExactlyInAnyOrder("Header A", "Header B");
    }

    @Test
    void deleteByIdRemovesReview() {
        User user = entityManager.persistFlushFind(new User("g4", "reviewer4", "reviewer4@example.com", null, null));
        Album album = entityManager.persistFlushFind(new Album());
        Review persisted = entityManager.persistFlushFind(new Review(user.getId(), album.getId(), null, "To delete", "Content", new BigDecimal("1.0")));

        reviewRepository.deleteById(persisted.getId());

        assertThat(reviewRepository.findById(persisted.getId())).isEmpty();
    }

    @Test
    void findByUserIdReturnsOnlyThatUsersReviews() {
        User userOne = entityManager.persistFlushFind(new User("g5", "reviewer5", "reviewer5@example.com", null, null));
        User userTwo = entityManager.persistFlushFind(new User("g6", "reviewer6", "reviewer6@example.com", null, null));
        Album album = entityManager.persistFlushFind(new Album());
        entityManager.persistAndFlush(new Review(userOne.getId(), album.getId(), null, "Header A", "Content A", new BigDecimal("2.0")));
        entityManager.persistAndFlush(new Review(userTwo.getId(), album.getId(), null, "Header B", "Content B", new BigDecimal("3.0")));

        List<Review> reviews = reviewRepository.findByUserId(userOne.getId());

        assertThat(reviews).hasSize(1)
                .extracting(Review::getHeader)
                .containsExactly("Header A");
    }

    @Test
    void findByUserIdOrderByCreatedAtDescReturnsThatUsersReviewsNewestFirst() throws InterruptedException {
        User user = entityManager.persistFlushFind(new User("g7", "reviewer7", "reviewer7@example.com", null, null));
        Album album = entityManager.persistFlushFind(new Album());
        Review older = entityManager.persistFlushFind(new Review(user.getId(), album.getId(), null, "Older", "Content", new BigDecimal("2.0")));
        Thread.sleep(5);
        Review newer = entityManager.persistFlushFind(new Review(user.getId(), album.getId(), null, "Newer", "Content", new BigDecimal("4.0")));

        List<Review> reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        assertThat(reviews).extracting(Review::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void findByAlbumIdOrderByCreatedAtDescReturnsThatAlbumsReviewsNewestFirst() throws InterruptedException {
        User user = entityManager.persistFlushFind(new User("g8", "reviewer8", "reviewer8@example.com", null, null));
        Album albumOne = entityManager.persistFlushFind(new Album());
        Album albumTwo = entityManager.persistFlushFind(new Album());
        Review older = entityManager.persistFlushFind(new Review(user.getId(), albumOne.getId(), null, "Older", "Content", new BigDecimal("2.0")));
        Thread.sleep(5);
        Review newer = entityManager.persistFlushFind(new Review(user.getId(), albumOne.getId(), null, "Newer", "Content", new BigDecimal("4.0")));
        entityManager.persistAndFlush(new Review(user.getId(), albumTwo.getId(), null, "Other album", "Content", new BigDecimal("3.0")));

        List<Review> reviews = reviewRepository.findByAlbumIdOrderByCreatedAtDesc(albumOne.getId());

        assertThat(reviews).extracting(Review::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void findByUserIdAndAlbumIdReturnsMatchingReview() {
        User user = entityManager.persistFlushFind(new User("g9", "reviewer9", "reviewer9@example.com", null, null));
        Album album = entityManager.persistFlushFind(new Album());
        entityManager.persistAndFlush(new Review(user.getId(), album.getId(), null, "Header", "Content", new BigDecimal("3.0")));

        Optional<Review> found = reviewRepository.findByUserIdAndAlbumId(user.getId(), album.getId());

        assertThat(found).isPresent();
    }

    @Test
    void findByUserIdAndAlbumIdReturnsEmptyWhenNoMatch() {
        Optional<Review> found = reviewRepository.findByUserIdAndAlbumId(999L, 998L);

        assertThat(found).isEmpty();
    }

    @Test
    void findAllByOrderByCreatedAtDescReturnsAllReviewsNewestFirst() throws InterruptedException {
        User user = entityManager.persistFlushFind(new User("g10", "reviewer10", "reviewer10@example.com", null, null));
        Album album = entityManager.persistFlushFind(new Album());
        Review older = entityManager.persistFlushFind(new Review(user.getId(), album.getId(), null, "Older", "Content", new BigDecimal("2.0")));
        Thread.sleep(5);
        Review newer = entityManager.persistFlushFind(new Review(user.getId(), album.getId(), null, "Newer", "Content", new BigDecimal("4.0")));

        List<Review> reviews = reviewRepository.findAllByOrderByCreatedAtDesc();

        assertThat(reviews).extracting(Review::getId)
                .containsExactly(newer.getId(), older.getId());
    }
}
