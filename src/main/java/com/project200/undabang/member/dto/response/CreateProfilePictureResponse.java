package com.project200.undabang.member.dto.response;

import com.project200.undabang.common.entity.Picture;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProfilePictureResponse {
    private Long pictureId;
    private String profileImageUrl;

    public static CreateProfilePictureResponse from(Picture picture) {
        // todo 추후 섬네일 개발시 null 자리에 생성된 memberPicture.getMemberPicturesUrl()을 추가해야 함
        return CreateProfilePictureResponse.builder()
                .pictureId(picture.getId())
                .profileImageUrl(picture.getPictureUrl())
                .build();
    }
}
