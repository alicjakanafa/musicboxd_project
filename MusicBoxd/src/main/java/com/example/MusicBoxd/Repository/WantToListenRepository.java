package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.WantToListen;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface WantToListenRepository extends CrudRepository<WantToListen,Long> {
    Optional<WantToListen> findAllByUserIdOrderByCreatedAtDesc(Long id);
}
