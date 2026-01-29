package com.project200.undabang.feed.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.feed.dto.request.CreateFeedRequest;
import com.project200.undabang.feed.dto.response.CreateFeedResponse;
import com.project200.undabang.feed.service.FeedCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class FeedCommandController {

    private final FeedCommandService feedCommandService;

    @PostMapping("/v1/feeds")
    public ResponseEntity<CommonResponse<CreateFeedResponse>> createMemberFeed(@RequestBody CreateFeedRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.create(feedCommandService.createMemberFeed(request)));
    }
}
