package com.i_route.backend.board.repository;

import com.i_route.backend.board.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    boolean existsByCommentIdAndUserId(Long commentId, Long userId);

    long countByCommentId(Long commentId);

    void deleteByCommentIdAndUserId(Long commentId, Long userId);

    @Modifying
    @Query(value = """
        DELETE FROM comment_like
        WHERE user_id = :userId
    """, nativeQuery = true)
    void deleteByUserIdNative(@Param("userId") Long userId);

    @Modifying
    @Query(value = """
        DELETE cl
        FROM comment_like cl
        JOIN comment c ON cl.comment_id = c.id
        JOIN post p ON c.post_id = p.id
        WHERE p.user_id = :userId
    """, nativeQuery = true)
    void deleteByPostOwnerId(@Param("userId") Long userId);
}