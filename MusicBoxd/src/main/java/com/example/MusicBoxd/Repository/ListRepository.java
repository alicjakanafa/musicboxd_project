package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.List;
import com.example.MusicBoxd.Model.ListType;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ListRepository extends CrudRepository<List,Long> {

    java.util.List<List> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<List> findByUserIdAndListType(
            Long userId,
            ListType listType
    );

}
