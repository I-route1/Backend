package com.i_route.backend.board.repository;

import com.i_route.backend.board.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {

    // 게시판 이름 키워드 검색
    List<Board> findByNameContaining(String keyword);

    // 특정 학원의 게시판 목록 조회 (필요 시 활용)
    List<Board> findByAcademy_AcademyId(Long academyId);

    // 게시판 상세 조회 시 Academy와 담당 User까지 한 번에 로딩 (LazyInitialization 방지)
    @Query("SELECT b FROM Board b LEFT JOIN FETCH b.academy a LEFT JOIN FETCH a.user WHERE b.id = :id")
    Optional<Board> findByIdWithAcademyAndUser(@Param("id") Long id);

    // 전체 게시판 목록 조회 시에도 Academy와 담당 User를 함께 로딩
    @Query("SELECT b FROM Board b LEFT JOIN FETCH b.academy a LEFT JOIN FETCH a.user")
    List<Board> findAllWithAcademyAndUser();
}