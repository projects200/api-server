package com.project200.undabang.comment.service.impl;

import com.project200.undabang.comment.dto.request.CreateCommentRequest;
import com.project200.undabang.comment.dto.response.CreateCommentResponse;
import com.project200.undabang.comment.entity.Comment;
import com.project200.undabang.comment.entity.CommentTag;
import com.project200.undabang.comment.repository.CommentRepository;
import com.project200.undabang.comment.repository.CommentTagRepository;
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
    private final CommentTagRepository commentTagRepository;
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
        Comment parentComment = request.parentCommentId() == null ? null
                : commentRepository.findByIdAndDeletedAtIsNull(request.parentCommentId())
                .orElseThrow(() -> new CustomException(
                        ErrorCode.COMMENT_PARENT_NOT_FOUND));

        // 댓글 생성
        Comment comment = Comment.create(member, feed, parentComment, request);
        Comment savedComment = commentRepository.save(comment);

        // 태그 처리
        if (request.taggedMemberId() != null) {
            // 대댓글인지 검증
            if (request.parentCommentId() == null) {
                throw new CustomException(ErrorCode.COMMENT_TAG_NOT_ALLOWED);
            }

            // 태그된 회원 존재 여부 검증
            Member taggedMember = memberRepository.findById(request.taggedMemberId())
                    .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            // CommentTag 생성 및 저장
            CommentTag commentTag = CommentTag.of(savedComment, taggedMember);
            commentTagRepository.save(commentTag);
        }

        return new CreateCommentResponse(savedComment.getId());
    }

    @Override
    public void deleteComment(Long commentId) {
        UUID currentUserId = UserContextHolder.getUserId();

        // 댓글 조회
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        // 작성자 권한 검증
        memberRepository.findByMemberIdAndMemberDeletedAtNull(currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        if (!comment.getMember().getMemberId().equals(currentUserId)) {
            throw new CustomException(ErrorCode.COMMENT_DELETE_FORBIDDEN);
        }

        // Soft delete
        comment.delete();
    }
}
