package com.project200.undabang.feed.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.feed.dto.response.FeedDetailResponse;
import com.project200.undabang.feed.dto.response.GetAllMemberFeedsResponse;
import com.project200.undabang.feed.dto.response.GetMyPageFeedsResponse;
import com.project200.undabang.feed.dto.response.GetSpecificFeedResponse;
import com.project200.undabang.feed.repository.FeedRepository;
import com.project200.undabang.feed.service.FeedQueryService;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FeedQueryServiceImpl implements FeedQueryService {

    private final FeedRepository feedRepository;
    private final MemberRepository memberRepository;

    /**
     * 이전 피드 ID와 페이지 정보(Pageable)를 기반으로 회원의 피드 목록을 마이페이지에서 조회합니다.
     */
    @Override
    public GetMyPageFeedsResponse getMyPageFeeds(Long prevFeedId, Pageable pageable) {
        Member member = memberRepository.findMemberWithProfileImage(UserContextHolder.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Slice<FeedDetailResponse> feedSliceList = feedRepository.getMyPageFeedList(member, prevFeedId, pageable);

        return GetMyPageFeedsResponse.from(member, feedSliceList);
    }

    /**
     * 주어진 피드 ID를 기반으로 특정 피드의 세부 정보를 조회합니다.
     */
    @Override
    public GetSpecificFeedResponse getSpecificFeed(Long feedId) {
        Member member = getMember(UserContextHolder.getUserId());

        return feedRepository.getSpecificFeed(member, feedId).orElseThrow(() -> new CustomException(ErrorCode.FEED_NOT_FOUND));
    }

    /**
     * 이전 피드 ID와 페이지 정보를 바탕으로 모든 피드 목록을 조회합니다.
     */
    @Override
    public GetAllMemberFeedsResponse getAllMemberFeeds(Long prevFeedId, Pageable pageable) {
        Member member = getMember(UserContextHolder.getUserId());

        return GetAllMemberFeedsResponse.of(feedRepository.getAllFeedList(member, prevFeedId, pageable));
    }

    /**
     * 주어진 회원 ID를 기반으로 회원 정보를 조회합니다.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
