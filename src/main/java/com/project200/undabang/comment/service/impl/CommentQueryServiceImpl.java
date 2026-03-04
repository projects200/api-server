package com.project200.undabang.comment.service.impl;

import com.project200.undabang.comment.dto.response.CommentResponse;
import com.project200.undabang.comment.repository.CommentRepository;
import com.project200.undabang.comment.service.CommentQueryService;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.feed.repository.FeedRepository;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryServiceImpl implements CommentQueryService {

    private final CommentRepository commentRepository;
    private final FeedRepository feedRepository;
    private final MemberRepository memberRepository;

    @Override
    public List<CommentResponse> getComments(Long feedId) {
        // 피드 존재 여부 검증
        if (!feedRepository.existsById(feedId)) {
            throw new CustomException(ErrorCode.FEED_NOT_FOUND);
        }

        // 현재 사용자 조회 (비로그인 시 null)
        UUID currentUserId = UserContextHolder.getUserId();
        Member currentMember = memberRepository
                .findByMemberIdAndMemberDeletedAtNull(currentUserId)
                .orElse(null);

        return commentRepository.findCommentsWithChildrenByFeedId(feedId, currentMember);
    }
}
