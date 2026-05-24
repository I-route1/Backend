package com.i_route.backend.domain.board.service;

import com.i_route.backend.domain.board.dto.CommentDto;
import com.i_route.backend.domain.board.entity.Comment;
import com.i_route.backend.domain.board.entity.Post;
import com.i_route.backend.domain.board.repository.CommentRepository;
import com.i_route.backend.domain.board.repository.PostRepository;
import com.i_route.backend.domain.user.entity.User;
import com.i_route.backend.domain.user.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentDto.Response addComment(Long postId, CommentDto.Request request, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."));

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setAuthor(user);
        comment.setContent(request.getContent());

        return CommentDto.Response.from(commentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("댓글을 찾을 수 없습니다."));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }

        commentRepository.delete(comment);
    }
}

