package com.project200.undabang.member.controller.block;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.member.dto.response.GetBlockedMembersResponse;
import com.project200.undabang.member.service.MemberBlockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MemberBlockQueryController {
    private final MemberBlockQueryService memberBlockQueryService;

    @GetMapping("/v1/members/blocks")
    public ResponseEntity<CommonResponse<List<GetBlockedMembersResponse>>> getBlockedMembers() {

        return ResponseEntity.ok(CommonResponse.success(memberBlockQueryService.getBlockedMembers()));
    }
}
