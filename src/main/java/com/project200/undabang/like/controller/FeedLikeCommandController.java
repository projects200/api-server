package com.project200.undabang.like.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.like.dto.CreateFeedLikeRequest;
import com.project200.undabang.like.dto.CreateFeedLikeResponse;
import com.project200.undabang.like.service.impl.FeedLikeCommandServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class FeedLikeCommandController {

    private final FeedLikeCommandServiceImpl feedLikeCommandService;

    @PostMapping("/v1/feeds/{feedId}/like")
    public ResponseEntity<CommonResponse<CreateFeedLikeResponse>> createFeedLike(@PathVariable Long feedId,
            @Valid @RequestBody CreateFeedLikeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.create(feedLikeCommandService.createFeedLike(feedId, request)));
    }
}
