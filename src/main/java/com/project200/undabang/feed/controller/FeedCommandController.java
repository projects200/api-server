package com.project200.undabang.feed.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.feed.dto.request.CreateFeedRequest;
import com.project200.undabang.feed.dto.request.UpdateFeedRequest;
import com.project200.undabang.feed.dto.response.CreateFeedResponse;
import com.project200.undabang.feed.dto.response.UpdateFeedResponse;
import com.project200.undabang.feed.service.FeedCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class FeedCommandController {

    private final FeedCommandService feedCommandService;

    /**
     * 사용자가 제공한 요청 데이터를 기반으로 새로운 피드 리소스를 생성합니다.
     */
    @PostMapping("/v1/feeds")
    public ResponseEntity<CommonResponse<CreateFeedResponse>> createMemberFeed(@Valid @RequestBody CreateFeedRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.create(feedCommandService.createMemberFeed(request)));
    }

    /**
     * 특정 피드의 정보를 업데이트합니다.
     * 피드 ID에 해당하는 리소스를 찾아 사용자가 제공한 요청 데이터를 기반으로 수정합니다.
     */
    @PatchMapping("/v1/feeds/{feedId}")
    public ResponseEntity<CommonResponse<UpdateFeedResponse>> updateMemberFeed(@PathVariable Long feedId,
                                                                               @Valid @RequestBody UpdateFeedRequest request) {

        return ResponseEntity.ok(CommonResponse.update(feedCommandService.updateMemberFeed(feedId, request)));
    }

    /**
     * 주어진 피드 ID를 기반으로 특정 피드를 논리적으로 삭제합니다.
     */
    @DeleteMapping("/v1/feeds/{feedId}")
    public ResponseEntity<CommonResponse<Void>> deleteMemberFeed(@PathVariable Long feedId) {

        return ResponseEntity.ok(CommonResponse.delete(feedCommandService.deleteMemberFeed(feedId)));
    }
}
