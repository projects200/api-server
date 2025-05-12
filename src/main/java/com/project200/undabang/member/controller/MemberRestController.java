package com.project200.undabang.member.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.member.dto.response.MemberRegistrationStatusResponseDto;
import com.project200.undabang.member.service.MemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MemberRestController는 회원과 관련된 RESTful API를 처리하는 컨트롤러입니다.
 * 특정 회원의 등록 상태를 조회하는 기능을 제공합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/members")
public class MemberRestController {

    private final MemberQueryService memberQueryService;

    /**
     * 현재 회원의 등록 상태를 조회합니다.
     *
     * @return 회원 등록 상태를 포함한 ApiResponse 객체를 반환합니다.
     *         반환되는 데이터는 MemberRegistrationStatusResponseDto 형태입니다.
     */
    @GetMapping("/me/registration-status")
    public ResponseEntity<CommonResponse<MemberRegistrationStatusResponseDto>> getRegistrationStatus() {
        return ResponseEntity.ok(CommonResponse.success(memberQueryService.getRegistrationStatus()));
    }
}