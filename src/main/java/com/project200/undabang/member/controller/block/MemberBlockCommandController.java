package com.project200.undabang.member.controller.block;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.member.dto.response.CreateMemberBlockResponse;
import com.project200.undabang.member.service.MemberBlockCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MemberBlockCommandController {
    private final MemberBlockCommandService memberBlockCommandService;

    @PostMapping("/v1/members/{memberId}/block")
    public ResponseEntity<CommonResponse<CreateMemberBlockResponse>> blockMember(@PathVariable UUID memberId) {


        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.create(memberBlockCommandService.CreateMemberBlock(memberId)));
    }
}
