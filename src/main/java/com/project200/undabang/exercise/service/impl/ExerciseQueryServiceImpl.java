package com.project200.undabang.exercise.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordByPeriodResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordDateResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordResponseDto;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.exercise.service.ExerciseQueryService;
import com.project200.undabang.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 운동 기록 조회 관련 서비스 구현체입니다.
 * 사용자의 운동 기록을 조회하고 접근 권한을 검증하는 기능을 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseQueryServiceImpl implements ExerciseQueryService {
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
        UUID memberId = UserContextHolder.getUserId();

        if(!checkMemberId(memberId)){
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
        if(!checkExerciseRecordId(recordId)){
            throw new CustomException(ErrorCode.EXERCISE_RECORD_NOT_FOUND);
        }
        if(!validateMemberRecordAccess(memberId, recordId)){
            throw new CustomException(ErrorCode.AUTHORIZATION_DENIED);
        }

        log.debug("운동기록 조회 요청 : {}", recordId);
        FindExerciseRecordResponseDto responseDto = exerciseRepository.findExerciseByExerciseId(memberId, recordId);

        if(Objects.isNull(responseDto)){
            // 혹시 DB에서 어떤 이유로 조회 실패한 경우 에러 발생
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return responseDto;
    }

    /**
     * 특정 날짜에 해당하는 운동 기록 목록을 조회합니다.
     */

    @Override
    public Optional<List<FindExerciseRecordDateResponseDto>>  findExerciseRecordByDate(LocalDate inputDate) {
        if(inputDate.isBefore(LocalDate.of(1945, 8, 15)) || inputDate.isAfter(LocalDate.now())){
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        UUID memberId = UserContextHolder.getUserId();

        log.debug("날짜별 운동 기록 조회 요청: date={}", inputDate);
        Optional<List<FindExerciseRecordDateResponseDto>> responseDtoList = exerciseRepository.findExerciseRecordByDate(memberId, inputDate);
//        log.debug("날짜별 운동 기록 조회 결과: date={}, count={}", inputDate, responseDtoList.map(List::size).orElse(0));

        return responseDtoList;
    }

    /**
     * 특정 기간 동안의 날짜별 운동 기록 개수를 조회합니다.
     */

    @Override
    public List<FindExerciseRecordByPeriodResponseDto> findExerciseRecordsByPeriod(LocalDate startDate, LocalDate endDate) {
        if(startDate.isBefore(LocalDate.of(1945,8,15)) || endDate.isAfter(LocalDate.now())){
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if(startDate.isAfter(endDate)){
            throw new CustomException(ErrorCode.IMPOSSIBLE_INPUT_DATE);
        }

        UUID memberId = UserContextHolder.getUserId();
        List<FindExerciseRecordByPeriodResponseDto> responseDto = exerciseRepository.findExercisesByPeriod(memberId, startDate, endDate);

        return responseDto;
    }
}

