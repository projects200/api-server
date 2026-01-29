package com.project200.undabang.comment.service.impl;

import com.project200.undabang.comment.dto.request.CreateCommentRequest;
import com.project200.undabang.comment.dto.response.CreateCommentResponse;
import com.project200.undabang.comment.entity.Comment;
import com.project200.undabang.comment.repository.CommentRepository;
import com.project200.undabang.comment.service.CommentCommandService;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.feed.entity.Feed;
import com.project200.undabang.feed.repository.FeedRepository;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentCommandServiceImpl implements CommentCommandService {

    private final CommentRepository commentRepository;
    private final FeedRepository feedRepository;
    private final MemberRepository memberRepository;

    @Override
    public CreateCommentResponse createComment(Long feedId, CreateCommentRequest request) {
        UUID currentUserId = UserContextHolder.getUserId();

        // 피드 존재 여부 검증
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEED_NOT_FOUND));

        // 현재 사용자 조회
        Member member = memberRepository.findByMemberIdAndMemberDeletedAtNull(currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 부모 댓글 조회 (대댓글인 경우)
        Comment parentComment = null;
        if (request.parentCommentId() != null) {
            parentComment = commentRepository.findByIdAndDeletedAtIsNull(request.parentCommentId())
                    .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_PARENT_NOT_FOUND));
        }

        // 댓글 생성
        Comment comment = Comment.builder()
                .member(member)
                .feed(feed)
                .parent(parentComment)
                .content(request.content())
                .build();

        Comment savedComment = commentRepository.save(comment);

        return new CreateCommentResponse(savedComment.getId());
    }

    @Override
    public void deleteComment(Long commentId) {
        UUID currentUserId = UserContextHolder.getUserId();

        // 댓글 조회
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        // 작성자 권한 검증
        if (!comment.getMember().getMemberId().equals(currentUserId)) {
            throw new CustomException(ErrorCode.COMMENT_DELETE_FORBIDDEN);
        }

        // Soft delete
        comment.delete();
    }
}
