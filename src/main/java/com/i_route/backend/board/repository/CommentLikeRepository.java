package com.i_route.backend.board.repository;

import com.i_route.backend.board.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    boolean existsByCommentIdAndUserId(Long commentId, Long userId);

    long countByCommentId(Long commentId);

    void deleteByCommentIdAndUserId(Long commentId, Long userId);
}