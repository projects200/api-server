package com.project200.undabang.exercise.sync;

import com.project200.undabang.exercise.sync.repository.ExercisePhotoCleanupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExercisePhotoCleanupScheduler {
    private final ExercisePhotoCleanupRepository cleanupRepository;
    private final ExercisePhotoCleanupWorker cleanupWorker;

    @Scheduled(fixedDelayString = "${exercise.photo-cleanup-delay-ms:60000}")
    public void retryPending() {
        cleanupRepository.findTop50ByNextAttemptAtLessThanEqualOrderById(LocalDateTime.now())
                .forEach(task -> {
                    try {
                        cleanupWorker.process(task.getId());
                    } catch (RuntimeException ex) {
                        log.warn("운동 사진 정리 트랜잭션 실패: taskId={}", task.getId());
                    }
                });
    }
}
