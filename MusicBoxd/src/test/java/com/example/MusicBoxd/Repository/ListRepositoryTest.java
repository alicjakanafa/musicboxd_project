package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.ListType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("datajpatest")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ListRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ListRepository listRepository;

    @Test
    void savesListAndPopulatesGeneratedIdAndCreatedAt() {
        com.example.MusicBoxd.Model.List list = new com.example.MusicBoxd.Model.List(1L, "Favourites", "My favourite albums", ListType.CUSTOM);

        com.example.MusicBoxd.Model.List saved = listRepository.save(list);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findByIdReturnsAllPersistedFields() {
        com.example.MusicBoxd.Model.List list = new com.example.MusicBoxd.Model.List(2L, "Road Trip", "Songs for driving", ListType.CUSTOM);
        com.example.MusicBoxd.Model.List persisted = entityManager.persistFlushFind(list);

        Optional<com.example.MusicBoxd.Model.List> found = listRepository.findById(persisted.getId());

        assertThat(found).isPresent();
        com.example.MusicBoxd.Model.List result = found.get();
        assertThat(result.getUserId()).isEqualTo(2L);
        assertThat(result.getTitle()).isEqualTo("Road Trip");
        assertThat(result.getDescription()).isEqualTo("Songs for driving");
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void findAllReturnsAllPersistedLists() {
        entityManager.persistAndFlush(new com.example.MusicBoxd.Model.List(1L, "List One", "First list", ListType.CUSTOM));
        entityManager.persistAndFlush(new com.example.MusicBoxd.Model.List(1L, "List Two", "Second list", ListType.CUSTOM));

        java.util.List<com.example.MusicBoxd.Model.List> lists = (java.util.List<com.example.MusicBoxd.Model.List>) listRepository.findAll();

        assertThat(lists).hasSize(2)
                .extracting(com.example.MusicBoxd.Model.List::getTitle)
                .containsExactlyInAnyOrder("List One", "List Two");
    }

    @Test
    void deleteByIdRemovesList() {
        com.example.MusicBoxd.Model.List persisted = entityManager.persistFlushFind(
                new com.example.MusicBoxd.Model.List(3L, "To Delete", "Temp list", ListType.CUSTOM));

        listRepository.deleteById(persisted.getId());

        assertThat(listRepository.findById(persisted.getId())).isEmpty();
    }

    @Test
    void savesListWithNullDescription() {
        com.example.MusicBoxd.Model.List list = new com.example.MusicBoxd.Model.List(4L, "No Description", null, ListType.CUSTOM);

        com.example.MusicBoxd.Model.List saved = entityManager.persistFlushFind(list);

        assertThat(saved.getDescription()).isNull();
        assertThat(saved.getTitle()).isEqualTo("No Description");
    }

    @Test
    void findByUserIdOrderByCreatedAtDescReturnsOnlyThatUsersListsNewestFirst() throws InterruptedException {
        com.example.MusicBoxd.Model.List older = entityManager.persistFlushFind(
                new com.example.MusicBoxd.Model.List(5L, "Older List", null, ListType.CUSTOM));
        Thread.sleep(5);
        com.example.MusicBoxd.Model.List newer = entityManager.persistFlushFind(
                new com.example.MusicBoxd.Model.List(5L, "Newer List", null, ListType.CUSTOM));
        entityManager.persistAndFlush(new com.example.MusicBoxd.Model.List(6L, "Other User List", null, ListType.CUSTOM));

        java.util.List<com.example.MusicBoxd.Model.List> lists = listRepository.findByUserIdOrderByCreatedAtDesc(5L);

        assertThat(lists).extracting(com.example.MusicBoxd.Model.List::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void findByUserIdOrderByCreatedAtDescReturnsEmptyListWhenUserHasNoLists() {
        java.util.List<com.example.MusicBoxd.Model.List> lists = listRepository.findByUserIdOrderByCreatedAtDesc(999L);

        assertThat(lists).isEmpty();
    }

    @Test
    void findByUserIdAndListTypeReturnsMatchingList() {
        entityManager.persistAndFlush(new com.example.MusicBoxd.Model.List(7L, "Favourites", null, ListType.FAVOURITES));
        entityManager.persistAndFlush(new com.example.MusicBoxd.Model.List(7L, "Custom List", null, ListType.CUSTOM));

        Optional<com.example.MusicBoxd.Model.List> found = listRepository.findByUserIdAndListType(7L, ListType.FAVOURITES);

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Favourites");
    }

    @Test
    void findByUserIdAndListTypeReturnsEmptyWhenNoMatchingListType() {
        entityManager.persistAndFlush(new com.example.MusicBoxd.Model.List(8L, "Custom List", null, ListType.CUSTOM));

        Optional<com.example.MusicBoxd.Model.List> found = listRepository.findByUserIdAndListType(8L, ListType.WANT_TO_LISTEN);

        assertThat(found).isEmpty();
    }
}
