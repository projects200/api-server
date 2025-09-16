package com.project200.undabang.member.dto.response;

import com.project200.undabang.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfilePictureResponse {
    private long profileImageId;

    public static UpdateProfilePictureResponse from(Member member) {
        return UpdateProfilePictureResponse
                .builder()
                .profileImageId(member.getMemberPicture().getId())
                .build();
    }
}
