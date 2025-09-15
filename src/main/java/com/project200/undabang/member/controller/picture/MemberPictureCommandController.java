package com.project200.undabang.member.controller.picture;

import com.project200.undabang.common.validation.AllowedExtensions;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.member.dto.response.CreateProfilePictureResponse;
import com.project200.undabang.member.dto.response.UpdateProfilePictureResponse;
import com.project200.undabang.member.service.MemberPictureCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MemberPictureCommandController {

    private final MemberPictureCommandService memberPictureCommandService;

    /**
     * 사용자 프로필 사진을 생성합니다. 요청된 파일은 특정 확장자(.jpg, .jpeg, .png)만 허용됩니다.
     *
     * @param profilePicture 업로드할 프로필 사진 파일(MultipartFile 형식).
     *                       반드시 .jpg, .jpeg, .png 확장자 중 하나여야 합니다.
     * @return 생성된 프로필 사진 정보를 포함하는 ResponseEntity 객체를 반환합니다.
     * 반환 데이터는 CommonResponse<CreateProfilePictureResponse> 형태입니다.
     */
    @PostMapping(path = "/v1/profile-pictures", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<CreateProfilePictureResponse>> createProfilePicture(
            @AllowedExtensions(extensions = {".jpg", ".jpeg", ".png"})
            @RequestPart("profilePicture") MultipartFile profilePicture) {

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.create(memberPictureCommandService.createProfilePicture(profilePicture)));
    }

    @DeleteMapping("/v1/profile-pictures/{pictureId}")
    public ResponseEntity<CommonResponse<Void>> deleteProfilePicture(@PathVariable Long pictureId) {

        return ResponseEntity.ok(CommonResponse.delete(null));
    }

    /**
     * 특정 프로필 사진을 대표 이미지로 업데이트합니다.
     *
     * @param pictureId 대표 이미지로 설정할 프로필 사진의 고유 ID
     * @return 업데이트된 대표 프로필 사진 정보를 포함하는 ResponseEntity 객체를 반환합니다.
     * 반환 데이터는 CommonResponse<UpdateProfilePictureResponse> 형태입니다.
     */
    @PutMapping("/v1/profile-pictures/{pictureId}/represent")
    public ResponseEntity<CommonResponse<UpdateProfilePictureResponse>> updateRepresentativeProfileImage(@PathVariable Long pictureId) {

        return ResponseEntity.ok(CommonResponse.update(memberPictureCommandService.updateRepresentativeProfileImage(pictureId)));
    }
}
