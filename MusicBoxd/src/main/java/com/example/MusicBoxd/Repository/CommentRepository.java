package com.example.MusicBoxd.Repository;

import com.example.MusicBoxd.Model.Comment;
import org.springframework.data.repository.CrudRepository;

public interface CommentRepository extends CrudRepository<Comment,Long> {
}
