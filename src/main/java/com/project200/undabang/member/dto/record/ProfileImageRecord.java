package com.project200.undabang.member.dto.record;

import com.project200.undabang.common.entity.Picture;
import lombok.Builder;

@Builder
public record ProfileImageRecord(long profileImageId, String profileImageUrl, String profileImageName,
                                 String profileImageExtension) {

    public static ProfileImageRecord from(Picture picture) {
        return ProfileImageRecord.builder()
                .profileImageId(picture.getId())
                .profileImageUrl(picture.getPictureUrl())
                .profileImageName(picture.getPictureName())
                .profileImageExtension(picture.getPictureExtension())
                .build();
    }
}