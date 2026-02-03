package com.project200.undabang.like.service.impl;

import com.project200.undabang.comment.entity.Comment;
import com.project200.undabang.comment.repository.CommentRepository;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.like.dto.request.CreateCommentLikeRequest;
import com.project200.undabang.like.dto.response.CreateCommentLikeResponse;
import com.project200.undabang.like.entity.CommentLike;
import com.project200.undabang.like.repository.CommentLikeRepository;
import com.project200.undabang.like.service.CommentCommandLikeService;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentCommandLikeServiceImpl implements CommentCommandLikeService {

    private final CommentLikeRepository commentLikeRepository;
    private final MemberRepository memberRepository;
    private final CommentRepository commentRepository;

    @Override
    public CreateCommentLikeResponse createCommentLike(Long commentId, CreateCommentLikeRequest request) {
        UUID currentUserId = UserContextHolder.getUserId();

        // 댓글 존재 여부 검증
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        // 현재 사용자 조회
        Member member = memberRepository.findByMemberIdAndMemberDeletedAtNull(currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 댓글 좋아요 생성
        Optional<CommentLike> existingLike = commentLikeRepository.findByCommentAndMember(comment, member);

        if (Boolean.TRUE.equals(request.status())) {
            // 좋아요 생성: 이미 존재하면 아무것도 안 함
            if (existingLike.isEmpty()) {
                commentLikeRepository.save(CommentLike.create(comment, member));
            }
        } else {
            // 좋아요 취소: 존재하면 삭제
            existingLike.ifPresent(commentLikeRepository::delete);
        }
        return new CreateCommentLikeResponse();
    }
}
