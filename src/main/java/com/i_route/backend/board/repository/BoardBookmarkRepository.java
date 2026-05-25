package com.i_route.backend.domain.board.repository;

import com.i_route.backend.domain.board.entity.BoardBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardBookmarkRepository extends JpaRepository<BoardBookmark, Long> {

    Optional<BoardBookmark> findByUserIdAndBoardId(Long userId, Long boardId);
    List<BoardBookmark> findByUserId(Long userId);
    boolean existsByUserIdAndBoardId(Long userId, Long boardId);
}
