package com.project200.undabang.member.controller.picture;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.member.dto.response.GetProfilePictureResponse;
import com.project200.undabang.member.service.MemberPictureQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MemberPictureQueryController {
    private final MemberPictureQueryService memberPictureQueryService;

    @GetMapping("/v1/profile-pictures")
    public ResponseEntity<CommonResponse<GetProfilePictureResponse>> getProfilePictures() {

        return ResponseEntity.ok(CommonResponse.success(memberPictureQueryService.getProfilePictures()));
    }
}
