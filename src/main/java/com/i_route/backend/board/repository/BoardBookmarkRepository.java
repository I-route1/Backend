// BoardBookmarkRepository.java
package com.i_route.backend.board.repository;

import com.i_route.backend.board.entity.BoardBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BoardBookmarkRepository extends JpaRepository<BoardBookmark, Long> {

    @Query("select b.boardId from BoardBookmark b where b.userId = :userId")
    List<Long> findBoardIdsByUserId(
            @org.springframework.data.repository.query.Param("userId") Long userId);

    interface BookmarkCount {
        Long getBoardId();
        long getTotal();
    }

    @Query("select b.boardId as boardId, count(b) as total from BoardBookmark b group by b.boardId")
    List<BookmarkCount> countGroupedByBoard();

    boolean existsByBoardIdAndUserId(Long boardId, Long userId);

    void deleteByBoardIdAndUserId(Long boardId, Long userId);

    @Query("select b.userId from BoardBookmark b where b.boardId = :boardId")
    List<Long> findUserIdsByBoardId(
            @org.springframework.data.repository.query.Param("boardId") Long boardId);
}