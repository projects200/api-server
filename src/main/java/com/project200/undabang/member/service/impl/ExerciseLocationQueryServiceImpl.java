package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
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

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseLocationQueryServiceImpl implements ExerciseLocationQueryService {
    private final ExerciseLocationRepository exerciseLocationRepository;
    private final MemberRepository memberRepository;

    /**
     * 회원들의 운동 위치 정보를 조회하여 반환합니다.
     *
     * @return 회원들의 운동 위치 정보를 담고 있는 GetMembersExerciseLocationsResponse 객체 리스트
     */
    @Override
    public List<GetMembersExerciseLocationsResponse> getMembersExerciseLocations() {

        return exerciseLocationRepository.getMembersExerciseLocations();
    }

    /**
     * 현재 사용자가 등록한 운동 위치 정보를 가져와 반환합니다.
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
     * 주어진 회원 ID에 해당하는 회원 정보를 가져옵니다.
     * 회원 정보가 존재하지 않을 경우 예외를 발생시킵니다.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
