package com.project200.undabang.feed.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.feed.dto.request.CreateFeedRequest;
import com.project200.undabang.feed.dto.request.UpdateFeedRequest;
import com.project200.undabang.feed.dto.response.CreateFeedResponse;
import com.project200.undabang.feed.dto.response.UpdateFeedResponse;
import com.project200.undabang.feed.entity.Feed;
import com.project200.undabang.feed.entity.FeedType;
import com.project200.undabang.feed.repository.FeedRepository;
import com.project200.undabang.feed.repository.FeedTypeRepository;
import com.project200.undabang.feed.service.FeedCommandService;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedCommandServiceImpl implements FeedCommandService {
    private final FeedRepository feedRepository;
    private final FeedTypeRepository feedTypeRepository;
    private final MemberRepository memberRepository;

    /**
     * 주어진 피드 ID를 바탕으로 회원의 피드를 삭제합니다.
     */
    @Override
    public Void deleteMemberFeed(Long feedId) {
        Member member = getMember(UserContextHolder.getUserId());

        Feed feed = getFeed(feedId, member);
        feed.delete();

        return null;
    }

    /**
     * 주어진 피드 ID와 업데이트 요청 정보를 바탕으로 피드 정보를 수정합니다.
     */
    @Override
    public UpdateFeedResponse updateMemberFeed(Long feedId, UpdateFeedRequest request) {
        Member member = getMember(UserContextHolder.getUserId());

        Feed feed = getFeed(feedId, member);
        FeedType feedType = getFeedType(request.getFeedTypeId());

        feed.update(request.getFeedContent(), feedType);

        return UpdateFeedResponse.of(feed);
    }

    /**
     * 주어진 요청 정보를 바탕으로 새 피드를 생성합니다.
     */
    @Override
    public CreateFeedResponse createMemberFeed(CreateFeedRequest request) {
        Member member = getMember(UserContextHolder.getUserId());

        FeedType feedType = getFeedType(request.getFeedTypeId());
        Feed savedFeed = feedRepository.save(Feed.create(member, request.getFeedContent(), feedType));

        return CreateFeedResponse.of(savedFeed);
    }

    /**
     * 주어진 피드 ID를 기준으로 피드 정보를 조회합니다.
     * 피드가 존재하지 않을 경우 예외를 발생시킵니다.
     */
    private Feed getFeed(Long feedId, Member member) {
        return feedRepository.findByIdAndMemberAndDeletedAtNull(feedId, member).orElseThrow(() -> new CustomException(ErrorCode.FEED_NOT_FOUND));
    }

    /**
     * 주어진 feedTypeId를 이용하여 FeedType 엔티티를 조회합니다.
     * feedTypeId가 null인 경우 null을 반환하며, 조회 실패 시 예외를 발생시킵니다.
     */
    private FeedType getFeedType(Long feedTypeId) {
        if (feedTypeId == null) {
            return null;
        }

        return feedTypeRepository.findById(feedTypeId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEED_TYPE_NOT_FOUND));
    }

    /**
     * 주어진 회원 ID를 기반으로 회원 정보를 조회합니다.
     * 회원이 존재하지 않을 경우 예외를 발생시킵니다.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
