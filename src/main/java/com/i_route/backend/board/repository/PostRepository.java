package com.i_route.backend.board.repository;

import com.i_route.backend.board.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 1. 특정 게시판(boardId)의 글 목록 조회
    List<Post> findByBoardId(Long boardId);

    List<Post> findByBoard_Academy_AcademyId(Long academyId);

    // 3. 제목 또는 내용 키워드 검색
    List<Post> findByTitleContainingOrContentContaining(String title, String content);

    // 4. 회원 탈퇴 시 작성글 일괄 삭제
    void deleteByUser_Id(Long userId);
}