package com.i_route.backend.board.repository;

import com.i_route.backend.board.entity.PostBookmark;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostBookmarkRepository extends JpaRepository<PostBookmark, Long> {

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    void deleteByPostIdAndUserId(Long postId, Long userId);

    void deleteByPost_User_Id(Long userId);

    void deleteByUser_Id(Long userId);
}