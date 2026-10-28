package com.project200.undabang.exercise.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.exercise.sync.entity.ExerciseReceipt;
import com.project200.undabang.exercise.sync.repository.ExerciseReceiptRepository;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ExerciseSyncService {
    private final MemberRepository memberRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseReceiptRepository receiptRepository;
    private final ObjectMapper objectMapper;

    /** 모든 본문/사진 쓰기는 대상 및 영수증 조회 전에 회원 잠금을 획득합니다. */
    public Member lockMember(UUID memberId) {
        return memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    public Exercise ownedExercise(UUID memberId, Long exerciseId) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXERCISE_NOT_FOUND));
        if (!exercise.isOwnedBy(memberId)) {
            throw new CustomException(ErrorCode.AUTHORIZATION_DENIED);
        }
        return exercise;
    }

    public void requireActive(Exercise exercise) {
        if (exercise.getExerciseDeletedAt() != null) {
            throw new CustomException(ErrorCode.EXERCISE_DELETED);
        }
    }

    public <T> Optional<T> replay(Member member, UUID requestId, ExerciseOperation operation,
                                  Long exerciseId, String fingerprint, Class<T> responseType) {
        if (requestId == null) {
            return Optional.empty();
        }
        return receiptRepository.findByMember_MemberIdAndRequestId(member.getMemberId(), requestId)
                .map(receipt -> {
                    if (receipt.getOperation() != operation || !Objects.equals(receipt.getExerciseId(), exerciseId)
                            || !receipt.getFingerprint().equals(fingerprint)) {
                        throw new CustomException(ErrorCode.EXERCISE_REQUEST_ID_REUSED);
                    }
                    try {
                        return objectMapper.readValue(receipt.getResponseJson(), responseType);
                    } catch (JsonProcessingException ex) {
                        throw new IllegalStateException("운동 영수증 응답을 읽을 수 없습니다.", ex);
                    }
                });
    }

    public void record(Member member, UUID requestId, ExerciseOperation operation,
                       Long exerciseId, String fingerprint, Object response) {
        if (requestId != null) {
            receiptRepository.save(ExerciseReceipt.of(member, requestId, operation, exerciseId,
                    fingerprint, json(response)));
        }
    }

    /** 배열의 순서와 null을 보존하는 정규 입력만 전달합니다. */
    public String fingerprint(Object... fields) {
        return HexFormat.of().formatHex(digest().digest(json(fields).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    public String pictureFingerprint(List<MultipartFile> files, List<UUID> clientImageIds) {
        List<Object> fields = new ArrayList<>();
        for (int index = 0; index < files.size(); index++) {
            MultipartFile file = files.get(index);
            MessageDigest digest = digest();
            try (InputStream input = new DigestInputStream(file.getInputStream(), digest)) {
                input.transferTo(java.io.OutputStream.nullOutputStream());
            } catch (IOException ex) {
                throw new CustomException(ErrorCode.EXERCISE_PICTURE_UPLOAD_FAILED);
            }
            fields.add(Arrays.asList(clientImageIds.get(index), file.getOriginalFilename(), file.getContentType(),
                    file.getSize(), HexFormat.of().formatHex(digest.digest())));
        }
        return fingerprint(fields);
    }

    public static void validateEdit(UUID editId, Long baseVersion) {
        if ((editId == null) != (baseVersion == null) || (baseVersion != null && baseVersion < 0)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    public static void validatePictures(List<MultipartFile> files, UUID requestId, List<UUID> clientImageIds) {
        if (files == null || files.isEmpty() || files.size() > 5
                || (requestId == null) != (clientImageIds == null)
                || (clientImageIds != null && (clientImageIds.size() != files.size()
                || clientImageIds.stream().anyMatch(Objects::isNull) || new HashSet<>(clientImageIds).size() != clientImageIds.size()))) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("운동 동기화 데이터를 직렬화할 수 없습니다.", ex);
        }
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
