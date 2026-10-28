package com.project200.undabang.exercise.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
import com.project200.undabang.exercise.dto.request.UpdateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.CreateExerciseResponseDto;
import com.project200.undabang.exercise.dto.response.ExerciseIdResponseDto;
import com.project200.undabang.exercise.dto.response.ExercisePicturesResponse;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.exercise.service.ExerciseCommandService;
import com.project200.undabang.exercise.service.ExercisePictureService;
import com.project200.undabang.exercise.sync.ExerciseOperation;
import com.project200.undabang.exercise.sync.ExerciseSyncConflictException;
import com.project200.undabang.exercise.sync.ExerciseSyncService;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.score.service.ExerciseScoreCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ExerciseCommandServiceImpl implements ExerciseCommandService {
    private final ExercisePictureService exercisePictureService;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseScoreCommandService exerciseScoreCommandService;
    private final ExerciseSyncService syncService;

    @Override
    @Transactional
    public CreateExerciseResponseDto createExercise(CreateExerciseRequestDto request) {
        Member member = syncService.lockMember(UserContextHolder.getUserId());
        String fingerprint = request.getCreateRequestId() == null ? null : syncService.fingerprint(
                request.getExerciseTitle(), request.getExerciseDetail(), request.getExercisePersonalType(),
                request.getExerciseLocation(), request.getExerciseStartedAt(), request.getExerciseEndedAt());
        var replay = syncService.replay(member, request.getCreateRequestId(), ExerciseOperation.CREATE,
                null, fingerprint, CreateExerciseResponseDto.class);
        if (replay.isPresent()) {
            return replay.get();
        }
        Exercise exercise = request.toEntity(member);
        byte earnedPoints;
        if (request.getCreateRequestId() != null) {
            // 새 행이 일일 횟수 조회에 포함되기 전에 계산합니다. 점수 오류도 함께 롤백합니다.
            earnedPoints = exerciseScoreCommandService.awardPointsAtomically(exercise);
            exerciseRepository.save(exercise);
        } else {
            // 배포된 요청의 점수 실패 시 기록 보존 계약을 유지합니다.
            exerciseRepository.save(exercise);
            earnedPoints = exerciseScoreCommandService.awardPointsForExercise(exercise);
        }
        CreateExerciseResponseDto response = new CreateExerciseResponseDto(exercise.getId(), earnedPoints, exercise.getVersion());
        syncService.record(member, request.getCreateRequestId(), ExerciseOperation.CREATE, null, fingerprint, response);
        return response;
    }

    @Override
    public ExercisePicturesResponse uploadExerciseImages(Long exerciseId, List<MultipartFile> files,
                                                         UUID uploadRequestId, List<UUID> clientImageIds) {
        return exercisePictureService.uploadExercisePictures(exerciseId, files, uploadRequestId, clientImageIds);
    }

    @Override
    @Transactional
    public ExerciseIdResponseDto updateExercise(Long exerciseId, UpdateExerciseRequestDto request) {
        ExerciseSyncService.validateEdit(request.getEditId(), request.getBaseVersion());
        Member member = syncService.lockMember(UserContextHolder.getUserId());
        String fingerprint = request.getEditId() == null ? null : syncService.fingerprint(
                request.getBaseVersion(), request.getExerciseTitle(), request.getExerciseDetail(),
                request.getExercisePersonalType(), request.getExerciseLocation(),
                request.getExerciseStartedAt(), request.getExerciseEndedAt());
        var replay = syncService.replay(member, request.getEditId(), ExerciseOperation.UPDATE,
                exerciseId, fingerprint, ExerciseIdResponseDto.class);
        if (replay.isPresent()) {
            return replay.get();
        }
        Exercise exercise = syncService.ownedExercise(member.getMemberId(), exerciseId);
        syncService.requireActive(exercise);
        if (request.getBaseVersion() != null && request.getBaseVersion() != exercise.getVersion()) {
            throw new ExerciseSyncConflictException(exerciseId, exercise.getVersion());
        }
        checkStartEndDate(request.getExerciseStartedAt(), request.getExerciseEndedAt());
        exercise.updateExercise(request.getExerciseTitle(), request.getExerciseDetail(), request.getExercisePersonalType(),
                request.getExerciseLocation(), request.getExerciseStartedAt(), request.getExerciseEndedAt());
        ExerciseIdResponseDto response = new ExerciseIdResponseDto(exerciseId, exercise.getVersion());
        syncService.record(member, request.getEditId(), ExerciseOperation.UPDATE, exerciseId, fingerprint, response);
        return response;
    }

    @Override
    @Transactional
    public ExerciseIdResponseDto deleteExercise(Long exerciseId, UUID editId) {
        Member member = syncService.lockMember(UserContextHolder.getUserId());
        String fingerprint = editId == null ? null : syncService.fingerprint(exerciseId);
        var replay = syncService.replay(member, editId, ExerciseOperation.DELETE, exerciseId, fingerprint, ExerciseIdResponseDto.class);
        if (replay.isPresent()) {
            return replay.get();
        }
        Exercise exercise = syncService.ownedExercise(member.getMemberId(), exerciseId);
        if (exercise.getExerciseDeletedAt() == null) {
            List<Long> pictureIds = exercisePictureService.getAllImagesFromExercise(member.getMemberId(), exerciseId);
            exercisePictureService.deleteExercisePictures(member.getMemberId(), exerciseId, pictureIds);
            exercise.deleteExercise();
        }
        ExerciseIdResponseDto response = new ExerciseIdResponseDto(exerciseId, exercise.getVersion());
        syncService.record(member, editId, ExerciseOperation.DELETE, exerciseId, fingerprint, response);
        return response;
    }

    @Override
    @Transactional
    public ExerciseIdResponseDto deleteImages(Long exerciseId, List<Long> pictureIds, UUID editId) {
        if (pictureIds == null || pictureIds.isEmpty() || pictureIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Member member = syncService.lockMember(UserContextHolder.getUserId());
        List<Long> normalizedIds = pictureIds.stream().distinct().sorted().toList();
        String fingerprint = editId == null ? null : syncService.fingerprint(normalizedIds);
        var replay = syncService.replay(member, editId, ExerciseOperation.DELETE_PICTURES,
                exerciseId, fingerprint, ExerciseIdResponseDto.class);
        if (replay.isPresent()) {
            return replay.get();
        }
        Exercise exercise = syncService.ownedExercise(member.getMemberId(), exerciseId);
        if (exercisePictureService.deleteExercisePictures(member.getMemberId(), exerciseId, normalizedIds)) {
            exercise.incrementVersion();
        }
        ExerciseIdResponseDto response = new ExerciseIdResponseDto(exerciseId, exercise.getVersion());
        syncService.record(member, editId, ExerciseOperation.DELETE_PICTURES, exerciseId, fingerprint, response);
        return response;
    }

    private void checkStartEndDate(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate.isBefore(LocalDate.of(1945, 8, 15).atStartOfDay())
                || endDate.isAfter(LocalDate.now().plusDays(1).atStartOfDay())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
