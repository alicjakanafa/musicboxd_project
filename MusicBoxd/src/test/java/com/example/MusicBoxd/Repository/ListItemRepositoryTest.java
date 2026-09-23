package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.ListItem;
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
class ListItemRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ListItemRepository listItemRepository;

    @Test
    void savesListItemAndPopulatesGeneratedIdAndCreatedAt() {
        ListItem item = new ListItem(1L, 2L, null, 1);

        ListItem saved = listItemRepository.save(item);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findByIdReturnsAllPersistedFields() {
        ListItem item = new ListItem(3L, 4L, null, 2);
        ListItem persisted = entityManager.persistFlushFind(item);

        Optional<ListItem> found = listItemRepository.findById(persisted.getId().longValue());

        assertThat(found).isPresent();
        ListItem result = found.get();
        assertThat(result.getListId()).isEqualTo(3L);
        assertThat(result.getAlbumId()).isEqualTo(4L);
        assertThat(result.getSongId()).isNull();
        assertThat(result.getPosition()).isEqualTo(2);
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void findAllReturnsAllPersistedListItems() {
        entityManager.persistAndFlush(new ListItem(1L, 2L, null, 1));
        entityManager.persistAndFlush(new ListItem(1L, null, 5L, 2));

        List<ListItem> items = (List<ListItem>) listItemRepository.findAll();

        assertThat(items).hasSize(2)
                .extracting(ListItem::getPosition)
                .containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void deleteByIdRemovesListItem() {
        ListItem persisted = entityManager.persistFlushFind(new ListItem(1L, 2L, null, 1));

        listItemRepository.deleteById(persisted.getId().longValue());

        assertThat(listItemRepository.findById(persisted.getId().longValue())).isEmpty();
    }

    @Test
    void preservesOrderingPositionAcrossItemsInSameList() {
        ListItem first = entityManager.persistFlushFind(new ListItem(10L, 20L, null, 0));
        ListItem second = entityManager.persistFlushFind(new ListItem(10L, 30L, null, 1));

        assertThat(first.getPosition()).isEqualTo(0);
        assertThat(second.getPosition()).isEqualTo(1);
        assertThat(first.getListId()).isEqualTo(second.getListId());
    }

    @Test
    void findByListIdOrderByPositionAscReturnsOnlyThatListsItemsInOrder() {
        entityManager.persistAndFlush(new ListItem(100L, 1L, null, 2));
        entityManager.persistAndFlush(new ListItem(100L, 2L, null, 0));
        entityManager.persistAndFlush(new ListItem(100L, 3L, null, 1));
        entityManager.persistAndFlush(new ListItem(200L, 4L, null, 0));

        List<ListItem> items = listItemRepository.findByListIdOrderByPositionAsc(100L);

        assertThat(items).extracting(ListItem::getAlbumId)
                .containsExactly(2L, 3L, 1L);
    }

    @Test
    void findByListIdOrderByPositionAscReturnsEmptyListWhenListHasNoItems() {
        List<ListItem> items = listItemRepository.findByListIdOrderByPositionAsc(999L);

        assertThat(items).isEmpty();
    }

    @Test
    void findByListIdAndAlbumIdReturnsMatchingItem() {
        entityManager.persistAndFlush(new ListItem(300L, 40L, null, 0));

        Optional<ListItem> found = listItemRepository.findByListIdAndAlbumId(300L, 40L);

        assertThat(found).isPresent();
    }

    @Test
    void findByListIdAndAlbumIdReturnsEmptyWhenNoMatch() {
        Optional<ListItem> found = listItemRepository.findByListIdAndAlbumId(300L, 999L);

        assertThat(found).isEmpty();
    }

    @Test
    void existsByListIdAndAlbumIdReturnsTrueWhenItemExists() {
        entityManager.persistAndFlush(new ListItem(400L, 50L, null, 0));

        assertThat(listItemRepository.existsByListIdAndAlbumId(400L, 50L)).isTrue();
    }

    @Test
    void existsByListIdAndAlbumIdReturnsFalseWhenItemDoesNotExist() {
        assertThat(listItemRepository.existsByListIdAndAlbumId(400L, 999L)).isFalse();
    }

    @Test
    void findMaxPositionReturnsHighestPositionForList() {
        entityManager.persistAndFlush(new ListItem(500L, 60L, null, 0));
        entityManager.persistAndFlush(new ListItem(500L, 61L, null, 3));
        entityManager.persistAndFlush(new ListItem(500L, 62L, null, 1));

        Integer maxPosition = listItemRepository.findMaxPosition(500L);

        assertThat(maxPosition).isEqualTo(3);
    }

    @Test
    void findMaxPositionReturnsZeroWhenListHasNoItems() {
        Integer maxPosition = listItemRepository.findMaxPosition(999L);

        assertThat(maxPosition).isEqualTo(0);
    }

    @Test
    void countByListIdCountsOnlyItemsInThatList() {
        entityManager.persistAndFlush(new ListItem(600L, 70L, null, 0));
        entityManager.persistAndFlush(new ListItem(600L, 71L, null, 1));
        entityManager.persistAndFlush(new ListItem(601L, 72L, null, 0));

        long count = listItemRepository.countByListId(600L);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void deleteByListIdRemovesAllItemsInThatListOnly() {
        entityManager.persistAndFlush(new ListItem(700L, 80L, null, 0));
        entityManager.persistAndFlush(new ListItem(700L, 81L, null, 1));
        entityManager.persistAndFlush(new ListItem(701L, 82L, null, 0));

        listItemRepository.deleteByListId(700L);

        assertThat(listItemRepository.findByListIdOrderByPositionAsc(700L)).isEmpty();
        assertThat(listItemRepository.findByListIdOrderByPositionAsc(701L)).hasSize(1);
    }
}
