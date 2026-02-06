package com.project200.undabang.comment.controller;

import com.project200.undabang.comment.dto.response.CommentResponse;
import com.project200.undabang.comment.service.CommentQueryService;
import com.project200.undabang.common.web.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CommentQueryController {

    private final CommentQueryService commentQueryService;

    @GetMapping("/v1/feeds/{feedId}/comments")
    public ResponseEntity<CommonResponse<List<CommentResponse>>> getComments(
            @PathVariable Long feedId) {
        return ResponseEntity.ok(
                CommonResponse.success(commentQueryService.getComments(feedId)));
    }
}
