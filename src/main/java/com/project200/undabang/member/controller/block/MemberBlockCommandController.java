package com.project200.undabang.member.controller.block;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.member.dto.response.CreateMemberBlockResponse;
import com.project200.undabang.member.service.MemberBlockCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MemberBlockCommandController {
    private final MemberBlockCommandService memberBlockCommandService;

    @PostMapping("/v1/members/{memberId}/block")
    public ResponseEntity<CommonResponse<CreateMemberBlockResponse>> blockMember(@PathVariable UUID memberId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.create(memberBlockCommandService.createMemberBlock(memberId)));
    }

    @DeleteMapping("/v1/members/{memberId}/block")
    public ResponseEntity<CommonResponse<Void>> unBlockMember(@PathVariable UUID memberId) {

        memberBlockCommandService.unBlockMember(memberId);

        return ResponseEntity.ok(CommonResponse.delete(null));
    }
}
