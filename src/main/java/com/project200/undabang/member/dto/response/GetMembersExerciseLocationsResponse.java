package com.project200.undabang.member.dto.response;

import com.project200.undabang.member.dto.record.ExerciseLocationRecord;
import com.project200.undabang.member.enums.MemberGender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * GetMembersExerciseLocationsResponse 클래스는 회원의 프로필 정보와 운동 위치 정보를 나타냅니다.
 * 이 클래스는 특정 회원의 프로필 정보와 운동 기록을 통합적으로 제공하기 위한 DTO(Data Transfer Object)로 사용됩니다.
 * 주요 필드로는 회원의 고유 ID, 프로필 이미지 URL, 닉네임, 성별, 생년월일, 운동 위치 정보 리스트 등이 포함됩니다.
 * 이 클래스는 주로 주어진 데이터에서 객체를 생성하여 응답 데이터로 사용됩니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetMembersExerciseLocationsResponse {
    private UUID memberId;
    private String profileThumbnailUrl;
    private String profileImageUrl;
    private String nickname;
    private MemberGender gender;
    private LocalDate birthDate;
    private List<ExerciseLocationRecord> locations;
}