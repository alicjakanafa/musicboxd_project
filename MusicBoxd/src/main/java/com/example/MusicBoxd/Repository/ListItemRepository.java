package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.ListItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ListItemRepository extends CrudRepository<ListItem, Long> {

    List<ListItem> findByListIdOrderByPositionAsc(Long listId);

    Optional<ListItem> findByListIdAndAlbumId(Long listId, Long albumId);

    boolean existsByListIdAndAlbumId(
            Long listId,
            Long albumId
    );

    @Query("select coalesce(max(i.position), 0) from ListItem i where i.listId = :listId")
    Integer findMaxPosition(@Param("listId") Long listId);

    long countByListId(Long listId);

    void deleteByListId(Long listId);

    void deleteByListIdAndAlbumId(Long listId, Long albumId);
}
