package com.project200.undabang.member.dto.response;

import com.project200.undabang.member.dto.record.ProfileImageRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetProfilePictureResponse {
    private ProfileImageRecord representativeProfileImage;
    private int profileImageCount;
    private List<ProfileImageRecord> profileImages;

    public static GetProfilePictureResponse from(ProfileImageRecord representativeProfileImage, List<ProfileImageRecord> profileImages) {
        return GetProfilePictureResponse.builder()
                .representativeProfileImage(representativeProfileImage)
                .profileImageCount(profileImages.size())
                .profileImages(profileImages)
                .build();
    }
}