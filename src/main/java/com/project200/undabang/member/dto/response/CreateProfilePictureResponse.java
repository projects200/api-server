package com.project200.undabang.member.dto.response;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.member.entity.MemberPicture;
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
    private String profileThumbnailUrl;
    private String profileImageUrl;

    public static CreateProfilePictureResponse from(Picture picture, MemberPicture memberPicture) {
        return CreateProfilePictureResponse.builder()
                .pictureId(picture.getId())
                .profileThumbnailUrl(memberPicture.getMemberPicturesUrl())
                .profileImageUrl(picture.getPictureUrl())
                .build();
    }
}
