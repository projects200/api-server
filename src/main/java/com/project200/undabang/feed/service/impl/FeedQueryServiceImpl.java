package com.project200.undabang.feed.service.impl;

import com.project200.undabang.feed.dto.response.GetAllMemberFeedsResponse;
import com.project200.undabang.feed.repository.FeedRepository;
import com.project200.undabang.feed.service.FeedQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FeedQueryServiceImpl implements FeedQueryService {

    private final FeedRepository feedRepository;

    @Override
    public GetAllMemberFeedsResponse getAllMemberFeeds() {


        return null;
    }
}
