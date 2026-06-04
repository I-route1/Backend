package com.i_route.backend.board.repository;

import com.i_route.backend.board.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByBoardId(Long boardId);
    List<Post> findByTitleContainingOrContentContaining(String title, String content);

    void deleteByUser_Id(Long userId);
}
