// BoardNoticeNotificationRepository.java
package com.i_route.backend.board.repository;

import com.i_route.backend.board.entity.BoardNoticeNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardNoticeNotificationRepository
        extends JpaRepository<BoardNoticeNotification, Long> {

    List<BoardNoticeNotification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    Optional<BoardNoticeNotification> findByIdAndRecipientId(Long id, Long recipientId);

    boolean existsByPostId(Long postId);
}