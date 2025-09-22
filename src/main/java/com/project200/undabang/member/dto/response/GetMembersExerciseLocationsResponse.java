package com.project200.undabang.member.dto.response;

import com.project200.undabang.member.dto.record.ExerciseLocationRecord;
import com.project200.undabang.member.dto.record.MemberProfileAndLocationRecord;
import com.project200.undabang.member.enums.MemberGender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

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
    private String profileImageUrl;
    private String nickname;
    private MemberGender gender;
    private LocalDate birthDate;
    private List<ExerciseLocationRecord> locations;

    /**
     * 주어진 MemberProfileAndLocationRecord 목록에서 정보를 추출하여
     * GetMembersExerciseLocationsResponse 객체를 생성합니다.
     *
     * @param recordList MemberProfileAndLocationRecord 객체의 리스트
     * @return GetMembersExerciseLocationsResponse 객체
     */
    public static GetMembersExerciseLocationsResponse from(List<MemberProfileAndLocationRecord> recordList) {
        MemberProfileAndLocationRecord firstRecord = recordList.getFirst();

        List<ExerciseLocationRecord> locationRecordList = recordList.stream()
                .map(dto -> {
                    Point point = dto.locationPoint();
                    return new ExerciseLocationRecord(
                            dto.exerciseLocationName(),
                            point.getY(), // 위도
                            point.getX() // 경도
                    );
                })
                .toList();

        return GetMembersExerciseLocationsResponse.builder()
                .memberId(firstRecord.memberId())
                .profileImageUrl(firstRecord.profileThumbnailUrl())
                .nickname(firstRecord.nickname())
                .gender(firstRecord.gender())
                .birthDate(firstRecord.birthDate())
                .locations(locationRecordList)
                .build();
    }
}
