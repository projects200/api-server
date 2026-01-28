package com.project200.undabang.feed.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.feed.dto.response.GetAllMemberFeedsResponse;
import com.project200.undabang.feed.repository.FeedRepository;
import com.project200.undabang.feed.service.FeedQueryService;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FeedQueryServiceImpl implements FeedQueryService {

    private final FeedRepository feedRepository;
    private final MemberRepository memberRepository;

    @Override
    public GetAllMemberFeedsResponse getAllMemberFeeds(Long prevFeedId, Pageable pageable) {
        Member member = getMember(UserContextHolder.getUserId());

        return GetAllMemberFeedsResponse.of(feedRepository.getAllFeedList(member, prevFeedId, pageable));
    }

    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
