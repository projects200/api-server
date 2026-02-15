package com.project200.undabang.like.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.like.dto.request.CreateCommentLikeRequest;
import com.project200.undabang.like.dto.response.CreateCommentLikeResponse;
import com.project200.undabang.like.service.impl.CommentCommandLikeServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CommentLikeCommandController {

    private final CommentCommandLikeServiceImpl commentCommandLikeService;

    @PostMapping("/v1/comments/{commentId}/like")
    public ResponseEntity<CommonResponse<CreateCommentLikeResponse>> createCommentLike(@PathVariable Long commentId,
                                                                                       @Valid @RequestBody CreateCommentLikeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.create(commentCommandLikeService.createCommentLike(commentId, request)));

    }
}
