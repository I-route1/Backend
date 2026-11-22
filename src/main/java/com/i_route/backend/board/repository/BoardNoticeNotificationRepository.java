// BoardNoticeNotificationRepository.java
package com.i_route.backend.board.repository;

import com.i_route.backend.board.entity.BoardNoticeNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoardNoticeNotificationRepository
        extends JpaRepository<BoardNoticeNotification, Long> {

    List<BoardNoticeNotification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    Optional<BoardNoticeNotification> findByIdAndRecipientId(Long id, Long recipientId);

    Optional<BoardNoticeNotification> findByRecipientIdAndPostId(Long recipientId, Long postId);

    @Query("select n.recipientId from BoardNoticeNotification n where n.postId = :postId")
    List<Long> findRecipientIdsByPostId(@Param("postId") Long postId);
}