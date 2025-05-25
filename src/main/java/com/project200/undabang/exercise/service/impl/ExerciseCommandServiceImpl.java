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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    @Override
    public ExerciseIdResponseDto updateExercise(Long exerciseId, UpdateExerciseRequestDto requestDto) {
        return null;
    }

    @Override
    public void deleteExercise(Long exerciseId) {

    }

    @Override
    public void deleteExerciseImages(Long exerciseId, List<Long> pictureIds) {

    }
}
