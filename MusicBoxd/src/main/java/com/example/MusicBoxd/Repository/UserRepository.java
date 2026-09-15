package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User,Long> {
}
