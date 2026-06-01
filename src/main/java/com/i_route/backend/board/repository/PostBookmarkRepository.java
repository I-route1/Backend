package com.i_route.backend.board.repository;

import com.i_route.backend.board.entity.PostBookmark;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostBookmarkRepository extends JpaRepository<PostBookmark, Long> {

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);

    void deleteByPostIdAndUserId(Long postId, Long userId);
}