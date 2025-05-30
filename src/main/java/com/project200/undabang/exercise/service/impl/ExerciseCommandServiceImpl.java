package com.project200.undabang.exercise.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
import com.project200.undabang.exercise.dto.request.UpdateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.ExerciseIdResponseDto;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.exercise.service.ExerciseCommandService;
import com.project200.undabang.exercise.service.ExercisePictureService;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ExerciseCommandServiceImpl implements ExerciseCommandService {

    private final ExercisePictureService exercisePictureService;

    private final ExerciseRepository exerciseRepository;
    private final MemberRepository memberRepository;

    @Override
    public ExerciseIdResponseDto createExercise(CreateExerciseRequestDto requestDto) {
        Member member = findMember();

        Exercise exercise = requestDto.toEntity(member);

        exerciseRepository.save(exercise);

        return new ExerciseIdResponseDto(exercise.getId());
    }

    @Override
    public ExerciseIdResponseDto uploadExerciseImages(Long exerciseId, List<MultipartFile> exercisePictureList) {
        return exercisePictureService.uploadExercisePictures(exerciseId, exercisePictureList);
    }

    // 회원 정보를 조회하고 검증하는 메서드
    private Member findMember() {
        return memberRepository.findById(UserContextHolder.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    // 운동 기록 (문자데이터) 수정 메서드
    @Transactional
    @Override
    public ExerciseIdResponseDto updateExercise(Long exerciseId, UpdateExerciseRequestDto requestDto) {
        Member member = findMember();
        checkStartEndDate(requestDto.getExerciseStartedAt(), requestDto.getExerciseEndedAt());
        checkMemberIdAndExerciseId(member.getMemberId(), exerciseId);

        Exercise exercise = exerciseRepository.findById(exerciseId).orElseThrow(() -> new CustomException(ErrorCode.EXERCISE_RECORD_NOT_FOUND));

        // 더티 체킹 적용
        exercise.updateExercise(
                requestDto.getExerciseTitle(),
                requestDto.getExerciseDetail(),
                requestDto.getExercisePersonalType(),
                requestDto.getExerciseLocation(),
                requestDto.getExerciseStartedAt(),
                requestDto.getExerciseEndedAt()
        );

        return new ExerciseIdResponseDto(exerciseId);
    }

    @Override
    @Transactional
    public void deleteExercise(Long exerciseId){
        Member member = findMember();
        checkMemberIdAndExerciseId(member.getMemberId(), exerciseId);

        List<Long> exercisePictureIdList = exercisePictureService.getAllImagesFromExercise(member.getMemberId(), exerciseId);

        // 지울 사진이 있는 경우
        if(!Objects.isNull(exercisePictureIdList)){
            // 이미지 제거
            exercisePictureService.deleteExercisePictures(member.getMemberId(), exerciseId, exercisePictureIdList);
        }

        // 데이터베이스 제거
        Exercise exercise = exerciseRepository.findById(exerciseId).orElseThrow(() -> new CustomException(ErrorCode.EXERCISE_RECORD_NOT_FOUND));
        // 더티 체킹 적용
        exercise.deleteExercise();
    }

    @Override
    @Transactional
    public void deleteImages(Long exerciseId, List<Long> pictureIds) {
        Member member = findMember();
        checkMemberIdAndExerciseId(member.getMemberId(), exerciseId);

        // 이미지 제거
        exercisePictureService.deleteExercisePictures(member.getMemberId(), exerciseId, pictureIds);
    }

    private void checkStartEndDate(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate.isBefore(LocalDate.of(1945, 8, 15).atStartOfDay()) ||
                endDate.isAfter(LocalDate.now().plusDays(1).atStartOfDay())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    /**
     * 타인의 운동기록 혹은 사진 접근시 ACCESS DENIED 반환하도록 체크
     */
    private void checkMemberIdAndExerciseId(UUID memberId, Long exerciseId) {
        if (!exerciseRepository.existsByRecordIdAndMemberId(memberId, exerciseId)) {
            throw new CustomException(ErrorCode.AUTHORIZATION_DENIED);
        }
    }
}
