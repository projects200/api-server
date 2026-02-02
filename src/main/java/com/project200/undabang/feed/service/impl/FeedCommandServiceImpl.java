package com.project200.undabang.feed.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.feed.dto.request.CreateFeedRequest;
import com.project200.undabang.feed.dto.response.CreateFeedResponse;
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
     * 주어진 요청 정보를 바탕으로 새 피드를 생성합니다.
     */
    @Override
    public CreateFeedResponse createMemberFeed(CreateFeedRequest request) {
        Member member = getMember(UserContextHolder.getUserId());

        FeedType feedType = null;

        if (request.getFeedTypeId() != null) {
            feedType = feedTypeRepository.findById(request.getFeedTypeId()).orElseThrow(() -> new CustomException(ErrorCode.FEED_TYPE_NOT_FOUND));
        }

        Feed savedFeed = feedRepository.save(Feed.create(member, request.getFeedContent(), feedType));

        return CreateFeedResponse.of(savedFeed);
    }

    /**
     * 주어진 회원 ID를 기반으로 회원 정보를 조회합니다.
     * 회원이 존재하지 않을 경우 예외를 발생시킵니다.
     *
     * @param memberId 조회할 회원의 UUID
     * @return 조회된 회원 엔티티
     * @throws CustomException 회원 정보를 찾을 수 없을 때 발생
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
