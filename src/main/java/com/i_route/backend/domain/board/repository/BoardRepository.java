package com.i_route.backend.domain.board.repository;

import com.i_route.backend.domain.board.entity.Board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findByNameContainingIgnoreCase(String keyword);
}
