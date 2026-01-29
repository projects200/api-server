package com.project200.undabang.feed.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.feed.dto.response.GetAllMemberFeedsResponse;
import com.project200.undabang.feed.dto.response.GetMyPageFeedsResponse;
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

    /**
     * 모든 회원 피드를 조회합니다. 이전 피드 ID와 페이지 정보를 기반으로 결과를 필터링합니다.
     *
     * @param prevFeedId 이전 피드의 ID로, 해당 ID 이후의 피드를 조회합니다. 필수 값이 아니며, 제공되지 않을 경우 첫 번째 페이지부터 조회합니다.
     * @param pageable   페이지 정보로, 페이지 크기나 정렬 옵션 등을 지정할 수 있습니다.
     * @return {@code ResponseEntity<CommonResponse<GetAllMemberFeedsResponse>>} 형태로 응답하며,
     * 요청된 조건에 맞는 피드 목록과 추가 페이지 여부를 포함합니다.
     */
    @GetMapping("/v1/feeds")
    public ResponseEntity<CommonResponse<GetAllMemberFeedsResponse>> getAllMemberFeeds(@RequestParam(value = "prevFeedId", required = false) Long prevFeedId,
                                                                                       @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(CommonResponse.success(feedQueryService.getAllMemberFeeds(prevFeedId, pageable)));
    }

    /**
     * 특정 피드의 정보를 조회합니다.
     *
     * @param feedId 조회할 피드의 고유 ID입니다.
     * @return {@code ResponseEntity<CommonResponse<GetSpecificFeedResponse>>} 형태로 응답하며,
     *         특정 피드의 상세 정보를 포함합니다.
     */
    @GetMapping("/v1/feeds/{feedId}")
    public ResponseEntity<CommonResponse<GetSpecificFeedResponse>> getSpecificFeed(@PathVariable Long feedId) {

        return ResponseEntity.ok(CommonResponse.success(feedQueryService.getSpecificFeed(feedId)));
    }

    @GetMapping("/v1/mypage/feeds")
    public ResponseEntity<CommonResponse<GetMyPageFeedsResponse>> getAllMyFeeds(@RequestParam(value = "prevFeedId", required = false) Long prevFeedId,
                                                                                @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(CommonResponse.success(feedQueryService.getMyPageFeeds(prevFeedId, pageable)));
    }
}
