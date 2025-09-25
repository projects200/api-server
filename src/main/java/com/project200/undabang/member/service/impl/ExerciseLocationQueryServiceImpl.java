package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.record.MemberProfileAndLocationRecord;
import com.project200.undabang.member.dto.response.GetExerciseLocationsResponse;
import com.project200.undabang.member.dto.response.GetMembersExerciseLocationsResponse;
import com.project200.undabang.member.entity.ExerciseLocation;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.ExerciseLocationRepository;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.member.service.ExerciseLocationQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseLocationQueryServiceImpl implements ExerciseLocationQueryService {
    private final ExerciseLocationRepository exerciseLocationRepository;
    private final MemberRepository memberRepository;

    /**
     * 모든 회원의 운동 위치 정보를 가져와 반환합니다.
     * 이 메소드는 회원 ID를 기준으로 그룹화하고, 각 그룹화된 데이터를 처리하여
     * GetMembersExerciseLocationsResponse 객체의 리스트로 변환합니다.
     *
     * @return 회원 운동 위치 및 관련 정보를 포함하는 GetMembersExerciseLocationsResponse 객체의 리스트
     */
    @Override
    public List<GetMembersExerciseLocationsResponse> getMembersExerciseLocations() {

        List<MemberProfileAndLocationRecord> memberProfileAndLocationList = exerciseLocationRepository.getMembersExerciseLocations();

        Map<UUID, List<MemberProfileAndLocationRecord>> memberProfileAndLocationMap = groupMemberLocationRecordByUsingMap(memberProfileAndLocationList);

        return memberProfileAndLocationMap.values().stream()
                .map(GetMembersExerciseLocationsResponse::from)
                .toList();
    }

    /**
     * 현재 사용자와 관련된 운동 위치 정보를 가져와 반환합니다.
     * 이 정보는 삭제되지 않은 운동 위치를 대상으로 하며, 각 위치는
     * GetExerciseLocationsResponse 객체로 매핑됩니다.
     *
     * @return 현재 사용자와 관련된 운동 위치 정보를 포함하는 GetExerciseLocationsResponse 객체의 리스트
     */
    @Override
    public List<GetExerciseLocationsResponse> getExerciseLocations() {
        Member member = getMember(UserContextHolder.getUserId());

        List<ExerciseLocation> exerciseLocationList = exerciseLocationRepository.findAllByMemberAndExerciseLocationDeletedAtNull(member);

        return exerciseLocationList.stream()
                .map(GetExerciseLocationsResponse::from)
                .toList();
    }

    /**
     * 회원의 프로필 및 위치 정보 리스트를 회원 ID별로 그룹핑하여 Map으로 반환합니다.
     *
     * @param memberProfileAndLocationList 그룹핑되지 않은 전체 DTO 리스트
     * @return 회원 ID가 Key이고, 해당 회원의 DTO 리스트가 Value인 Map
     */
    private Map<UUID, List<MemberProfileAndLocationRecord>> groupMemberLocationRecordByUsingMap(List<MemberProfileAndLocationRecord> memberProfileAndLocationList) {
        Map<UUID, List<MemberProfileAndLocationRecord>> groupedMemberLocationsMap = new HashMap<>();

        for (MemberProfileAndLocationRecord profileAndLocationRecord : memberProfileAndLocationList) {
            UUID memberId = profileAndLocationRecord.memberId();
            // memberId가 map 에 없으면 ArrayList를 생성하고, Map에 추가한다. 그 후, record를 Map에 추가함
            groupedMemberLocationsMap.computeIfAbsent(memberId, key -> new ArrayList<>()).add(profileAndLocationRecord);
        }

        return groupedMemberLocationsMap;
    }

    /**
     * 주어진 회원 ID에 해당하는 회원 정보를 가져옵니다.
     * 회원 정보가 존재하지 않을 경우 예외를 발생시킵니다.
     *
     * @param memberId 회원의 고유 식별자(UUID)
     * @return 주어진 ID에 해당하는 회원 엔티티
     * @throws CustomException ErrorCode.MEMBER_NOT_FOUND가 포함된 예외가 발생할 수 있습니다.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
