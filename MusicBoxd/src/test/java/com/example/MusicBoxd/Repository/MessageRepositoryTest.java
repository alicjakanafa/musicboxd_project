package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Message;
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
class MessageRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MessageRepository messageRepository;

    @Test
    void savesMessageAndPopulatesGeneratedIdAndCreatedAt() {
        Message message = new Message(1L, 2L, "Hey, check this song out", null, null, null, null);

        Message saved = messageRepository.save(message);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findByIdReturnsAllPersistedFields() {
        Message message = new Message(3L, 4L, "Listen to this", "Song Title", "Song Artist",
                "http://img.example/cover.png", "http://preview.example/clip.mp3");
        Message persisted = entityManager.persistFlushFind(message);

        Optional<Message> found = messageRepository.findById(persisted.getId());

        assertThat(found).isPresent();
        Message result = found.get();
        assertThat(result.getSenderId()).isEqualTo(3L);
        assertThat(result.getReceiverId()).isEqualTo(4L);
        assertThat(result.getContent()).isEqualTo("Listen to this");
        assertThat(result.getSongTitle()).isEqualTo("Song Title");
        assertThat(result.getSongArtist()).isEqualTo("Song Artist");
        assertThat(result.getSongImageUrl()).isEqualTo("http://img.example/cover.png");
        assertThat(result.getSongPreviewUrl()).isEqualTo("http://preview.example/clip.mp3");
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void findAllReturnsAllPersistedMessages() {
        entityManager.persistAndFlush(new Message(1L, 2L, "First message", null, null, null, null));
        entityManager.persistAndFlush(new Message(2L, 1L, "Second message", null, null, null, null));

        List<Message> messages = (List<Message>) messageRepository.findAll();

        assertThat(messages).hasSize(2)
                .extracting(Message::getContent)
                .containsExactlyInAnyOrder("First message", "Second message");
    }

    @Test
    void deleteByIdRemovesMessage() {
        Message persisted = entityManager.persistFlushFind(
                new Message(1L, 2L, "To delete", null, null, null, null));

        messageRepository.deleteById(persisted.getId());

        assertThat(messageRepository.findById(persisted.getId())).isEmpty();
    }

    @Test
    void newMessageDefaultsToUnread() {
        Message message = new Message(1L, 2L, "Unread by default", null, null, null, null);

        Message saved = entityManager.persistFlushFind(message);

        assertThat(saved.isRead()).isFalse();
    }
}
