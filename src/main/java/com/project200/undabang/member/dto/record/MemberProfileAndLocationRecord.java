package com.project200.undabang.member.dto.record;

import com.project200.undabang.member.enums.MemberGender;
import org.locationtech.jts.geom.Point;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 회원의 프로필 정보와 운동 위치 정보를 나타내는 레코드입니다.
 * 주로 회원의 프로필 및 운동 위치와 관련된 데이터를 동시에 처리할 때 사용됩니다.
 */
public record MemberProfileAndLocationRecord(
        UUID memberId,
        String nickname,
        MemberGender gender,
        LocalDate birthDate,
        String profileThumbnailUrl,
        String exerciseLocationName,
        Point locationPoint
) {
}
