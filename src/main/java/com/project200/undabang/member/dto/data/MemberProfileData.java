package com.project200.undabang.member.dto.data;

import com.project200.undabang.member.enums.MemberGender;

public record MemberProfileData(
        String profileThumbnailUrl,
        String profileImageUrl,
        String nickname,
        MemberGender gender,
        String birthDate,
        String bio,
        int exerciseScore
) {
}
