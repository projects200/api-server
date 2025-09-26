package com.project200.undabang.member.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordByPeriodResponseDto;
import com.project200.undabang.member.dto.response.GetOtherMemberProfileResponse;
import com.project200.undabang.member.dto.response.MemberProfileResponse;
import com.project200.undabang.member.dto.response.MemberScoreResponseDto;
import com.project200.undabang.member.service.MemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * MemberRestController는 회원과 관련된 RESTful API를 처리하는 컨트롤러입니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MemberQueryController {
    private final MemberQueryService memberQueryService;

    @GetMapping("/v1/members/score")
    public ResponseEntity<CommonResponse<MemberScoreResponseDto>> getMemberScore() {
        return ResponseEntity.ok(CommonResponse.success(memberQueryService.getMemberScore()));
    }

    @GetMapping("/v1/profile")
    public MemberProfileResponse getProfile() {
        return memberQueryService.getMemberProfile();
    }


    @GetMapping("/v1/members/{memberId}/profile")
    public ResponseEntity<CommonResponse<GetOtherMemberProfileResponse>> getOtherMemberProfile(@PathVariable UUID memberId) {
        return ResponseEntity.ok(CommonResponse.success(memberQueryService.getOtherMemberProfile(memberId)));
    }

    @GetMapping("/v1/members/{memberId}/calendars")
    public ResponseEntity<CommonResponse<List<FindExerciseRecordByPeriodResponseDto>>> getOtherMemberCalendars(@PathVariable UUID memberId,
                                                                                                               @RequestParam(value = "start") LocalDate startDate,
                                                                                                               @RequestParam(value = "end") LocalDate endDate) {

        return ResponseEntity.ok(CommonResponse.success(memberQueryService.getOtherMemberCalendars(memberId, startDate, endDate)));
    }

}