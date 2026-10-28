package com.project200.undabang.exercise.sync;

import com.project200.undabang.common.service.S3Service;
import com.project200.undabang.exercise.sync.repository.ExercisePhotoCleanupRepository;
import com.project200.undabang.exercise.sync.repository.ExerciseReceiptRepository;
import com.project200.undabang.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExercisePhotoCleanupWorker {
    private final ExercisePhotoCleanupRepository cleanupRepository;
    private final ExerciseReceiptRepository receiptRepository;
    private final MemberRepository memberRepository;
    private final S3Service s3Service;

    /** 커밋된 삭제 작업만 처리하며 실패한 작업을 영속 상태로 남깁니다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long id) {
        cleanupRepository.findByIdForUpdate(id).ifPresent(task -> {
            try {
                s3Service.deleteImage(task.getObjectKey());
                cleanupRepository.delete(task);
            } catch (RuntimeException ex) {
                task.failed();
                log.warn("운동 사진 정리 실패: taskId={}", id);
            }
        });
    }

    /** 롤백 뒤 재시도 업로드와 같은 회원 잠금으로 직렬화합니다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rollbackUpload(UUID memberId, UUID requestId, List<String> objectKeys) {
        memberRepository.findByIdForUpdate(memberId);
        if (requestId != null && receiptRepository.findByMember_MemberIdAndRequestId(memberId, requestId)
                .filter(receipt -> receipt.getOperation() == ExerciseOperation.UPLOAD_PICTURES).isPresent()) {
            return;
        }
        for (String objectKey : objectKeys) {
            try {
                s3Service.deleteImage(objectKey);
            } catch (RuntimeException ex) {
                // 업로드 전 프로세스 종료와 같은 고아 객체 한계는 API 운영 계약에 명시합니다.
                log.warn("롤백된 운동 사진의 객체 정리에 실패했습니다.");
            }
        }
    }
}
