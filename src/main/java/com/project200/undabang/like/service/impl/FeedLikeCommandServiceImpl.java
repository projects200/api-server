package com.project200.undabang.like.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.feed.entity.Feed;
import com.project200.undabang.feed.repository.FeedRepository;
import com.project200.undabang.like.dto.CreateFeedLikeRequest;
import com.project200.undabang.like.dto.CreateFeedLikeResponse;
import com.project200.undabang.like.entity.FeedLike;
import com.project200.undabang.like.repository.FeedLikeRepository;
import com.project200.undabang.like.service.FeedLikeCommandService;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedLikeCommandServiceImpl implements FeedLikeCommandService {

    private final FeedLikeRepository feedLikeRepository;
    private final MemberRepository memberRepository;
    private final FeedRepository feedRepository;

    @Override
    public CreateFeedLikeResponse createFeedLike(Long feedId, @Valid CreateFeedLikeRequest request) {
        UUID currentUserId = UserContextHolder.getUserId();

        // 피드 존재 여부 검즘
        Feed feed = feedRepository.findByIdAndDeletedAtNull(feedId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEED_NOT_FOUND));

        // 현재 사용자 조회
        Member member = memberRepository.findByMemberIdAndMemberDeletedAtNull(currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 피드 좋아요 생성
        Optional<FeedLike> existingLike = feedLikeRepository.findByFeedAndMember(feed, member);

        if (Boolean.TRUE.equals(request.liked())) {
            // 좋아요 생성: 이미 존재하면 아무것도 안 함
            if (existingLike.isEmpty()) {
                feedLikeRepository.save(FeedLike.create(feed, member));
                feed.incrementLikesCount();
            }
        } else {
            // 좋아요 취소: 존재하면 삭제
            existingLike.ifPresent(like -> {
                feedLikeRepository.delete(like);
                feed.decrementLikesCount();
            });
        }
        return new CreateFeedLikeResponse(request.liked(), feed.getLikesCount());
    }
}
