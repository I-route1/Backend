package com.i_route.backend.board.repository;

import com.i_route.backend.board.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostIdOrderByCreatedAtDesc(Long postId);

    long countByPostId(Long postId);

    void deleteByUser_Id(Long userId);

    void deleteByPost_User_Id(Long userId);
}