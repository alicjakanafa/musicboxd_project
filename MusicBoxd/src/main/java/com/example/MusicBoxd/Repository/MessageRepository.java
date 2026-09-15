package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Message;
import org.springframework.data.repository.CrudRepository;

public interface MessageRepository extends CrudRepository<Message,Long> {
}
