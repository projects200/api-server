package com.project200.undabang.member.dto.record;

import com.project200.undabang.common.entity.Picture;
import lombok.Builder;

@Builder
public record ProfileImageRecord(long profileImageId, String profileImageUrl, String profileImageName,
                                 String profileImageExtension) {

    /**
     * 주어진 Picture 객체를 기반으로 ProfileImageRecord 객체를 생성합니다.
     */
    public static ProfileImageRecord from(Picture picture) {
        return ProfileImageRecord.builder()
                .profileImageId(picture.getId())
                .profileImageUrl(picture.getPictureUrl())
                .profileImageName(picture.getPictureName())
                .profileImageExtension(picture.getPictureExtension())
                .build();
    }
}