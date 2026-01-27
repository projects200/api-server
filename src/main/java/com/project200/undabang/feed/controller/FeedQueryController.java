package com.project200.undabang.feed.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.feed.dto.response.GetAllMemberFeedsResponse;
import com.project200.undabang.feed.service.FeedQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class FeedQueryController {

    private final FeedQueryService feedQueryService;

    @GetMapping("/v1/feeds")
    public ResponseEntity<CommonResponse<GetAllMemberFeedsResponse>> getAllMemberFeeds() {

        return feedQueryService.getAllMemberFeeds();
    }
}
