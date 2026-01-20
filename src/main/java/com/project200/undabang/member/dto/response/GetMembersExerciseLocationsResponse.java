package com.project200.undabang.member.dto.response;

import com.project200.undabang.member.dto.record.ExerciseLocationRecord;
import com.project200.undabang.member.dto.record.PreferredExerciseRecord;
import com.project200.undabang.member.enums.MemberGender;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 회원의 운동 위치 및 선호 운동 데이터를 응답하기 위한 DTO 클래스입니다.
 * QueryDSL에서 Projections.constructor를 사용하여 데이터베이스 조회 결과를 매핑하는 데 활용됩니다.
 *
 * 주요 필드:
 * - 회원 정보: 회원 ID, 프로필 이미지, 닉네임, 성별, 생년월일, 회원 점수
 * - 운동 정보: 운동 장소 목록(locations), 선호 운동 목록(preferredExercises)
 *
 * 생성자 설명:
 * - QueryDSL에서 데이터를 가져올 때 Left Join으로 인해 발생할 수 있는 null 값의
 *   컬렉션을 빈 Set으로 초기화하여 NullPointerException을 방지합니다.
 * - 조인된 데이터가 없는 경우 모든 필드가 null인 Ghost Object를 필터링하여 유효한 데이터만 포함합니다.
 */
@Getter
@Builder
@NoArgsConstructor
public class GetMembersExerciseLocationsResponse {
    private UUID memberId;
    private String profileThumbnailUrl;
    private String profileImageUrl;
    private String nickname;
    private MemberGender gender;
    private LocalDate birthDate;
    private Byte memberScore;
    private Set<ExerciseLocationRecord> locations;
    private Set<PreferredExerciseRecord> preferredExercises;

    /**
     * QueryDSL 프로젝션(Projections.constructor)을 위해 사용되는 생성자입니다.
     * DB 조회 시 Left Join으로 인해 발생할 수 있는 null 컬렉션을 빈 Set으로 안전하게 초기화하며,
     * 조인된 데이터가 없어 모든 필드가 null인 Ghost Object를 필터링하여 유효한 데이터만 포함하도록 합니다.
     */
    public GetMembersExerciseLocationsResponse(
            UUID memberId,
            String profileThumbnailUrl,
            String profileImageUrl,
            String nickname,
            MemberGender gender,
            LocalDate birthDate,
            Byte memberScore,
            Set<ExerciseLocationRecord> locations,
            Set<PreferredExerciseRecord> preferredExercises) {

        this.memberId = memberId;
        this.profileThumbnailUrl = profileThumbnailUrl;
        this.profileImageUrl = profileImageUrl;
        this.nickname = nickname;
        this.gender = gender;
        this.birthDate = birthDate;
        this.memberScore = memberScore;
        this.locations = Optional.ofNullable(locations)
                .orElse(Collections.emptySet()) // null이면 빈 Set 반환
                .stream()
                .filter(loc -> loc.exerciseLocationName() != null)
                .collect(Collectors.toSet());
        this.preferredExercises = Optional.ofNullable(preferredExercises)
                .orElse(Collections.emptySet())
                .stream()
                .filter(pe -> pe.preferredExerciseId() != null)
                .collect(Collectors.toSet());
    }
}