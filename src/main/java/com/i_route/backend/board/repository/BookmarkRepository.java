package com.i_route.backend.domain.board.repository;

import com.i_route.backend.domain.board.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByUserIdAndPostId(Long userId, Long postId);
    List<Bookmark> findByUserId(Long userId);
    boolean existsByUserIdAndPostId(Long userId, Long postId);
}
