package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long> {

    Optional<User> findByOktaUserId(String oktaUserId);

    List<User> findByUsernameContainingIgnoreCase(String username);
}