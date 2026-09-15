package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.ListItem;
import org.springframework.data.repository.CrudRepository;

public interface ListItemRepository extends CrudRepository<ListItem,Long> {
}
