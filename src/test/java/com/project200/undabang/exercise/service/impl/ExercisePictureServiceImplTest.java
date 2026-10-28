package com.project200.undabang.exercise.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.repository.PictureRepository;
import com.project200.undabang.common.service.S3Service;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.exception.S3UploadFailedException;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.entity.ExercisePicture;
import com.project200.undabang.exercise.repository.ExercisePictureRepository;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.exercise.sync.ExercisePhotoCleanupWorker;
import com.project200.undabang.exercise.sync.ExerciseSyncService;
import com.project200.undabang.exercise.sync.entity.ExercisePhotoCleanup;
import com.project200.undabang.exercise.sync.repository.ExercisePhotoCleanupRepository;
import com.project200.undabang.exercise.sync.repository.ExerciseReceiptRepository;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExercisePictureServiceImplTest {
    @Mock ExerciseRepository exerciseRepository;
    @Mock MemberRepository memberRepository;
    @Mock PictureRepository pictureRepository;
    @Mock ExercisePictureRepository exercisePictureRepository;
    @Mock ExerciseReceiptRepository receiptRepository;
    @Mock ExercisePhotoCleanupRepository cleanupRepository;
    @Mock ExercisePhotoCleanupWorker cleanupWorker;
    @Mock S3Service s3Service;

    private ExercisePictureServiceImpl service;
    private Member member;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        member = Member.builder().memberId(UUID.randomUUID()).build();
        exercise = Exercise.builder().id(1L).member(member).build();
        UserContextHolder.setUserId(member.getMemberId());
        TransactionSynchronizationManager.initSynchronization();
        lenient().when(memberRepository.findByIdForUpdate(member.getMemberId())).thenReturn(Optional.of(member));
        lenient().when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));
        ExerciseSyncService sync = new ExerciseSyncService(memberRepository, exerciseRepository, receiptRepository,
                new ObjectMapper().findAndRegisterModules());
        service = new ExercisePictureServiceImpl(pictureRepository, exercisePictureRepository, s3Service,
                sync, cleanupRepository, cleanupWorker);
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
        UserContextHolder.reset();
    }

    @Test
    void upload_returns_ordered_explicit_ids_and_preserves_long_filename_policy() {
        prepareUpload();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        var result = service.uploadExercisePictures(1L, List.of(file("a".repeat(300) + ".jpg"), file("다음.png")),
                UUID.randomUUID(), List.of(first, second));
        assertThat(result.version()).isEqualTo(1);
        assertThat(result.pictures()).extracting("clientImageId").containsExactly(first, second);
        assertThat(result.pictures()).extracting("pictureId").containsExactly(1L, 2L);
        ArgumentCaptor<Picture> saved = ArgumentCaptor.forClass(Picture.class);
        verify(pictureRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues().getFirst().getPictureName()).hasSize(255);
        assertThat(result.pictures()).allSatisfy(mapping -> assertThat(mapping.pictureUrl()).startsWith("https://s3/uploads/"));
    }

    @Test
    void legacy_upload_omits_sync_mapping() {
        prepareUpload();
        when(s3Service.generateObjectKey(anyString(), any())).thenReturn("uploads/legacy.jpg");
        assertThat(service.uploadExercisePictures(1L, List.of(file("파일.jpg")), null, null).pictures()).isNull();
    }

    @Test
    void missing_or_foreign_exercise_is_rejected_for_upload_lookup_and_delete() {
        when(exerciseRepository.findById(1L)).thenReturn(Optional.empty());
        assertError(() -> service.uploadExercisePictures(1L, List.of(file("1.jpg")), null, null), ErrorCode.EXERCISE_NOT_FOUND);
        assertError(() -> service.getAllImagesFromExercise(member.getMemberId(), 1L), ErrorCode.EXERCISE_NOT_FOUND);
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(Exercise.builder()
                .member(Member.builder().memberId(UUID.randomUUID()).build()).build()));
        assertError(() -> service.uploadExercisePictures(1L, List.of(file("1.jpg")), null, null), ErrorCode.AUTHORIZATION_DENIED);
        assertError(() -> service.getAllImagesFromExercise(member.getMemberId(), 1L), ErrorCode.AUTHORIZATION_DENIED);
        assertError(() -> service.deleteExercisePictures(member.getMemberId(), 1L, List.of(1L)), ErrorCode.AUTHORIZATION_DENIED);
        verifyNoInteractions(s3Service);
    }

    @Test
    void active_count_limit_and_deleted_record_block_fresh_upload() {
        when(exercisePictureRepository.countNotDeletedPicturesByExerciseId(1L)).thenReturn(5L);
        assertError(() -> service.uploadExercisePictures(1L, List.of(file("1.jpg")), null, null), ErrorCode.EXERCISE_PICTURE_COUNT_EXCEEDED);
        exercise.deleteExercise();
        assertError(() -> service.uploadExercisePictures(1L, List.of(file("1.jpg")), null, null), ErrorCode.EXERCISE_DELETED);
        verifyNoInteractions(s3Service);
    }

    @Test
    void upload_failure_cleans_attempted_keys_including_lost_put_response_on_rollback() {
        prepareUpload();
        when(s3Service.uploadImage(any(), anyString())).thenAnswer(invocation -> {
            if (((MultipartFile) invocation.getArgument(0)).getOriginalFilename().equals("2.jpg")) {
                throw new S3UploadFailedException("응답 유실");
            }
            return "https://s3/" + invocation.getArgument(1);
        });
        UUID request = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        assertError(() -> service.uploadExercisePictures(1L, List.of(file("1.jpg"), file("2.jpg")), request, List.of(first, second)),
                ErrorCode.EXERCISE_PICTURE_UPLOAD_FAILED);
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(cleanupWorker).rollbackUpload(member.getMemberId(), request,
                List.of("uploads/exercise/" + member.getMemberId() + "/" + request + "/" + first,
                        "uploads/exercise/" + member.getMemberId() + "/" + request + "/" + second));
    }

    @Test
    void database_save_failure_is_not_success_and_commit_failure_cleanup_is_registered() {
        when(s3Service.uploadImage(any(), anyString())).thenReturn("https://s3/uploads/key");
        when(pictureRepository.save(any())).thenThrow(new IllegalStateException("DB"));
        assertError(() -> service.uploadExercisePictures(1L, List.of(file("1.jpg")), UUID.randomUUID(), List.of(UUID.randomUUID())),
                ErrorCode.EXERCISE_PICTURE_UPLOAD_FAILED);
        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(cleanupWorker).rollbackUpload(eq(member.getMemberId()), any(), anyList());
    }

    @Test
    void deletion_persists_object_keys_and_never_deletes_before_commit() {
        Picture picture = Picture.builder().id(7L).pictureUrl("https://s3/uploads/key.jpg").build();
        when(exercisePictureRepository.findAllByExercise_Id(1L)).thenReturn(List.of(link(picture)));
        when(s3Service.extractObjectKeyFromUrl(picture.getPictureUrl())).thenReturn("uploads/key.jpg");
        when(cleanupRepository.save(any())).thenAnswer(invocation -> {
            ExercisePhotoCleanup task = invocation.getArgument(0);
            ReflectionTestUtils.setField(task, "id", 9L);
            return task;
        });
        assertThat(service.deleteExercisePictures(member.getMemberId(), 1L, List.of(7L))).isTrue();
        assertThat(picture.getPictureDeletedAt()).isNotNull();
        verifyNoInteractions(cleanupWorker);
        verify(s3Service, never()).deleteImage(anyString());
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(cleanupWorker).process(9L);
        assertThat(service.deleteExercisePictures(member.getMemberId(), 1L, List.of(7L))).isFalse();
        verify(cleanupRepository, times(1)).save(any());
    }

    @Test
    void cross_record_id_rejects_entire_batch_even_if_some_ids_are_valid() {
        Picture picture = Picture.builder().id(7L).build();
        when(exercisePictureRepository.findAllByExercise_Id(1L)).thenReturn(List.of(link(picture)));
        assertError(() -> service.deleteExercisePictures(member.getMemberId(), 1L, List.of(7L, 8L)), ErrorCode.AUTHORIZATION_DENIED);
        assertThat(picture.getPictureDeletedAt()).isNull();
        verifyNoInteractions(cleanupRepository);
    }

    @Test
    void lookup_returns_common_picture_ids_and_an_empty_list_when_absent() {
        when(exercisePictureRepository.findAllByExercise_Id(1L))
                .thenReturn(List.of(link(Picture.builder().id(7L).build()), link(Picture.builder().id(8L).build())), List.of());
        assertThat(service.getAllImagesFromExercise(member.getMemberId(), 1L)).containsExactly(7L, 8L);
        assertThat(service.getAllImagesFromExercise(member.getMemberId(), 1L)).isEmpty();
    }

    private void prepareUpload() {
        AtomicLong ids = new AtomicLong();
        when(s3Service.uploadImage(any(), anyString())).thenAnswer(invocation -> "https://s3/" + invocation.getArgument(1));
        when(pictureRepository.save(any())).thenAnswer(invocation -> {
            Picture picture = invocation.getArgument(0);
            ReflectionTestUtils.setField(picture, "id", ids.incrementAndGet());
            return picture;
        });
    }

    private ExercisePicture link(Picture picture) {
        return ExercisePicture.builder().id(picture.getId()).exercise(exercise).picture(picture).build();
    }

    private MultipartFile file(String name) {
        return new MockMultipartFile("pictures", name, "image/jpeg", new byte[]{1, 2, 3});
    }

    private void assertError(Runnable action, ErrorCode error) {
        assertThatThrownBy(action::run).isInstanceOf(CustomException.class).hasFieldOrPropertyWithValue("errorCode", error);
    }
}
