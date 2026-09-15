package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.Comment;
import com.example.MusicBoxd.Model.Review;
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
class CommentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CommentRepository commentRepository;

    private Review persistReview(User user, Album album) {
        return entityManager.persistFlushFind(
                new Review(user.getId(), album.getId(), null, "Header", "Content", new BigDecimal("4.0")));
    }

    @Test
    void savesCommentLinkedToUserAndReview() {
        User user = entityManager.persistFlushFind(new User("g1", "commenter", "commenter@example.com", null, null));
        Album album = entityManager.persistFlushFind(new Album());
        Review review = persistReview(user, album);

        Comment comment = new Comment(user.getId(), review.getId(), "Totally agree with this review!");

        Comment saved = commentRepository.save(comment);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(user.getId());
        assertThat(saved.getReviewId()).isEqualTo(review.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findByIdReturnsAllPersistedFields() {
        User user = entityManager.persistFlushFind(new User("g2", "commenter2", "commenter2@example.com", null, null));
        Album album = entityManager.persistFlushFind(new Album());
        Review review = persistReview(user, album);
        Comment persisted = entityManager.persistFlushFind(new Comment(user.getId(), review.getId(), "Nice writeup."));

        Optional<Comment> found = commentRepository.findById(persisted.getId());

        assertThat(found).isPresent();
        Comment result = found.get();
        assertThat(result.getUserId()).isEqualTo(user.getId());
        assertThat(result.getReviewId()).isEqualTo(review.getId());
        assertThat(result.getContent()).isEqualTo("Nice writeup.");
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void findAllReturnsAllPersistedComments() {
        User user = entityManager.persistFlushFind(new User("g3", "commenter3", "commenter3@example.com", null, null));
        Album album = entityManager.persistFlushFind(new Album());
        Review review = persistReview(user, album);
        entityManager.persistAndFlush(new Comment(user.getId(), review.getId(), "First comment"));
        entityManager.persistAndFlush(new Comment(user.getId(), review.getId(), "Second comment"));

        List<Comment> comments = (List<Comment>) commentRepository.findAll();

        assertThat(comments).hasSize(2)
                .extracting(Comment::getContent)
                .containsExactlyInAnyOrder("First comment", "Second comment");
    }

    @Test
    void deleteByIdRemovesComment() {
        User user = entityManager.persistFlushFind(new User("g4", "commenter4", "commenter4@example.com", null, null));
        Album album = entityManager.persistFlushFind(new Album());
        Review review = persistReview(user, album);
        Comment persisted = entityManager.persistFlushFind(new Comment(user.getId(), review.getId(), "To delete"));

        commentRepository.deleteById(persisted.getId());

        assertThat(commentRepository.findById(persisted.getId())).isEmpty();
    }
}
