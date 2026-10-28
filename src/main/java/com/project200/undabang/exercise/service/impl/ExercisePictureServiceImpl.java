package com.project200.undabang.exercise.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.repository.PictureRepository;
import com.project200.undabang.common.service.FileType;
import com.project200.undabang.common.service.S3Service;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.dto.response.ExercisePicturesResponse;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.entity.ExercisePicture;
import com.project200.undabang.exercise.repository.ExercisePictureRepository;
import com.project200.undabang.exercise.service.ExercisePictureService;
import com.project200.undabang.exercise.sync.ExerciseOperation;
import com.project200.undabang.exercise.sync.ExercisePhotoCleanupWorker;
import com.project200.undabang.exercise.sync.ExerciseSyncService;
import com.project200.undabang.exercise.sync.entity.ExercisePhotoCleanup;
import com.project200.undabang.exercise.sync.repository.ExercisePhotoCleanupRepository;
import com.project200.undabang.member.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class ExercisePictureServiceImpl implements ExercisePictureService {
    private final PictureRepository pictureRepository;
    private final ExercisePictureRepository exercisePictureRepository;
    private final S3Service s3Service;
    private final ExerciseSyncService syncService;
    private final ExercisePhotoCleanupRepository cleanupRepository;
    private final ExercisePhotoCleanupWorker cleanupWorker;

    @Override
    @Transactional
    public ExercisePicturesResponse uploadExercisePictures(Long exerciseId, List<MultipartFile> files,
                                                           UUID uploadRequestId, List<UUID> clientImageIds) {
        ExerciseSyncService.validatePictures(files, uploadRequestId, clientImageIds);
        Member member = syncService.lockMember(UserContextHolder.getUserId());
        String fingerprint = uploadRequestId == null ? null : syncService.pictureFingerprint(files, clientImageIds);
        var replay = syncService.replay(member, uploadRequestId, ExerciseOperation.UPLOAD_PICTURES,
                exerciseId, fingerprint, ExercisePicturesResponse.class);
        if (replay.isPresent()) {
            return replay.get();
        }
        Exercise exercise = syncService.ownedExercise(member.getMemberId(), exerciseId);
        syncService.requireActive(exercise);
        if (exercisePictureRepository.countNotDeletedPicturesByExerciseId(exerciseId) + files.size() > 5) {
            throw new CustomException(ErrorCode.EXERCISE_PICTURE_COUNT_EXCEEDED);
        }
        List<String> attemptedKeys = new ArrayList<>();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK && !attemptedKeys.isEmpty()) {
                    try {
                        cleanupWorker.rollbackUpload(member.getMemberId(), uploadRequestId, List.copyOf(attemptedKeys));
                    } catch (RuntimeException ex) {
                        log.warn("롤백된 운동 사진의 정리 트랜잭션에 실패했습니다.");
                    }
                }
            }
        });
        List<ExercisePicturesResponse.UploadedPicture> mappings = uploadRequestId == null ? null : new ArrayList<>(files.size());
        try {
            for (int index = 0; index < files.size(); index++) {
                MultipartFile file = files.get(index);
                UUID clientImageId = clientImageIds == null ? null : clientImageIds.get(index);
                String objectKey = uploadRequestId == null
                        ? s3Service.generateObjectKey(file.getOriginalFilename(), FileType.EXERCISE)
                        : "uploads/exercise/" + member.getMemberId() + "/" + uploadRequestId + "/" + clientImageId;
                // PUT 성공 직후 응답이 유실된 경우도 정리 대상에 포함합니다.
                attemptedKeys.add(objectKey);
                String url = s3Service.uploadImage(file, objectKey);
                Picture picture = pictureRepository.save(Picture.of(file, url));
                exercisePictureRepository.save(ExercisePicture.builder().exercise(exercise).picture(picture).build());
                if (mappings != null) {
                    mappings.add(new ExercisePicturesResponse.UploadedPicture(clientImageId, picture.getId(), url));
                }
            }
        } catch (RuntimeException ex) {
            throw new CustomException(ErrorCode.EXERCISE_PICTURE_UPLOAD_FAILED);
        }
        exercise.incrementVersion();
        ExercisePicturesResponse response = new ExercisePicturesResponse(exerciseId, exercise.getVersion(),
                uploadRequestId == null ? null : List.copyOf(mappings));
        syncService.record(member, uploadRequestId, ExerciseOperation.UPLOAD_PICTURES, exerciseId, fingerprint, response);
        return response;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Long> getAllImagesFromExercise(UUID memberId, Long exerciseId) {
        syncService.ownedExercise(memberId, exerciseId);
        return exercisePictureRepository.findAllByExercise_Id(exerciseId).stream().map(ExercisePicture::getId).toList();
    }

    /** 호출자가 회원 잠금을 보유합니다. 삭제 작업은 DB 커밋 후에만 S3에 반영합니다. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean deleteExercisePictures(UUID memberId, Long exerciseId, List<Long> pictureIds) {
        syncService.ownedExercise(memberId, exerciseId);
        List<ExercisePicture> pictures = exercisePictureRepository.findAllByExercise_Id(exerciseId);
        Set<Long> actualIds = new HashSet<>(pictures.stream().map(ExercisePicture::getId).toList());
        if (!actualIds.containsAll(pictureIds)) {
            throw new CustomException(ErrorCode.AUTHORIZATION_DENIED);
        }
        boolean changed = false;
        for (ExercisePicture exercisePicture : pictures) {
            Picture picture = exercisePicture.getPicture();
            if (pictureIds.contains(picture.getId()) && picture.getPictureDeletedAt() == null) {
                String objectKey = s3Service.extractObjectKeyFromUrl(picture.getPictureUrl());
                ExercisePhotoCleanup task = cleanupRepository.save(ExercisePhotoCleanup.of(objectKey));
                picture.softDelete();
                changed = true;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            cleanupWorker.process(task.getId());
                        } catch (RuntimeException ex) {
                            log.warn("커밋된 운동 사진 정리는 스케줄러에서 재시도합니다: taskId={}", task.getId());
                        }
                    }
                });
            }
        }
        return changed;
    }
}
