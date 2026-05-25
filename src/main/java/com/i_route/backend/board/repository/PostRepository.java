package com.i_route.backend.board.repository;

import com.i_route.backend.board.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // 삭제되지 않은 게시글만 조회
    Page<Post> findByBoardIdAndDeletedAtIsNull(Long boardId, Pageable pageable);

    // 제목 + 내용 검색
    @Query("SELECT p FROM Post p WHERE p.deletedAt IS NULL AND " +
            "(p.title LIKE %:keyword% OR p.content LIKE %:keyword%)")
    Page<Post> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);


}
