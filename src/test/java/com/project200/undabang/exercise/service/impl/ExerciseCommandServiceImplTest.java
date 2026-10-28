package com.project200.undabang.exercise.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
import com.project200.undabang.exercise.dto.request.UpdateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.CreateExerciseResponseDto;
import com.project200.undabang.exercise.dto.response.ExerciseIdResponseDto;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.exercise.service.ExercisePictureService;
import com.project200.undabang.exercise.sync.ExerciseSyncConflictException;
import com.project200.undabang.exercise.sync.ExerciseSyncService;
import com.project200.undabang.exercise.sync.repository.ExerciseReceiptRepository;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.score.service.ExerciseScoreCommandService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExerciseCommandServiceImplTest {
    @Mock ExerciseRepository exerciseRepository;
    @Mock MemberRepository memberRepository;
    @Mock ExerciseReceiptRepository receiptRepository;
    @Mock ExercisePictureService pictureService;
    @Mock ExerciseScoreCommandService scoreService;

    private ExerciseCommandServiceImpl service;
    private Member member;
    private Exercise exercise;
    private static final LocalDateTime START = LocalDateTime.of(2025, 5, 1, 10, 0);

    @BeforeEach
    void setUp() {
        member = Member.builder().memberId(UUID.randomUUID()).build();
        exercise = Exercise.builder().id(1L).member(member).exerciseTitle("원본")
                .exerciseStartedAt(START).exerciseEndedAt(START.plusHours(1)).build();
        UserContextHolder.setUserId(member.getMemberId());
        lenient().when(memberRepository.findByIdForUpdate(member.getMemberId())).thenReturn(Optional.of(member));
        lenient().when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));
        ExerciseSyncService sync = new ExerciseSyncService(memberRepository, exerciseRepository, receiptRepository,
                new ObjectMapper().findAndRegisterModules());
        service = new ExerciseCommandServiceImpl(pictureService, exerciseRepository, scoreService, sync);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.reset();
    }

    @Test
    void legacy_creation_keeps_the_original_score_response() {
        when(exerciseRepository.save(any())).thenAnswer(invocation -> {
            Exercise saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 2L);
            return saved;
        });
        when(scoreService.awardPointsForExercise(any())).thenReturn((byte) 10);
        assertThat(service.createExercise(create(null))).isEqualTo(new CreateExerciseResponseDto(2L, (byte) 10, 0));
    }

    @Test
    void sync_score_failure_is_not_a_successful_creation() {
        when(scoreService.awardPointsAtomically(any())).thenThrow(new IllegalStateException("정책 오류"));
        assertThatThrownBy(() -> service.createExercise(create(UUID.randomUUID()))).isInstanceOf(IllegalStateException.class);
        verify(exerciseRepository, never()).save(any());
        verify(receiptRepository, never()).save(any());
    }

    @Test
    void missing_member_rejects_every_command() {
        when(memberRepository.findByIdForUpdate(member.getMemberId())).thenReturn(Optional.empty());
        for (Supplier<?> action : commands()) {
            assertError(action, ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    @Test
    void another_members_record_rejects_update_and_both_deletions() {
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(Exercise.builder()
                .member(Member.builder().memberId(UUID.randomUUID()).build()).build()));
        for (Supplier<?> action : mutations()) {
            assertError(action, ErrorCode.AUTHORIZATION_DENIED);
        }
        verifyNoInteractions(pictureService);
    }

    @Test
    void missing_record_rejects_update_and_both_deletions() {
        when(exerciseRepository.findById(1L)).thenReturn(Optional.empty());
        for (Supplier<?> action : mutations()) {
            assertError(action, ErrorCode.EXERCISE_NOT_FOUND);
        }
    }

    @Test
    void legacy_edit_changes_text_and_increments_only_for_a_real_change() {
        assertThat(service.updateExercise(1L, update("변경", null, null))).isEqualTo(new ExerciseIdResponseDto(1L, 1));
        assertThat(exercise.getExerciseTitle()).isEqualTo("변경");
        assertThat(service.updateExercise(1L, update("변경", null, null)).version()).isEqualTo(1);
    }

    @Test
    void invalid_dates_and_partial_metadata_are_rejected() {
        UpdateExerciseRequestDto future = UpdateExerciseRequestDto.builder().exerciseTitle("변경")
                .exerciseStartedAt(START).exerciseEndedAt(LocalDateTime.now().plusDays(2)).build();
        assertError(() -> service.updateExercise(1L, future), ErrorCode.INVALID_INPUT_VALUE);
        assertError(() -> service.updateExercise(1L, update("변경", UUID.randomUUID(), null)), ErrorCode.INVALID_INPUT_VALUE);
        assertError(() -> service.updateExercise(1L, update("변경", null, 0L)), ErrorCode.INVALID_INPUT_VALUE);
        assertError(() -> service.updateExercise(1L, update("변경", UUID.randomUUID(), -1L)), ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void stale_edit_reports_current_version_without_changing_text() {
        exercise.incrementVersion();
        assertThatThrownBy(() -> service.updateExercise(1L, update("덮어쓰기", UUID.randomUUID(), 0L)))
                .isInstanceOf(ExerciseSyncConflictException.class)
                .extracting("currentValue").isEqualTo(new ExerciseIdResponseDto(1L, 1));
        assertThat(exercise.getExerciseTitle()).isEqualTo("원본");
    }

    @Test
    void photo_deletion_increments_only_when_an_active_picture_changes() {
        when(pictureService.deleteExercisePictures(member.getMemberId(), 1L, List.of(1L, 2L))).thenReturn(true, false);
        assertThat(service.deleteImages(1L, List.of(2L, 1L, 1L), null).version()).isEqualTo(1);
        assertThat(service.deleteImages(1L, List.of(1L, 2L), null).version()).isEqualTo(1);
    }

    @Test
    void record_deletion_is_idempotent_and_blocks_later_updates() {
        when(pictureService.getAllImagesFromExercise(member.getMemberId(), 1L)).thenReturn(List.of(3L));
        assertThat(service.deleteExercise(1L, null).version()).isEqualTo(1);
        assertThat(service.deleteExercise(1L, null).version()).isEqualTo(1);
        assertThat(exercise.getExerciseDeletedAt()).isNotNull();
        assertError(() -> service.updateExercise(1L, update("변경", null, null)), ErrorCode.EXERCISE_DELETED);
        verify(pictureService, times(1)).deleteExercisePictures(member.getMemberId(), 1L, List.of(3L));
    }

    @Test
    void failed_picture_lookup_or_deletion_does_not_tombstone_record() {
        when(pictureService.getAllImagesFromExercise(member.getMemberId(), 1L))
                .thenThrow(new CustomException(ErrorCode.EXERCISE_NOT_FOUND))
                .thenThrow(new CustomException(ErrorCode.AUTHORIZATION_DENIED))
                .thenReturn(List.of(3L));
        assertError(() -> service.deleteExercise(1L, null), ErrorCode.EXERCISE_NOT_FOUND);
        assertError(() -> service.deleteExercise(1L, null), ErrorCode.AUTHORIZATION_DENIED);
        when(pictureService.deleteExercisePictures(member.getMemberId(), 1L, List.of(3L)))
                .thenThrow(new CustomException(ErrorCode.EXERCISE_PICTURE_DELETE_FAILED));
        assertError(() -> service.deleteExercise(1L, null), ErrorCode.EXERCISE_PICTURE_DELETE_FAILED);
        assertThat(exercise.getExerciseDeletedAt()).isNull();
    }

    private List<Supplier<?>> mutations() {
        return List.of(() -> service.updateExercise(1L, update("변경", null, null)),
                () -> service.deleteExercise(1L, null), () -> service.deleteImages(1L, List.of(1L), null));
    }

    private List<Supplier<?>> commands() {
        return List.of(() -> service.createExercise(create(null)), () -> service.updateExercise(1L, update("변경", null, null)),
                () -> service.deleteExercise(1L, null), () -> service.deleteImages(1L, List.of(1L), null));
    }

    private void assertError(Supplier<?> action, ErrorCode error) {
        assertThatThrownBy(action::get).isInstanceOf(CustomException.class).hasFieldOrPropertyWithValue("errorCode", error);
    }

    private CreateExerciseRequestDto create(UUID key) {
        return CreateExerciseRequestDto.builder().createRequestId(key).exerciseTitle("생성")
                .exerciseStartedAt(START).exerciseEndedAt(START.plusHours(1)).build();
    }

    private UpdateExerciseRequestDto update(String title, UUID key, Long version) {
        return UpdateExerciseRequestDto.builder().editId(key).baseVersion(version).exerciseTitle(title)
                .exerciseStartedAt(START).exerciseEndedAt(START.plusHours(1)).build();
    }
}
