package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Tag;
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
class TagRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TagRepository tagRepository;

    @Test
    void savesTagAndGeneratesId() {
        Tag tag = new Tag("Shoegaze");

        Tag saved = tagRepository.save(tag);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Shoegaze");
    }

    @Test
    void findByIdReturnsPersistedTag() {
        Tag persisted = entityManager.persistFlushFind(new Tag("Jazz Fusion"));

        Optional<Tag> found = tagRepository.findById(persisted.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Jazz Fusion");
    }

    @Test
    void findAllReturnsAllPersistedTags() {
        entityManager.persistAndFlush(new Tag("Rock"));
        entityManager.persistAndFlush(new Tag("Pop"));

        List<Tag> tags = (List<Tag>) tagRepository.findAll();

        assertThat(tags).hasSize(2)
                .extracting(Tag::getName)
                .containsExactlyInAnyOrder("Rock", "Pop");
    }

    @Test
    void deleteByIdRemovesTag() {
        Tag persisted = entityManager.persistFlushFind(new Tag("To Delete"));

        tagRepository.deleteById(persisted.getId());

        assertThat(tagRepository.findById(persisted.getId())).isEmpty();
    }
}
