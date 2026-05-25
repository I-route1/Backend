package com.i_route.backend.board.service;

import com.i_route.backend.board.dto.CommentDto;
import com.i_route.backend.board.entity.Comment;
import com.i_route.backend.board.entity.CommentLike;
import com.i_route.backend.board.entity.Post;
import com.i_route.backend.board.repository.CommentLikeRepository;
import com.i_route.backend.board.repository.CommentRepository;
import com.i_route.backend.board.repository.PostRepository;
import com.i_route.backend.user.entity.User;
import com.i_route.backend.user.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentLikeRepository commentLikeRepository;

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

        Comment saved = commentRepository.save(comment);

        boolean likedByMe = false; // 댓글 생성 직후는 보통 false

        return CommentDto.Response.from(saved, likedByMe);
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

    @Transactional
    public String toggleCommentLike(Long commentId, Long userId) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("댓글 없음"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유저 없음"));

        return commentLikeRepository.findByCommentIdAndUserId(commentId, userId)
                .map(existing -> {
                    commentLikeRepository.delete(existing);
                    return "댓글 좋아요 취소";
                })
                .orElseGet(() -> {
                    CommentLike like = new CommentLike();
                    like.setComment(comment);
                    like.setUser(user);
                    commentLikeRepository.save(like);
                    return "댓글 좋아요";
                });
    }

    public Page<CommentDto.Response> getComments(Long postId, Long userId, Pageable pageable) {

        return commentRepository.findByPostId(postId, pageable)
                .map(comment -> CommentDto.Response.from(
                        comment,
                        commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), userId)
                ));
    }
}

