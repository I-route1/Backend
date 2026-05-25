package com.i_route.backend.board.service;

import com.i_route.backend.board.dto.BoardDto;
import com.i_route.backend.board.dto.PostDto;
import com.i_route.backend.board.entity.Board;
import com.i_route.backend.board.entity.BoardBookmark;
import com.i_route.backend.board.repository.BoardBookmarkRepository;
import com.i_route.backend.board.repository.BoardRepository;
import com.i_route.backend.board.repository.PostRepository;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public List<BoardDto.Response> getAllBoards() {
        return boardRepository.findAll().stream()
                .map(BoardDto.Response::from)
                .collect(Collectors.toList());
    }

    public List<BoardDto.Response> searchBoards(String keyword) {
        return boardRepository.findByNameContainingIgnoreCase(keyword).stream()
                .map(BoardDto.Response::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public BoardDto.Response createBoard(BoardDto.Request request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."));

        Board board = new Board();
        board.setName(request.getName());
        board.setDescription(request.getDescription());
        board.setCreatedBy(user);

        return BoardDto.Response.from(boardRepository.save(board));
    }

    @Transactional
    public BoardDto.Response updateBoard(Long boardId, BoardDto.Request request, Long userId) {
        Board board = getBoardOrThrow(boardId);
        checkOwner(board.getCreatedBy().getId(), userId);

        board.setName(request.getName());
        board.setDescription(request.getDescription());

        return BoardDto.Response.from(board);
    }

    @Transactional
    public void deleteBoard(Long boardId, Long userId) {
        Board board = getBoardOrThrow(boardId);
        checkOwner(board.getCreatedBy().getId(), userId);
        boardRepository.delete(board);
    }

    private Board getBoardOrThrow(Long id) {
        return boardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("게시판을 찾을 수 없습니다."));
    }

    private void checkOwner(Long ownerId, Long requesterId) {
        if (!ownerId.equals(requesterId)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }
    }

    // BoardService 필드에 추가
    private final BoardBookmarkRepository boardBookmarkRepository;

    // 게시판 즐겨찾기 토글
    @Transactional
    public String toggleBoardBookmark(Long boardId, Long userId) {
        Board board = getBoardOrThrow(boardId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."));

        Optional<BoardBookmark> existing = boardBookmarkRepository.findByUserIdAndBoardId(userId, boardId);
        if (existing.isPresent()) {
            boardBookmarkRepository.delete(existing.get());
            return "게시판 즐겨찾기를 해제했습니다.";
        }

        BoardBookmark bookmark = new BoardBookmark();
        bookmark.setUser(user);
        bookmark.setBoard(board);
        boardBookmarkRepository.save(bookmark);
        return "게시판 즐겨찾기에 추가했습니다.";
    }

    // 내 게시판 즐겨찾기 목록
    public List<BoardDto.Response> getMyBoardBookmarks(Long userId) {
        return boardBookmarkRepository.findByUserId(userId).stream()
                .map(b -> BoardDto.Response.from(b.getBoard()))
                .collect(Collectors.toList());
    }

    public BoardDto.DetailResponse getBoardDetail(Long boardId, Long userId, Pageable pageable) {
        Board board = getBoardOrThrow(boardId);
        boolean bookmarked = boardBookmarkRepository.existsByUserIdAndBoardId(userId, boardId);

        Page<PostDto.ListResponse> posts = postRepository
                .findByBoardIdAndDeletedAtIsNull(boardId, pageable)
                .map(PostDto.ListResponse::from);

        return BoardDto.DetailResponse.builder()
                .id(board.getId())
                .name(board.getName())
                .description(board.getDescription())
                .createdBy(board.getCreatedBy().getNickname())
                .createdAt(board.getCreatedAt())
                .bookmarkedByMe(bookmarked)
                .posts(posts)
                .build();
    }

}
