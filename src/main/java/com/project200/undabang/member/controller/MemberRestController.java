package com.project200.undabang.member.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.member.dto.response.MemberScoreResponseDto;
import com.project200.undabang.member.service.MemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MemberRestController는 회원과 관련된 RESTful API를 처리하는 컨트롤러입니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MemberRestController {
    private final MemberQueryService memberQueryService;

    @GetMapping("/v1/members/score")
    public ResponseEntity<CommonResponse<MemberScoreResponseDto>> getMemberScore() {
        return ResponseEntity.ok(CommonResponse.success(memberQueryService.getMemberScore()));
    }
}