package com.project200.undabang.exercise.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project200.undabang.admin.component.NotifyErrorToAdmin;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.service.S3Service;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.exception.S3UploadFailedException;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
import com.project200.undabang.exercise.dto.request.UpdateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.CreateExerciseResponseDto;
import com.project200.undabang.exercise.dto.response.ExerciseIdResponseDto;
import com.project200.undabang.exercise.dto.response.ExercisePicturesResponse;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.exercise.service.ExerciseCommandService;
import com.project200.undabang.exercise.service.impl.ExerciseCommandServiceImpl;
import com.project200.undabang.exercise.service.impl.ExercisePictureServiceImpl;
import com.project200.undabang.exercise.sync.repository.ExercisePhotoCleanupRepository;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.entity.Policy;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import com.project200.undabang.score.service.impl.ExercisePolicyValidatorImpl;
import com.project200.undabang.score.service.impl.ExerciseScoreCommandServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@Import({TestQuerydslConfig.class, ExerciseSynchronizationIntegrationTest.JsonConfig.class,
        ExerciseCommandServiceImpl.class, ExercisePictureServiceImpl.class, ExerciseSyncService.class,
        ExerciseScoreCommandServiceImpl.class, ExercisePolicyValidatorImpl.class,
        ExercisePhotoCleanupWorker.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ExerciseSynchronizationIntegrationTest {
    @TestConfiguration
    static class JsonConfig {
        @Bean ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Autowired ExerciseCommandService service;
    @Autowired MemberRepository memberRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired ExercisePhotoCleanupRepository cleanupRepository;
    @Autowired ExercisePhotoCleanupWorker cleanupWorker;
    private ExercisePhotoCleanupScheduler cleanupScheduler;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean PolicyService policyService;
    @MockitoBean S3Service s3Service;
    @MockitoBean NotifyErrorToAdmin notifyErrorToAdmin;

    private TransactionTemplate transactions;
    private final List<UUID> memberIds = new ArrayList<>();
    private static final LocalDateTime START = LocalDate.now().minusDays(1).atTime(12, 0);

    @BeforeEach
    void setUp() {
        transactions = new TransactionTemplate(transactionManager);
        transactions.setTimeout(10);
        cleanupScheduler = new ExercisePhotoCleanupScheduler(cleanupRepository, cleanupWorker);
        when(policyService.getPolicy(PolicyKey.EXERCISE_RECORD_VALIDITY_PERIOD)).thenReturn(Policy.builder()
                .policyValue("30").policyUnit("DAYS").build());
        when(policyService.getPolicyValueAsInt(PolicyKey.EXERCISE_RECORD_MAX_PER_DAY)).thenReturn(2);
        when(policyService.getPolicyValueAsByte(PolicyKey.POINTS_PER_EXERCISE)).thenReturn((byte) 3);
        when(policyService.getPolicyValueAsByte(PolicyKey.EXERCISE_SCORE_MIN_POINTS)).thenReturn((byte) 0);
        when(policyService.getPolicyValueAsByte(PolicyKey.EXERCISE_SCORE_MAX_POINTS)).thenReturn((byte) 100);
        when(s3Service.uploadImage(any(), anyString())).thenAnswer(invocation -> "https://s3/" + invocation.getArgument(1));
        when(s3Service.extractObjectKeyFromUrl(anyString())).thenAnswer(invocation -> ((String) invocation.getArgument(0)).substring("https://s3/".length()));
        when(s3Service.generateObjectKey(anyString(), any())).thenAnswer(invocation ->
                "uploads/exercise/" + UserContextHolder.getUserId() + "/legacy/" + UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.reset();
        if (memberIds.isEmpty()) {
            return;
        }
        transaction(() -> {
            for (UUID id : memberIds) {
                em.createQuery("delete from ExercisePhotoCleanup t where t.objectKey like :prefix")
                        .setParameter("prefix", "uploads/exercise/" + id + "/%").executeUpdate();
            }
            List<Long> pictures = em.createQuery("select ep.picture.id from ExercisePicture ep where ep.exercise.member.memberId in :ids", Long.class)
                    .setParameter("ids", memberIds).getResultList();
            em.createQuery("delete from ExerciseReceipt r where r.member.memberId in :ids").setParameter("ids", memberIds).executeUpdate();
            em.createQuery("delete from ExercisePicture ep where ep.exercise.id in (select e.id from Exercise e where e.member.memberId in :ids)")
                    .setParameter("ids", memberIds).executeUpdate();
            if (!pictures.isEmpty()) {
                em.createQuery("delete from Picture p where p.id in :ids").setParameter("ids", pictures).executeUpdate();
            }
            em.createQuery("delete from Exercise e where e.member.memberId in :ids").setParameter("ids", memberIds).executeUpdate();
            em.createQuery("delete from Member m where m.memberId in :ids").setParameter("ids", memberIds).executeUpdate();
            return null;
        });
    }

    @Test
    void concurrent_same_create_commits_one_record_one_score_and_one_receipt() throws Exception {
        UUID member = member((byte) 0);
        CreateExerciseRequestDto request = create(UUID.randomUUID(), "생성", START);
        List<CreateExerciseResponseDto> results = concurrent(member, () -> service.createExercise(request), () -> service.createExercise(request));
        assertThat(results.getLast()).isEqualTo(results.getFirst());
        assertThat(results.getFirst().earnedPoints()).isEqualTo((byte) 3);
        assertThat(exerciseCount(member)).isEqualTo(1);
        assertThat(receiptCount(member)).isEqualTo(1);
        assertThat(score(member)).isEqualTo((byte) 3);
    }

    @Test
    void creation_replay_keeps_original_points_and_version_after_edit_and_delete() {
        UUID member = member((byte) 0);
        CreateExerciseRequestDto request = create(UUID.randomUUID(), "생성", START);
        CreateExerciseResponseDto original = command(member, () -> service.createExercise(request));
        command(member, () -> service.updateExercise(original.exerciseId(), update(UUID.randomUUID(), 0L, "수정")));
        command(member, () -> service.deleteExercise(original.exerciseId(), UUID.randomUUID()));
        assertThat(command(member, () -> service.createExercise(request))).isEqualTo(original);
        assertThat(snapshot(original.exerciseId()).deleted()).isTrue();
        assertThat(score(member)).isEqualTo((byte) 3);
        assertThat(exerciseCount(member)).isEqualTo(1);
    }

    @Test
    void changed_body_operation_or_target_reuses_no_request_key() {
        UUID member = member((byte) 0);
        UUID key = UUID.randomUUID();
        var first = command(member, () -> service.createExercise(create(key, "원본", START)));
        assertError(() -> command(member, () -> service.createExercise(create(key, "다른 본문", START))), ErrorCode.EXERCISE_REQUEST_ID_REUSED);
        var emptyDetail = CreateExerciseRequestDto.builder().createRequestId(key).exerciseTitle("원본")
                .exerciseDetail("").exerciseStartedAt(START).exerciseEndedAt(START.plusHours(1)).build();
        assertError(() -> command(member, () -> service.createExercise(emptyDetail)), ErrorCode.EXERCISE_REQUEST_ID_REUSED);
        assertError(() -> command(member, () -> service.deleteExercise(first.exerciseId(), key)), ErrorCode.EXERCISE_REQUEST_ID_REUSED);
        UUID edit = UUID.randomUUID();
        var request = update(edit, 0L, "수정");
        command(member, () -> service.updateExercise(first.exerciseId(), request));
        var second = command(member, () -> service.createExercise(create(UUID.randomUUID(), "다른 기록", START)));
        assertError(() -> command(member, () -> service.updateExercise(second.exerciseId(), request)), ErrorCode.EXERCISE_REQUEST_ID_REUSED);
        assertThat(snapshot(first.exerciseId()).title()).isEqualTo("수정");
    }

    @Test
    void edit_replay_precedes_version_and_tombstone_guards_and_stale_edit_reports_current_version() {
        UUID member = member((byte) 0);
        long id = command(member, () -> service.createExercise(create(UUID.randomUUID(), "원본", START))).exerciseId();
        var edit = update(UUID.randomUUID(), 0L, "첫 편집");
        var first = command(member, () -> service.updateExercise(id, edit));
        command(member, () -> service.updateExercise(id, update(null, null, "레거시 편집")));
        assertThat(command(member, () -> service.updateExercise(id, edit))).isEqualTo(first);
        assertThat(snapshot(id).title()).isEqualTo("레거시 편집");
        assertThatThrownBy(() -> command(member, () -> service.updateExercise(id, update(UUID.randomUUID(), 1L, "충돌"))))
                .isInstanceOf(ExerciseSyncConflictException.class).extracting("currentValue")
                .isEqualTo(new ExerciseIdResponseDto(id, 2));
        command(member, () -> service.deleteExercise(id, UUID.randomUUID()));
        assertThat(command(member, () -> service.updateExercise(id, edit))).isEqualTo(first);
        assertError(() -> command(member, () -> service.updateExercise(id, update(UUID.randomUUID(), 2L, "늦은 편집"))), ErrorCode.EXERCISE_DELETED);
    }

    @Test
    void delete_receipt_replays_and_an_already_deleted_owned_target_does_not_increment_again() {
        UUID member = member((byte) 0);
        long id = command(member, () -> service.createExercise(create(UUID.randomUUID(), "삭제", START))).exerciseId();
        UUID key = UUID.randomUUID();
        var first = command(member, () -> service.deleteExercise(id, key));
        assertThat(command(member, () -> service.deleteExercise(id, key))).isEqualTo(first);
        assertThat(command(member, () -> service.deleteExercise(id, UUID.randomUUID()))).isEqualTo(first);
        assertError(() -> command(member, () -> service.deleteExercise(Long.MAX_VALUE, UUID.randomUUID())), ErrorCode.EXERCISE_NOT_FOUND);
        assertThat(score(member)).isEqualTo((byte) 3);
    }

    @Test
    void another_member_can_use_same_key_but_never_access_the_first_members_record() {
        UUID firstMember = member((byte) 0);
        UUID secondMember = member((byte) 0);
        var request = create(UUID.randomUUID(), "동일 키", START);
        var first = command(firstMember, () -> service.createExercise(request));
        var second = command(secondMember, () -> service.createExercise(request));
        assertThat(second.exerciseId()).isNotEqualTo(first.exerciseId());
        assertError(() -> command(secondMember, () -> service.deleteExercise(first.exerciseId(), UUID.randomUUID())), ErrorCode.AUTHORIZATION_DENIED);
        assertThat(score(firstMember)).isEqualTo((byte) 3);
        assertThat(score(secondMember)).isEqualTo((byte) 3);
    }

    @Test
    void legacy_and_sync_edits_serialize_without_silent_sync_overwrite() throws Exception {
        UUID member = member((byte) 0);
        long id = command(member, () -> service.createExercise(create(UUID.randomUUID(), "원본", START))).exerciseId();
        List<Object> results = concurrent(member,
                () -> outcome(() -> service.updateExercise(id, update(null, null, "레거시"))),
                () -> outcome(() -> service.updateExercise(id, update(UUID.randomUUID(), 0L, "동기화"))));
        if (results.getLast() instanceof ExerciseSyncConflictException) {
            assertThat(snapshot(id)).isEqualTo(new Snapshot("레거시", 1, false));
        } else {
            assertThat(results.getLast()).isInstanceOf(ExerciseIdResponseDto.class);
            assertThat(snapshot(id)).isEqualTo(new Snapshot("레거시", 2, false));
        }
    }

    @Test
    void concurrent_delete_wins_over_text_edit_without_resurrection() throws Exception {
        UUID member = member((byte) 0);
        long id = command(member, () -> service.createExercise(create(UUID.randomUUID(), "원본", START))).exerciseId();
        List<Object> results = concurrent(member,
                () -> outcome(() -> service.deleteExercise(id, UUID.randomUUID())),
                () -> outcome(() -> service.updateExercise(id, update(UUID.randomUUID(), 0L, "편집"))));
        assertThat(results.getFirst()).isInstanceOf(ExerciseIdResponseDto.class);
        assertThat(snapshot(id).deleted()).isTrue();
        assertThat(results.getLast()).isInstanceOfAny(ExerciseIdResponseDto.class, CustomException.class);
    }

    @Test
    void actual_persisted_score_respects_daily_cap_late_zero_and_maximum() {
        UUID member = member((byte) 98);
        assertThat(command(member, () -> service.createExercise(create(UUID.randomUUID(), "첫 번째", START))).earnedPoints()).isEqualTo((byte) 2);
        assertThat(command(member, () -> service.createExercise(create(UUID.randomUUID(), "두 번째", START))).earnedPoints()).isZero();
        assertThat(command(member, () -> service.createExercise(create(UUID.randomUUID(), "일일 초과", START))).earnedPoints()).isZero();
        assertThat(score(member)).isEqualTo((byte) 100);
        UUID capped = member((byte) 0);
        for (int index = 0; index < 3; index++) {
            command(capped, () -> service.createExercise(create(UUID.randomUUID(), "일일 기록", START)));
        }
        assertThat(score(capped)).isEqualTo((byte) 6);
        var late = command(capped, () -> service.createExercise(create(UUID.randomUUID(), "지연", START.minusYears(5))));
        assertThat(late.earnedPoints()).isZero();
        assertThat(score(capped)).isEqualTo((byte) 6);
    }

    @Test
    void policy_failure_and_database_failure_roll_back_record_score_and_receipt() {
        UUID member = member((byte) 0);
        when(policyService.getPolicyValueAsByte(PolicyKey.POINTS_PER_EXERCISE)).thenThrow(new IllegalStateException("정책 오류"));
        assertThatThrownBy(() -> command(member, () -> service.createExercise(create(UUID.randomUUID(), "실패", START))))
                .isInstanceOf(IllegalStateException.class);
        assertThat(exerciseCount(member)).isZero();
        assertThat(receiptCount(member)).isZero();
        assertThat(score(member)).isZero();
        doReturn((byte) 3).when(policyService).getPolicyValueAsByte(PolicyKey.POINTS_PER_EXERCISE);
        assertThatThrownBy(() -> command(member, () -> service.createExercise(create(UUID.randomUUID(), "x".repeat(256), START))))
                .isInstanceOf(RuntimeException.class);
        assertThat(exerciseCount(member)).isZero();
        assertThat(receiptCount(member)).isZero();
        assertThat(score(member)).isZero();
    }

    @Test
    void legacy_score_policy_failure_preserves_record_but_sync_does_not_invent_zero() {
        UUID member = member((byte) 0);
        when(policyService.getPolicy(PolicyKey.EXERCISE_RECORD_VALIDITY_PERIOD)).thenThrow(new IllegalStateException("정책 오류"));
        assertThat(command(member, () -> service.createExercise(create(null, "기존 요청", START))).earnedPoints()).isZero();
        assertThat(exerciseCount(member)).isEqualTo(1);
        assertThat(score(member)).isZero();
        verify(notifyErrorToAdmin).sendErrorToSlackApi(any());
    }

    @Test
    void rolled_back_outer_transaction_leaves_no_score_record_or_receipt() {
        UUID member = member((byte) 0);
        UUID key = UUID.randomUUID();
        assertThatThrownBy(() -> command(member, () -> transaction(() -> {
            service.createExercise(create(key, "롤백", START));
            throw new IllegalStateException("외부 트랜잭션 실패");
        }))).isInstanceOf(IllegalStateException.class);
        assertThat(exerciseCount(member)).isZero();
        assertThat(receiptCount(member)).isZero();
        assertThat(score(member)).isZero();
        assertThat(command(member, () -> service.createExercise(create(key, "롤백", START))).earnedPoints()).isEqualTo((byte) 3);
    }

    @Test
    void photo_receipt_maps_ordered_ids_rejects_changed_binary_and_replays_at_limit_and_after_deletion() {
        UUID member = member((byte) 0);
        long id = command(member, () -> service.createExercise(create(UUID.randomUUID(), "사진", START))).exerciseId();
        UUID key = UUID.randomUUID();
        List<UUID> imageIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        List<MultipartFile> files = List.of(file("1.jpg", 1), file("2.jpg", 2));
        var first = command(member, () -> service.uploadExerciseImages(id, files, key, imageIds));
        assertThat(first.pictures()).extracting(ExercisePicturesResponse.UploadedPicture::clientImageId).containsExactlyElementsOf(imageIds);
        assertThat(first.pictures()).allSatisfy(mapping -> {
            assertThat(mapping.pictureId()).isPositive();
            assertThat(mapping.pictureUrl()).contains(key.toString(), mapping.clientImageId().toString());
        });
        List<UUID> moreIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        command(member, () -> service.uploadExerciseImages(id, List.of(file("3.jpg", 3), file("4.jpg", 4), file("5.jpg", 5)), UUID.randomUUID(), moreIds));
        assertThat(command(member, () -> service.uploadExerciseImages(id, files, key, imageIds))).isEqualTo(first);
        assertError(() -> command(member, () -> service.uploadExerciseImages(id, List.of(file("1.jpg", 9), file("2.jpg", 2)), key, imageIds)), ErrorCode.EXERCISE_REQUEST_ID_REUSED);
        assertError(() -> command(member, () -> service.uploadExerciseImages(id, List.of(file("6.jpg", 6)), UUID.randomUUID(), List.of(UUID.randomUUID()))), ErrorCode.EXERCISE_PICTURE_COUNT_EXCEEDED);
        command(member, () -> service.deleteExercise(id, UUID.randomUUID()));
        assertThat(command(member, () -> service.uploadExerciseImages(id, files, key, imageIds))).isEqualTo(first);
        assertError(() -> command(member, () -> service.uploadExerciseImages(id, List.of(file("6.jpg", 6)), UUID.randomUUID(), List.of(UUID.randomUUID()))), ErrorCode.EXERCISE_DELETED);
        assertThat(activePictures(id)).isZero();
    }

    @Test
    void photo_delete_is_owned_normalized_replayable_and_deleted_ids_are_safe() {
        UUID member = member((byte) 0);
        long first = command(member, () -> service.createExercise(create(UUID.randomUUID(), "첫 기록", START))).exerciseId();
        long second = command(member, () -> service.createExercise(create(UUID.randomUUID(), "둘째 기록", START))).exerciseId();
        var upload = command(member, () -> service.uploadExerciseImages(first, List.of(file("1.jpg", 1), file("2.jpg", 2)),
                UUID.randomUUID(), List.of(UUID.randomUUID(), UUID.randomUUID())));
        List<Long> ids = upload.pictures().stream().map(ExercisePicturesResponse.UploadedPicture::pictureId).toList();
        assertError(() -> command(member, () -> service.deleteImages(second, ids, UUID.randomUUID())), ErrorCode.AUTHORIZATION_DENIED);
        UUID key = UUID.randomUUID();
        var deleted = command(member, () -> service.deleteImages(first, ids, key));
        assertThat(deleted.version()).isEqualTo(2);
        assertThat(command(member, () -> service.deleteImages(first, List.of(ids.getLast(), ids.getFirst(), ids.getFirst()), key))).isEqualTo(deleted);
        assertThat(command(member, () -> service.deleteImages(first, ids, UUID.randomUUID()))).isEqualTo(deleted);
        assertThat(activePictures(first)).isZero();
    }

    @Test
    void deleting_then_adding_counts_only_active_photos() {
        UUID member = member((byte) 0);
        long id = command(member, () -> service.createExercise(create(UUID.randomUUID(), "사진", START))).exerciseId();
        var initial = command(member, () -> service.uploadExerciseImages(id,
                List.of(file("1.jpg", 1), file("2.jpg", 2), file("3.jpg", 3)), UUID.randomUUID(),
                List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())));
        command(member, () -> service.deleteImages(id, initial.pictures().subList(0, 2).stream()
                .map(ExercisePicturesResponse.UploadedPicture::pictureId).toList(), UUID.randomUUID()));
        command(member, () -> service.uploadExerciseImages(id,
                List.of(file("4.jpg", 4), file("5.jpg", 5), file("6.jpg", 6)), UUID.randomUUID(),
                List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())));
        assertThat(activePictures(id)).isEqualTo(4);
    }

    @Test
    void failed_upload_and_commit_time_failure_clean_only_attempted_object_keys() {
        UUID member = member((byte) 0);
        long id = command(member, () -> service.createExercise(create(UUID.randomUUID(), "사진", START))).exerciseId();
        UUID key = UUID.randomUUID();
        UUID image = UUID.randomUUID();
        String objectKey = "uploads/exercise/" + member + "/" + key + "/" + image;
        when(s3Service.uploadImage(any(), eq(objectKey))).thenThrow(new S3UploadFailedException("PUT 응답 유실"));
        assertError(() -> command(member, () -> service.uploadExerciseImages(id, List.of(file("1.jpg", 1)), key, List.of(image))), ErrorCode.EXERCISE_PICTURE_UPLOAD_FAILED);
        verify(s3Service).deleteImage(objectKey);
        assertThat(activePictures(id)).isZero();
        assertThat(receiptCount(member)).isEqualTo(1);
        doReturn("https://s3/" + objectKey).when(s3Service).uploadImage(any(), eq(objectKey));
        assertThatThrownBy(() -> command(member, () -> transaction(() -> {
            service.uploadExerciseImages(id, List.of(file("1.jpg", 1)), key, List.of(image));
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void beforeCommit(boolean readOnly) {
                    throw new IllegalStateException("커밋 실패");
                }
            });
            return null;
        }))).isInstanceOf(IllegalStateException.class);
        verify(s3Service, times(2)).deleteImage(objectKey);
        assertThat(activePictures(id)).isZero();
        assertThat(snapshot(id).version()).isZero();
        assertThat(receiptCount(member)).isEqualTo(1);
        command(member, () -> service.uploadExerciseImages(id, List.of(file("1.jpg", 1)), key, List.of(image)));
        cleanupWorker.rollbackUpload(member, key, List.of(objectKey));
        verify(s3Service, times(2)).deleteImage(objectKey);
        assertThat(activePictures(id)).isEqualTo(1);
    }

    @Test
    void deletion_failure_keeps_durable_task_and_scheduled_retry_completes_it() {
        UUID member = member((byte) 0);
        long id = command(member, () -> service.createExercise(create(UUID.randomUUID(), "사진", START))).exerciseId();
        var upload = command(member, () -> service.uploadExerciseImages(id, List.of(file("1.jpg", 1)),
                UUID.randomUUID(), List.of(UUID.randomUUID())));
        String key = upload.pictures().getFirst().pictureUrl().substring("https://s3/".length());
        doThrow(new S3UploadFailedException("S3 장애")).when(s3Service).deleteImage(key);
        command(member, () -> service.deleteImages(id, List.of(upload.pictures().getFirst().pictureId()), UUID.randomUUID()));
        assertThat(activePictures(id)).isZero();
        Long taskId = transaction(() -> em.createQuery("select t.id from ExercisePhotoCleanup t where t.objectKey = :key", Long.class)
                .setParameter("key", key).getSingleResult());
        assertThat(transaction(() -> cleanupRepository.findById(taskId).orElseThrow().getAttempts())).isEqualTo(1);
        transaction(() -> {
            em.createQuery("update ExercisePhotoCleanup t set t.nextAttemptAt = :due where t.id = :id")
                    .setParameter("due", LocalDateTime.now().minusMinutes(1)).setParameter("id", taskId).executeUpdate();
            return null;
        });
        doNothing().when(s3Service).deleteImage(key);
        cleanupScheduler.retryPending();
        assertThat(cleanupRepository.findById(taskId)).isEmpty();
        verify(s3Service, times(2)).deleteImage(key);
    }

    @Test
    void deletion_rollback_never_removes_committed_photo_or_persists_cleanup_task() {
        UUID member = member((byte) 0);
        long id = command(member, () -> service.createExercise(create(UUID.randomUUID(), "사진", START))).exerciseId();
        var upload = command(member, () -> service.uploadExerciseImages(id, List.of(file("1.jpg", 1)), UUID.randomUUID(), List.of(UUID.randomUUID())));
        String key = upload.pictures().getFirst().pictureUrl().substring("https://s3/".length());
        assertThatThrownBy(() -> command(member, () -> transaction(() -> {
            service.deleteImages(id, List.of(upload.pictures().getFirst().pictureId()), UUID.randomUUID());
            throw new IllegalStateException("롤백");
        }))).isInstanceOf(IllegalStateException.class);
        assertThat(activePictures(id)).isEqualTo(1);
        verify(s3Service, never()).deleteImage(key);
        assertThat(transaction(() -> em.createQuery("select count(t) from ExercisePhotoCleanup t where t.objectKey = :key", Long.class)
                .setParameter("key", key).getSingleResult())).isZero();
    }

    @Test
    void concurrent_distinct_creations_respect_the_persisted_daily_score_cap() throws Exception {
        UUID member = member((byte) 0);
        when(policyService.getPolicyValueAsInt(PolicyKey.EXERCISE_RECORD_MAX_PER_DAY)).thenReturn(1);
        var responses = concurrent(member,
                () -> service.createExercise(create(UUID.randomUUID(), "첫 요청", START)),
                () -> service.createExercise(create(UUID.randomUUID(), "둘째 요청", START)));
        assertThat(responses).extracting(CreateExerciseResponseDto::earnedPoints)
                .containsExactlyInAnyOrder((byte) 3, (byte) 0);
        assertThat(score(member)).isEqualTo((byte) 3);
        assertThat(exerciseCount(member)).isEqualTo(2);
    }

    @Test
    void concurrent_identical_photo_batches_commit_one_mapping_and_detail_version() throws Exception {
        UUID member = member((byte) 0);
        long id = command(member, () -> service.createExercise(create(UUID.randomUUID(), "사진", START))).exerciseId();
        UUID key = UUID.randomUUID();
        List<UUID> imageIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        List<MultipartFile> files = List.of(file("1.jpg", 1), file("2.jpg", 2));
        var responses = concurrent(member, () -> service.uploadExerciseImages(id, files, key, imageIds),
                () -> service.uploadExerciseImages(id, files, key, imageIds));
        assertThat(responses.getLast()).isEqualTo(responses.getFirst());
        assertThat(activePictures(id)).isEqualTo(2);
        assertThat(receiptCount(member)).isEqualTo(2);
        var detail = transaction(() -> exerciseRepository.findExerciseByExerciseId(member, id));
        assertThat(detail.getVersion()).isEqualTo(1);
        assertThat(detail.getPictureDataList().orElseThrow()).extracting("pictureId")
                .containsExactlyInAnyOrderElementsOf(responses.getFirst().pictures().stream()
                        .map(ExercisePicturesResponse.UploadedPicture::pictureId).toList());
        verify(s3Service, times(2)).uploadImage(any(), anyString());
        assertError(() -> command(member, () -> service.uploadExerciseImages(id, files, key,
                List.of(imageIds.getLast(), imageIds.getFirst()))), ErrorCode.EXERCISE_REQUEST_ID_REUSED);
    }

    @Test
    void concurrent_different_photo_batches_cannot_exceed_five_active_photos() throws Exception {
        UUID member = member((byte) 0);
        long id = command(member, () -> service.createExercise(create(UUID.randomUUID(), "사진", START))).exerciseId();
        command(member, () -> service.uploadExerciseImages(id, List.of(file("1.jpg", 1), file("2.jpg", 2), file("3.jpg", 3)),
                UUID.randomUUID(), List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())));
        var results = concurrent(member,
                () -> outcome(() -> service.uploadExerciseImages(id, List.of(file("4.jpg", 4), file("5.jpg", 5)),
                        UUID.randomUUID(), List.of(UUID.randomUUID(), UUID.randomUUID()))),
                () -> outcome(() -> service.uploadExerciseImages(id, List.of(file("6.jpg", 6), file("7.jpg", 7)),
                        UUID.randomUUID(), List.of(UUID.randomUUID(), UUID.randomUUID()))));
        assertThat(results.stream().filter(ExercisePicturesResponse.class::isInstance).count()).isEqualTo(1);
        assertThat(results.stream().filter(CustomException.class::isInstance).map(CustomException.class::cast)
                .map(CustomException::getErrorCode).toList()).containsExactly(ErrorCode.EXERCISE_PICTURE_COUNT_EXCEEDED);
        assertThat(activePictures(id)).isEqualTo(5);
        assertThat(snapshot(id).version()).isEqualTo(2);
    }

    private UUID member(byte score) {
        UUID id = UUID.randomUUID();
        memberIds.add(id);
        transaction(() -> memberRepository.saveAndFlush(Member.builder().memberId(id).memberEmail(id + "@test.com")
                .memberNickname(id.toString()).memberScore(score).build()));
        return id;
    }

    private byte score(UUID member) {
        return transaction(() -> memberRepository.findById(member).orElseThrow().getMemberScore());
    }

    private long exerciseCount(UUID member) {
        return transaction(() -> em.createQuery("select count(e) from Exercise e where e.member.memberId = :id", Long.class)
                .setParameter("id", member).getSingleResult());
    }

    private long receiptCount(UUID member) {
        return transaction(() -> em.createQuery("select count(r) from ExerciseReceipt r where r.member.memberId = :id", Long.class)
                .setParameter("id", member).getSingleResult());
    }

    private long activePictures(long id) {
        return transaction(() -> em.createQuery("select count(ep) from ExercisePicture ep where ep.exercise.id = :id and ep.picture.pictureDeletedAt is null", Long.class)
                .setParameter("id", id).getSingleResult());
    }

    private Snapshot snapshot(long id) {
        return transaction(() -> {
            Exercise exercise = exerciseRepository.findById(id).orElseThrow();
            return new Snapshot(exercise.getExerciseTitle(), exercise.getVersion(), exercise.getExerciseDeletedAt() != null);
        });
    }

    private CreateExerciseRequestDto create(UUID key, String title, LocalDateTime start) {
        return CreateExerciseRequestDto.builder().createRequestId(key).exerciseTitle(title)
                .exerciseStartedAt(start).exerciseEndedAt(start.plusHours(1)).build();
    }

    private UpdateExerciseRequestDto update(UUID key, Long version, String title) {
        return UpdateExerciseRequestDto.builder().editId(key).baseVersion(version).exerciseTitle(title)
                .exerciseStartedAt(START).exerciseEndedAt(START.plusHours(1)).build();
    }

    private MultipartFile file(String name, int value) {
        return new MockMultipartFile("pictures", name, "image/jpeg", new byte[]{(byte) value});
    }

    private <T> T transaction(Supplier<T> action) {
        return transactions.execute(status -> action.get());
    }

    private <T> T command(UUID member, Supplier<T> action) {
        UserContextHolder.setUserId(member);
        try {
            return action.get();
        } finally {
            UserContextHolder.reset();
        }
    }

    private Object outcome(Supplier<?> action) {
        try {
            return action.get();
        } catch (CustomException ex) {
            return ex;
        }
    }

    private void assertError(Runnable action, ErrorCode error) {
        assertThatThrownBy(action::run).isInstanceOf(CustomException.class).hasFieldOrPropertyWithValue("errorCode", error);
    }

    private <T> List<T> concurrent(UUID member, Supplier<T> first, Supplier<T> second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2))) {
            List<Future<T>> futures = new ArrayList<>();
            for (Supplier<T> action : List.of(first, second)) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("동시 요청 시작 시간 초과");
                    }
                    return command(member, action);
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(futures.getFirst().get(20, TimeUnit.SECONDS), futures.getLast().get(20, TimeUnit.SECONDS));
        }
    }

    private record Snapshot(String title, long version, boolean deleted) {
    }
}
