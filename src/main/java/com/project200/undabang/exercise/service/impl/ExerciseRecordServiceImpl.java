package com.project200.undabang.exercise.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordResponseDto;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.exercise.service.ExerciseRecordService;
import com.project200.undabang.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

/**
 * 운동 기록 조회 관련 서비스 구현체입니다.
 * 사용자의 운동 기록을 조회하고 접근 권한을 검증하는 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
public class ExerciseRecordServiceImpl implements ExerciseRecordService {
    private final ExerciseRepository exerciseRepository;
    private final MemberRepository memberRepository;
    /**
     * 회원 ID가 유효한지 확인합니다.
     */
    @Override
    public boolean checkMemberId(UUID memberId) {
        return memberRepository.existsByMemberId(memberId);
    }
    /**
     * 운동 기록 ID가 유효한지 확인합니다.
     */
    @Override
    public boolean checkExerciseRecordId(Long recordId) {
        return exerciseRepository.existsById(recordId);
    }
    /**
     * 특정 회원이 특정 운동 기록에 접근할 권한이 있는지 확인합니다.
     * 운동 기록의 소유자인 경우에만 접근 권한이 있습니다.
     */
    @Override
    public boolean validateMemberRecordAccess(UUID memberId, Long recordId) {
        return exerciseRepository.existsByRecordIdAndMemberId(memberId, recordId);
    }

    /**
     * 특정 운동 기록의 상세 정보를 조회합니다.
     * 현재 인증된 사용자가 해당 운동 기록에 접근 권한이 있는지 검증합니다.
     */

    @Override
    public FindExerciseRecordResponseDto findExerciseRecordByRecordId(Long recordId) {
        if(!checkMemberId(UserContextHolder.getUserId())){
            throw new CustomException(ErrorCode.USER_ID_HEADER_MISSING);
        }
        if(!checkExerciseRecordId(recordId)){
            throw new CustomException(ErrorCode.EXERCISE_RECORD_NOT_FOUND);
        }
        if(!validateMemberRecordAccess(UserContextHolder.getUserId(), recordId)){
            throw new CustomException(ErrorCode.AUTHORIZATION_DENIED);
        }

        UUID memberId = UserContextHolder.getUserId();
        FindExerciseRecordResponseDto responseDto = exerciseRepository.findExerciseByExerciseId(memberId, recordId);

        if(Objects.isNull(responseDto)){
            // 혹시 DB에서 어떤 이유로 조회 실패한 경우 에러 발생
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return responseDto;
    }
}

