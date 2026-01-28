package com.project200.undabang.feed.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.feed.dto.response.GetAllMemberFeedsResponse;
import com.project200.undabang.feed.dto.response.GetSpecificFeedResponse;
import com.project200.undabang.feed.service.FeedQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class FeedQueryController {

    private final FeedQueryService feedQueryService;

    @GetMapping("/v1/feeds")
    public ResponseEntity<CommonResponse<GetAllMemberFeedsResponse>> getAllMemberFeeds(@RequestParam(value = "prevFeedId", required = false) Long prevFeedId,
                                                                                       @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(CommonResponse.success(feedQueryService.getAllMemberFeeds(prevFeedId, pageable)));
    }

    @GetMapping("/v1/feeds/{feedId}")
    public ResponseEntity<CommonResponse<GetSpecificFeedResponse>> getSpecificFeed(@PathVariable Long feedId) {

        return ResponseEntity.ok(CommonResponse.success(feedQueryService.getSpecificFeed(feedId)));
    }
}
