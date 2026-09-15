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
}
