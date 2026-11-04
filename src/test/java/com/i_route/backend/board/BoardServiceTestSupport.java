package com.i_route.backend.board;

import com.i_route.backend.board.entity.*;
import com.i_route.backend.board.repository.*;
import com.i_route.backend.board.service.BoardService;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.UserRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
abstract class BoardServiceTestSupport {
    @InjectMocks BoardService service;
    @Mock BoardRepository boards;
    @Mock PostRepository posts;
    @Mock CommentRepository comments;
    @Mock UserRepository users;
    @Mock PostLikeRepository postLikes;
    @Mock PostBookmarkRepository bookmarks;
    @Mock CommentLikeRepository commentLikes;

    User author() { return User.builder().id(7L).nickname("작성자").build(); }
    Board board() { return Board.builder().id(1L).name("게시판").build(); }
    Post post() { return Post.builder().id(10L).board(board()).user(author()).title("제목").content("내용").category("자유").build(); }
    Comment comment() { return Comment.builder().id(20L).post(post()).user(author()).author("작성자").content("댓글").build(); }
}
