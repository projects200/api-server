package com.project200.undabang.comment.controller;

import com.project200.undabang.comment.dto.request.CreateCommentRequest;
import com.project200.undabang.comment.dto.response.CreateCommentResponse;
import com.project200.undabang.comment.service.CommentCommandService;
import com.project200.undabang.common.web.response.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CommentCommandController {

    private final CommentCommandService commentCommandService;

    @PostMapping("/v1/feeds/{feedId}/comments")
    public ResponseEntity<CommonResponse<CreateCommentResponse>> createComment(
            @PathVariable Long feedId,
            @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.create(
                        commentCommandService.createComment(feedId, request)));
    }

    @DeleteMapping("/v1/comments/{commentId}")
    public ResponseEntity<CommonResponse<Void>> deleteComment(
            @PathVariable Long commentId) {
        commentCommandService.deleteComment(commentId);
        return ResponseEntity.ok(CommonResponse.delete(null));
    }

}
