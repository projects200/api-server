package com.project200.undabang.timer.sync;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import com.project200.undabang.timer.custom.dto.request.CustomTimerCreateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerNameUpdateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerStepCreateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerUpdateRequest;
import com.project200.undabang.timer.custom.dto.response.CustomTimerCreateResponse;
import com.project200.undabang.timer.custom.dto.response.CustomTimerDetailResponse;
import com.project200.undabang.timer.custom.dto.response.CustomTimerStepRecord;
import com.project200.undabang.timer.custom.entity.CustomTimer;
import com.project200.undabang.timer.custom.entity.CustomTimerStep;
import com.project200.undabang.timer.custom.repository.CustomTimerRepository;
import com.project200.undabang.timer.custom.repository.CustomTimerStepRepository;
import com.project200.undabang.timer.custom.service.CustomTimerCommandService;
import com.project200.undabang.timer.custom.service.impl.CustomTimerCommandServiceImpl;
import com.project200.undabang.timer.simple.dto.request.SimpleTimerCreateRequestDto;
import com.project200.undabang.timer.simple.dto.request.SimpleTimerUpdateRequestDto;
import com.project200.undabang.timer.simple.dto.response.SimpleTimerCreateResponseDto;
import com.project200.undabang.timer.simple.dto.response.SimpleTimerRecord;
import com.project200.undabang.timer.simple.entity.SimpleTimer;
import com.project200.undabang.timer.simple.repository.SimpleTimerRepository;
import com.project200.undabang.timer.simple.service.SimpleTimerCommandService;
import com.project200.undabang.timer.simple.service.impl.SimpleTimerCommandServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@DataJpaTest
@Import({TestQuerydslConfig.class, TimerSyncService.class,
        SimpleTimerCommandServiceImpl.class, CustomTimerCommandServiceImpl.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("타이머 동기화 서비스 DB 통합 테스트")
class TimerSynchronizationIntegrationTest {

    @Autowired
    private SimpleTimerCommandService simpleTimerCommandService;
    @Autowired
    private CustomTimerCommandService customTimerCommandService;
    @Autowired
    private TimerSyncService timerSyncService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private SimpleTimerRepository simpleTimerRepository;
    @Autowired
    private CustomTimerRepository customTimerRepository;
    @Autowired
    private CustomTimerStepRepository customTimerStepRepository;
    @Autowired
    private EntityManager em;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private PolicyService policyService;

    private final List<UUID> fixtureMemberIds = new ArrayList<>();
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setTimeout(10);
        given(policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_MAX_COUNT)).willReturn(6);
        given(policyService.getPolicyValueAsInt(PolicyKey.CUSTOM_TIMER_STEP_MIN_COUNT)).willReturn(1);
        given(policyService.getPolicyValueAsInt(PolicyKey.CUSTOM_TIMER_STEP_MAX_COUNT)).willReturn(10);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.reset();
        if (fixtureMemberIds.isEmpty()) {
            return;
        }
        // 동시성 검증을 위해 커밋한 이 테스트의 회원과 그 하위 데이터만 제거한다.
        transaction(() -> {
            em.createQuery("delete from TimerEditReceipt r where r.member.memberId in :memberIds")
                    .setParameter("memberIds", fixtureMemberIds).executeUpdate();
            em.createQuery("delete from TimerCreateReceipt r where r.member.memberId in :memberIds")
                    .setParameter("memberIds", fixtureMemberIds).executeUpdate();
            em.createQuery("delete from CustomTimerStep s where s.customTimer.id in "
                            + "(select t.id from CustomTimer t where t.member.memberId in :memberIds)")
                    .setParameter("memberIds", fixtureMemberIds).executeUpdate();
            em.createQuery("delete from CustomTimer t where t.member.memberId in :memberIds")
                    .setParameter("memberIds", fixtureMemberIds).executeUpdate();
            em.createQuery("delete from SimpleTimer t where t.member.memberId in :memberIds")
                    .setParameter("memberIds", fixtureMemberIds).executeUpdate();
            em.createQuery("delete from Member m where m.memberId in :memberIds")
                    .setParameter("memberIds", fixtureMemberIds).executeUpdate();
            return null;
        });
    }

    @Test
    @DisplayName("서로 다른 트랜잭션의 동일 심플 생성 요청은 타이머 한 개와 같은 원본 응답을 남긴다")
    void concurrent_simple_creations_with_the_same_request_id_create_one_timer() throws Exception {
        UUID memberId = createMember();
        UUID createRequestId = UUID.randomUUID();
        SimpleTimerCreateRequestDto request = new SimpleTimerCreateRequestDto(60, createRequestId, 1000L);

        List<SimpleTimerCreateResponseDto> responses = concurrentCommands(memberId,
                () -> simpleTimerCommandService.createSimpleTimer(request));

        SimpleTimerCreateResponseDto first = responses.getFirst();
        assertThat(responses.getLast()).isEqualTo(first);
        assertMillisecondTimestamp(first.getUpdatedAt());
        assertThat(simpleTimerIds(memberId)).containsExactly(first.getSimpleTimerId());
        assertThat(readSimple(first.getSimpleTimerId()))
                .isEqualTo(new SimpleSnapshot(60, 1000L, first.getUpdatedAt(), null));
        assertThat(transaction(() -> timerSyncService.findCreation(memberId, createRequestId, TimerType.SIMPLE)))
                .contains(new TimerCreateResult(first.getSimpleTimerId(), first.getUpdatedAt()));
        assertThat(creationReceiptCount(memberId)).isEqualTo(1);
    }

    @Test
    @DisplayName("서로 다른 트랜잭션의 동일 커스텀 생성 요청은 부모와 스텝을 한 번만 생성한다")
    void concurrent_custom_creations_with_the_same_request_id_create_one_aggregate() throws Exception {
        UUID memberId = createMember();
        UUID createRequestId = UUID.randomUUID();
        CustomTimerCreateRequest request = new CustomTimerCreateRequest("동시 생성", steps("준비", "운동"),
                createRequestId, 1000L);

        List<CustomTimerCreateResponse> responses = concurrentCommands(memberId,
                () -> customTimerCommandService.createCustomTimer(request));

        CustomTimerCreateResponse first = responses.getFirst();
        assertThat(responses.getLast()).isEqualTo(first);
        assertMillisecondTimestamp(first.updatedAt());
        assertThat(customTimerIds(memberId)).containsExactly(first.customTimerId());
        CustomSnapshot saved = readCustom(first.customTimerId());
        assertThat(saved.name()).isEqualTo("동시 생성");
        assertThat(saved.editedAt()).isEqualTo(1000L);
        assertThat(saved.updatedAt()).isEqualTo(first.updatedAt());
        assertThat(saved.steps()).hasSize(2).allSatisfy(step -> assertThat(step.deletedAt()).isNull());
        assertThat(activeSteps(saved)).extracting(StepSnapshot::name).containsExactly("준비", "운동");
        assertThat(transaction(() -> timerSyncService.findCreation(memberId, createRequestId, TimerType.CUSTOM)))
                .contains(new TimerCreateResult(first.customTimerId(), first.updatedAt()));
        assertThat(creationReceiptCount(memberId)).isEqualTo(1);
    }

    @Test
    @DisplayName("더 최신 수정 뒤 과거 성공 요청을 재전송하면 원본 시각을 반환하고 최신 값을 보존한다")
    void simple_edit_replay_preserves_a_newer_committed_edit() {
        UUID memberId = createMember();
        Long timerId = createSimple(memberId, UUID.randomUUID(), 1000L).getSimpleTimerId();
        SimpleTimerUpdateRequestDto original = new SimpleTimerUpdateRequestDto(90, UUID.randomUUID(), 2000L);
        TimerMutationResponse first = command(memberId,
                () -> simpleTimerCommandService.updateSimpleTimer(timerId, original));
        command(memberId, () -> simpleTimerCommandService.updateSimpleTimer(timerId,
                new SimpleTimerUpdateRequestDto(120, UUID.randomUUID(), 3000L)));
        SimpleSnapshot newer = readSimple(timerId);

        TimerMutationResponse replay = command(memberId,
                () -> simpleTimerCommandService.updateSimpleTimer(timerId, original));

        assertThat(replay).isEqualTo(first);
        assertThat(readSimple(timerId)).isEqualTo(newer);
        assertThat(newer.time()).isEqualTo(120);
        assertThat(newer.editedAt()).isEqualTo(3000L);
        assertThat(editReceiptCount(memberId)).isEqualTo(2);
    }

    @Test
    @DisplayName("커스텀 PUT 재전송은 최신 이름과 기존 스텝 ID 및 전체 스텝 수를 보존한다")
    void custom_replace_replay_does_not_insert_steps_or_overwrite_a_newer_name() {
        UUID memberId = createMember();
        Long timerId = createCustom(memberId, UUID.randomUUID(), 1000L).customTimerId();
        CustomTimerUpdateRequest original = new CustomTimerUpdateRequest("교체 이름", steps("운동", "휴식"),
                UUID.randomUUID(), 2000L);
        TimerMutationResponse first = command(memberId,
                () -> customTimerCommandService.updateCustomTimer(timerId, original));
        CustomSnapshot replaced = readCustom(timerId);
        assertThat(replaced.steps()).hasSize(3);
        assertThat(activeSteps(replaced)).extracting(StepSnapshot::name).containsExactly("운동", "휴식");
        assertThat(command(memberId, () -> customTimerCommandService.updateCustomTimer(timerId, original)))
                .isEqualTo(first);
        assertThat(readCustom(timerId)).isEqualTo(replaced);

        command(memberId, () -> customTimerCommandService.updateCustomTimerName(timerId,
                new CustomTimerNameUpdateRequest("더 최신 이름", UUID.randomUUID(), 3000L)));
        CustomSnapshot newer = readCustom(timerId);
        TimerMutationResponse replay = command(memberId,
                () -> customTimerCommandService.updateCustomTimer(timerId, original));

        assertThat(replay).isEqualTo(first);
        assertThat(readCustom(timerId)).isEqualTo(newer);
        assertThat(newer.name()).isEqualTo("더 최신 이름");
        assertThat(newer.editedAt()).isEqualTo(3000L);
        assertThat(editReceiptCount(memberId)).isEqualTo(2);
    }

    @Test
    @DisplayName("삭제 뒤 생성 재전송은 두 종류 모두 원본 ID와 시각을 반환하며 복원하지 않는다")
    void creation_receipts_survive_later_changes_and_deletion_for_both_timer_types() {
        UUID memberId = createMember();
        UUID simpleRequestId = UUID.randomUUID();
        UUID customRequestId = UUID.randomUUID();
        SimpleTimerCreateResponseDto simple = createSimple(memberId, simpleRequestId, 1000L);
        CustomTimerCreateResponse custom = createCustom(memberId, customRequestId, 1000L);
        command(memberId, () -> simpleTimerCommandService.updateSimpleTimer(simple.getSimpleTimerId(),
                new SimpleTimerUpdateRequestDto(180, UUID.randomUUID(), 2000L)));
        command(memberId, () -> customTimerCommandService.updateCustomTimer(custom.customTimerId(),
                new CustomTimerUpdateRequest("교체됨", steps("교체 스텝"), UUID.randomUUID(), 2000L)));
        command(memberId, () -> simpleTimerCommandService.deleteSimpleTimer(simple.getSimpleTimerId(),
                new TimerDeleteRequest(UUID.randomUUID(), 3000L)));
        command(memberId, () -> customTimerCommandService.deleteCustomTimer(custom.customTimerId(),
                new TimerDeleteRequest(UUID.randomUUID(), 3000L)));
        SimpleSnapshot deletedSimple = readSimple(simple.getSimpleTimerId());
        CustomSnapshot deletedCustom = readCustom(custom.customTimerId());

        assertThat(createSimple(memberId, simpleRequestId, 1000L)).isEqualTo(simple);
        assertThat(createCustom(memberId, customRequestId, 1000L)).isEqualTo(custom);
        assertThat(readSimple(simple.getSimpleTimerId())).isEqualTo(deletedSimple);
        assertThat(readCustom(custom.customTimerId())).isEqualTo(deletedCustom);
        assertThat(deletedSimple.deletedAt()).isNotNull();
        assertThat(deletedCustom.deletedAt()).isNotNull();
        assertThat(activeSteps(deletedCustom)).isEmpty();
        assertThat(simpleTimerIds(memberId)).containsExactly(simple.getSimpleTimerId());
        assertThat(customTimerIds(memberId)).containsExactly(custom.customTimerId());
        assertThat(creationReceiptCount(memberId)).isEqualTo(2);
    }

    @Test
    @DisplayName("생성 즉시 오래된 항목으로 정리된 심플 타이머도 생성 재전송 시 원본 응답을 반환한다")
    void creation_replay_preserves_the_original_response_when_the_new_timer_prunes_itself() {
        given(policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_MAX_COUNT)).willReturn(2);
        UUID memberId = createMember();
        SimpleTimerCreateResponseDto newer = createSimple(memberId, UUID.randomUUID(), 2000L);
        SimpleTimerCreateResponseDto newest = createSimple(memberId, UUID.randomUUID(), 3000L);
        SimpleSnapshot newerBefore = readSimple(newer.getSimpleTimerId());
        SimpleSnapshot newestBefore = readSimple(newest.getSimpleTimerId());
        UUID createRequestId = UUID.randomUUID();

        SimpleTimerCreateResponseDto accepted = createSimple(memberId, createRequestId, 1000L);
        SimpleSnapshot pruned = readSimple(accepted.getSimpleTimerId());
        assertThat(pruned.deletedAt()).isNotNull();
        assertThat(accepted.getSimpleTimerId()).isNotIn(newer.getSimpleTimerId(), newest.getSimpleTimerId());

        SimpleTimerCreateResponseDto replay = createSimple(memberId, createRequestId, 1000L);

        assertThat(replay).isEqualTo(accepted);
        assertThat(readSimple(accepted.getSimpleTimerId())).isEqualTo(pruned);
        assertThat(readSimple(newer.getSimpleTimerId())).isEqualTo(newerBefore);
        assertThat(readSimple(newest.getSimpleTimerId())).isEqualTo(newestBefore);
        assertThat(simpleTimerIds(memberId)).containsExactlyInAnyOrder(
                newer.getSimpleTimerId(), newest.getSimpleTimerId(), accepted.getSimpleTimerId());
        assertThat(transaction(() -> simpleTimerRepository.countByMemberAndSimpleTimerDeletedAtNull(
                memberRepository.findById(memberId).orElseThrow()))).isEqualTo(2);
        assertThat(transaction(() -> timerSyncService.findCreation(memberId, createRequestId, TimerType.SIMPLE)))
                .contains(new TimerCreateResult(accepted.getSimpleTimerId(), accepted.getUpdatedAt()));
        assertThat(creationReceiptCount(memberId)).isEqualTo(3);
    }

    @Test
    @DisplayName("커스텀 생성 이후 후속 작업 실패는 부모와 스텝과 생성 영수증을 모두 롤백한다")
    void later_failure_rolls_back_custom_creation_and_its_receipt() {
        UUID memberId = createMember();
        UUID createRequestId = UUID.randomUUID();
        CustomTimerCreateRequest request = new CustomTimerCreateRequest("롤백 대상", steps("준비", "운동"),
                createRequestId, 1000L);

        assertThatThrownBy(() -> command(memberId, () -> {
            CustomTimerCreateResponse created = customTimerCommandService.createCustomTimer(request);
            em.flush();
            em.clear();
            assertThat(customTimerRepository.findById(created.customTimerId())).isPresent();
            assertThat(stepSnapshots(created.customTimerId())).hasSize(2);
            assertThat(timerSyncService.findCreation(memberId, createRequestId, TimerType.CUSTOM))
                    .contains(new TimerCreateResult(created.customTimerId(), created.updatedAt()));
            throw new IllegalStateException("후속 작업 실패");
        })).isInstanceOf(IllegalStateException.class).hasMessage("후속 작업 실패");

        assertThat(customTimerIds(memberId)).isEmpty();
        assertThat(customStepCount(memberId)).isZero();
        assertThat(creationReceiptCount(memberId)).isZero();
        assertThat(transaction(() -> timerSyncService.findCreation(memberId, createRequestId, TimerType.CUSTOM)))
                .isEmpty();
        CustomTimerCreateResponse retry = command(memberId,
                () -> customTimerCommandService.createCustomTimer(request));
        assertThat(customTimerIds(memberId)).containsExactly(retry.customTimerId());
        assertThat(readCustom(retry.customTimerId()).steps()).hasSize(2);
        assertThat(creationReceiptCount(memberId)).isEqualTo(1);
    }

    @Test
    @DisplayName("커스텀 교체 이후 후속 작업 실패는 부모 변경과 스텝 삭제 및 삽입과 수정 영수증을 롤백한다")
    void later_failure_rolls_back_parent_steps_and_edit_receipt_during_replacement() {
        UUID memberId = createMember();
        Long timerId = createCustom(memberId, UUID.randomUUID(), 1000L).customTimerId();
        CustomSnapshot before = readCustom(timerId);
        UUID editId = UUID.randomUUID();
        CustomTimerUpdateRequest request = new CustomTimerUpdateRequest("실패할 교체", steps("운동", "휴식"),
                editId, 2000L);

        assertThatThrownBy(() -> command(memberId, () -> {
            TimerMutationResponse response = customTimerCommandService.updateCustomTimer(timerId, request);
            customTimerStepRepository.flush();
            em.clear();
            CustomTimer changed = customTimerRepository.findById(timerId).orElseThrow();
            assertThat(changed.getCustomTimerName()).isEqualTo("실패할 교체");
            assertThat(changed.getEditedAt()).isEqualTo(2000L);
            List<StepSnapshot> changedSteps = stepSnapshots(timerId);
            assertThat(changedSteps).hasSize(3);
            assertThat(changedSteps.stream().filter(step -> step.deletedAt() == null))
                    .extracting(StepSnapshot::name).containsExactly("운동", "휴식");
            assertThat(timerSyncService.findEdit(memberId, TimerType.CUSTOM, timerId, editId,
                    TimerEditOperation.REPLACE)).contains(response);
            throw new IllegalStateException("후속 작업 실패");
        })).isInstanceOf(IllegalStateException.class).hasMessage("후속 작업 실패");

        assertThat(readCustom(timerId)).isEqualTo(before);
        assertThat(editReceiptCount(memberId)).isZero();
        assertThat(transaction(() -> timerSyncService.findEdit(memberId, TimerType.CUSTOM, timerId, editId,
                TimerEditOperation.REPLACE))).isEmpty();
        TimerMutationResponse retry = command(memberId,
                () -> customTimerCommandService.updateCustomTimer(timerId, request));
        CustomSnapshot saved = readCustom(timerId);
        assertThat(saved.name()).isEqualTo("실패할 교체");
        assertThat(saved.editedAt()).isEqualTo(2000L);
        assertThat(saved.updatedAt()).isEqualTo(retry.updatedAt());
        assertThat(saved.steps()).hasSize(3);
        assertThat(activeSteps(saved)).extracting(StepSnapshot::name).containsExactly("운동", "휴식");
        assertThat(editReceiptCount(memberId)).isEqualTo(1);
    }

    @Test
    @DisplayName("오래된 심플 수정은 현재 DTO로 충돌하고 삭제는 우선하며 과거 성공은 삭제 뒤에도 재생된다")
    void simple_stale_and_deleted_edits_preserve_committed_state_and_successful_replays() {
        UUID memberId = createMember();
        Long timerId = createSimple(memberId, UUID.randomUUID(), 1000L).getSimpleTimerId();
        SimpleTimerUpdateRequestDto successful = new SimpleTimerUpdateRequestDto(90, UUID.randomUUID(), 3000L);
        TimerMutationResponse success = command(memberId,
                () -> simpleTimerCommandService.updateSimpleTimer(timerId, successful));
        SimpleSnapshot current = readSimple(timerId);
        UUID staleEditId = UUID.randomUUID();

        assertThatThrownBy(() -> command(memberId, () -> simpleTimerCommandService.updateSimpleTimer(timerId,
                new SimpleTimerUpdateRequestDto(120, staleEditId, 2000L))))
                .isInstanceOfSatisfying(TimerSyncConflictException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TIMER_EDIT_CONFLICT);
                    assertThat(exception.getCurrentValue())
                            .isEqualTo(new SimpleTimerRecord(timerId, 90, current.updatedAt()));
                });
        assertThat(readSimple(timerId)).isEqualTo(current);
        assertThat(transaction(() -> timerSyncService.findEdit(memberId, TimerType.SIMPLE, timerId, staleEditId,
                TimerEditOperation.UPDATE))).isEmpty();

        TimerMutationResponse deleted = command(memberId, () -> simpleTimerCommandService.deleteSimpleTimer(timerId,
                new TimerDeleteRequest(UUID.randomUUID(), 2000L)));
        SimpleSnapshot tombstone = readSimple(timerId);
        assertThat(tombstone.deletedAt()).isNotNull();
        assertThat(tombstone.updatedAt()).isEqualTo(deleted.updatedAt());
        assertThatThrownBy(() -> command(memberId, () -> simpleTimerCommandService.updateSimpleTimer(timerId,
                new SimpleTimerUpdateRequestDto(120, UUID.randomUUID(), 4000L))))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TIMER_DELETED));
        assertThatThrownBy(() -> command(memberId, () -> simpleTimerCommandService.updateSimpleTimer(timerId,
                new SimpleTimerUpdateRequestDto(120, UUID.randomUUID(), 1000L))))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TIMER_DELETED));
        assertThat(command(memberId, () -> simpleTimerCommandService.updateSimpleTimer(timerId, successful)))
                .isEqualTo(success);
        TimerMutationResponse repeatedDelete = command(memberId,
                () -> simpleTimerCommandService.deleteSimpleTimer(timerId,
                        new TimerDeleteRequest(UUID.randomUUID(), 5000L)));
        assertThat(repeatedDelete).isEqualTo(deleted);
        assertThat(readSimple(timerId)).isEqualTo(tombstone);
        assertThat(editReceiptCount(memberId)).isEqualTo(3);
    }

    @Test
    @DisplayName("오래된 커스텀 교체는 활성 스텝 전체로 충돌하고 삭제 후 새 수정은 410이지만 성공 재생은 유지된다")
    void custom_stale_and_deleted_edits_return_current_steps_without_mutating_the_aggregate() {
        UUID memberId = createMember();
        Long timerId = createCustom(memberId, UUID.randomUUID(), 1000L).customTimerId();
        CustomTimerUpdateRequest successful = new CustomTimerUpdateRequest("현재 이름", steps("운동", "휴식"),
                UUID.randomUUID(), 3000L);
        TimerMutationResponse success = command(memberId,
                () -> customTimerCommandService.updateCustomTimer(timerId, successful));
        CustomSnapshot current = readCustom(timerId);
        UUID staleEditId = UUID.randomUUID();

        assertThatThrownBy(() -> command(memberId, () -> customTimerCommandService.updateCustomTimer(timerId,
                new CustomTimerUpdateRequest("오래된 이름", steps("이전 스텝"), staleEditId, 2000L))))
                .isInstanceOfSatisfying(TimerSyncConflictException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TIMER_EDIT_CONFLICT);
                    assertThat(exception.getCurrentValue()).isInstanceOfSatisfying(CustomTimerDetailResponse.class,
                            value -> {
                                assertThat(value.getCustomTimerId()).isEqualTo(timerId);
                                assertThat(value.getCustomTimerName()).isEqualTo("현재 이름");
                                assertThat(value.getUpdatedAt()).isEqualTo(current.updatedAt());
                                assertThat(value.getCustomTimerStepCount()).isEqualTo(2);
                                assertThat(value.getCustomTimerSteps()).containsExactlyInAnyOrderElementsOf(
                                        activeSteps(current).stream().map(step -> new CustomTimerStepRecord(
                                                step.id(), step.name(), step.order(), step.time())).toList());
                            });
                });
        assertThat(readCustom(timerId)).isEqualTo(current);
        assertThat(transaction(() -> timerSyncService.findEdit(memberId, TimerType.CUSTOM, timerId, staleEditId,
                TimerEditOperation.REPLACE))).isEmpty();

        TimerMutationResponse deleted = command(memberId, () -> customTimerCommandService.deleteCustomTimer(timerId,
                new TimerDeleteRequest(UUID.randomUUID(), 2000L)));
        CustomSnapshot tombstone = readCustom(timerId);
        assertThat(tombstone.deletedAt()).isNotNull();
        assertThat(tombstone.updatedAt()).isEqualTo(deleted.updatedAt());
        assertThat(activeSteps(tombstone)).isEmpty();
        assertThatThrownBy(() -> command(memberId, () -> customTimerCommandService.updateCustomTimer(timerId,
                new CustomTimerUpdateRequest("복원 불가", steps("새 스텝"), UUID.randomUUID(), 1000L))))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TIMER_DELETED));
        assertThatThrownBy(() -> command(memberId, () -> customTimerCommandService.updateCustomTimerName(timerId,
                new CustomTimerNameUpdateRequest("복원 불가", UUID.randomUUID(), 4000L))))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TIMER_DELETED));
        assertThat(command(memberId, () -> customTimerCommandService.updateCustomTimer(timerId, successful)))
                .isEqualTo(success);
        assertThat(command(memberId, () -> customTimerCommandService.deleteCustomTimer(timerId,
                new TimerDeleteRequest(UUID.randomUUID(), 5000L)))).isEqualTo(deleted);
        assertThat(readCustom(timerId)).isEqualTo(tombstone);
        assertThat(editReceiptCount(memberId)).isEqualTo(3);
    }

    @Test
    @DisplayName("생성 키를 다른 타이머 종류에 사용하면 400이며 다른 회원의 동일 키는 별도로 생성된다")
    void creation_request_ids_are_global_across_timer_types_but_scoped_to_the_member() {
        UUID ownerId = createMember();
        UUID otherId = createMember();
        UUID simpleRequestId = UUID.randomUUID();
        UUID customRequestId = UUID.randomUUID();
        SimpleTimerCreateResponseDto ownerSimple = createSimple(ownerId, simpleRequestId, 1000L);
        CustomTimerCreateResponse ownerCustom = createCustom(ownerId, customRequestId, 1000L);

        assertThatThrownBy(() -> createCustom(ownerId, simpleRequestId, 1000L))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TIMER_REQUEST_ID_REUSED));
        assertThatThrownBy(() -> createSimple(ownerId, customRequestId, 1000L))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TIMER_REQUEST_ID_REUSED));
        SimpleTimerCreateResponseDto otherSimple = createSimple(otherId, simpleRequestId, 1000L);
        CustomTimerCreateResponse otherCustom = createCustom(otherId, customRequestId, 1000L);

        assertThat(otherSimple.getSimpleTimerId()).isNotEqualTo(ownerSimple.getSimpleTimerId());
        assertThat(otherCustom.customTimerId()).isNotEqualTo(ownerCustom.customTimerId());
        assertThat(simpleTimerIds(ownerId)).containsExactly(ownerSimple.getSimpleTimerId());
        assertThat(customTimerIds(ownerId)).containsExactly(ownerCustom.customTimerId());
        assertThat(simpleTimerIds(otherId)).containsExactly(otherSimple.getSimpleTimerId());
        assertThat(customTimerIds(otherId)).containsExactly(otherCustom.customTimerId());
        assertThat(creationReceiptCount(ownerId)).isEqualTo(2);
        assertThat(creationReceiptCount(otherId)).isEqualTo(2);
    }

    @Test
    @DisplayName("타인 ID와 성공한 editId로 수정하거나 삭제해도 원본 영수증을 재생하거나 타이머를 변경하지 않는다")
    void foreign_members_cannot_replay_receipts_or_mutate_owned_timers() {
        UUID ownerId = createMember();
        UUID otherId = createMember();
        Long simpleId = createSimple(ownerId, UUID.randomUUID(), 1000L).getSimpleTimerId();
        Long customId = createCustom(ownerId, UUID.randomUUID(), 1000L).customTimerId();
        UUID simpleEditId = UUID.randomUUID();
        UUID customEditId = UUID.randomUUID();
        command(ownerId, () -> simpleTimerCommandService.updateSimpleTimer(simpleId,
                new SimpleTimerUpdateRequestDto(90, simpleEditId, 2000L)));
        command(ownerId, () -> customTimerCommandService.updateCustomTimer(customId,
                new CustomTimerUpdateRequest("소유자 수정", steps("소유자 스텝"), customEditId, 2000L)));
        SimpleSnapshot simpleBefore = readSimple(simpleId);
        CustomSnapshot customBefore = readCustom(customId);

        assertThatThrownBy(() -> command(otherId, () -> simpleTimerCommandService.updateSimpleTimer(simpleId,
                new SimpleTimerUpdateRequestDto(120, simpleEditId, 2000L))))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SIMPLE_TIMER_NOT_FOUND));
        assertThatThrownBy(() -> command(otherId, () -> simpleTimerCommandService.deleteSimpleTimer(simpleId,
                new TimerDeleteRequest(simpleEditId, 2000L))))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SIMPLE_TIMER_NOT_FOUND));
        assertThatThrownBy(() -> command(otherId, () -> customTimerCommandService.updateCustomTimer(customId,
                new CustomTimerUpdateRequest("타인 수정", steps("타인 스텝"), customEditId, 2000L))))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CUSTOM_TIMER_NOT_FOUND));
        assertThatThrownBy(() -> command(otherId, () -> customTimerCommandService.updateCustomTimerName(customId,
                new CustomTimerNameUpdateRequest("타인 수정", UUID.randomUUID(), 2000L))))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CUSTOM_TIMER_NOT_FOUND));
        assertThatThrownBy(() -> command(otherId, () -> customTimerCommandService.deleteCustomTimer(customId,
                new TimerDeleteRequest(customEditId, 2000L))))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CUSTOM_TIMER_NOT_FOUND));

        assertThat(readSimple(simpleId)).isEqualTo(simpleBefore);
        assertThat(readCustom(customId)).isEqualTo(customBefore);
        assertThat(editReceiptCount(ownerId)).isEqualTo(2);
        assertThat(editReceiptCount(otherId)).isZero();
        assertThat(simpleTimerIds(otherId)).isEmpty();
        assertThat(customTimerIds(otherId)).isEmpty();
    }

    private UUID createMember() {
        UUID memberId = UUID.randomUUID();
        transaction(() -> memberRepository.saveAndFlush(Member.builder()
                .memberId(memberId)
                .memberEmail(memberId + "@timer-sync.test")
                .memberNickname("timer-sync-" + memberId)
                .memberBday(LocalDate.of(2000, 1, 1))
                .build()));
        fixtureMemberIds.add(memberId);
        return memberId;
    }

    private SimpleTimerCreateResponseDto createSimple(UUID memberId, UUID createRequestId, long editedAt) {
        return command(memberId, () -> simpleTimerCommandService.createSimpleTimer(
                new SimpleTimerCreateRequestDto(60, createRequestId, editedAt)));
    }

    private CustomTimerCreateResponse createCustom(UUID memberId, UUID createRequestId, long editedAt) {
        return command(memberId, () -> customTimerCommandService.createCustomTimer(
                new CustomTimerCreateRequest("초기 이름", steps("초기 스텝"), createRequestId, editedAt)));
    }

    private List<CustomTimerStepCreateRequest> steps(String... names) {
        List<CustomTimerStepCreateRequest> stepList = new ArrayList<>();
        for (int index = 0; index < names.length; index++) {
            stepList.add(new CustomTimerStepCreateRequest(names[index], (byte) index, 60 + index));
        }
        return stepList;
    }

    private <T> T transaction(Supplier<T> operation) {
        return transactionTemplate.execute(status -> operation.get());
    }

    private <T> T command(UUID memberId, Supplier<T> operation) {
        UserContextHolder.setUserId(memberId);
        try {
            return transaction(operation);
        } finally {
            UserContextHolder.reset();
        }
    }

    private <T> List<T> concurrentCommands(UUID memberId, Supplier<T> operation) throws Exception {
        ExecutorService executor = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(2));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        try {
            for (int index = 0; index < 2; index++) {
                futures.add(executor.submit(() -> command(memberId, () -> {
                    // 각 스레드는 별도 트랜잭션을 연 뒤 같은 출발 신호를 기다린다.
                    ready.countDown();
                    awaitStart(start);
                    return operation.get();
                })));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).as("두 트랜잭션의 시작 준비").isTrue();
            start.countDown();
            List<T> responses = new ArrayList<>();
            for (Future<T> future : futures) {
                responses.add(future.get(15, TimeUnit.SECONDS));
            }
            return responses;
        } finally {
            start.countDown();
            futures.forEach(future -> future.cancel(true));
            executor.shutdownNow();
            assertThat(executor.awaitTermination(15, TimeUnit.SECONDS)).as("타이머 요청 작업 종료").isTrue();
        }
    }

    private void awaitStart(CountDownLatch start) {
        try {
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시 요청 출발 신호 대기 시간 초과");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시 요청 대기 중 인터럽트", exception);
        }
    }

    private SimpleSnapshot readSimple(Long timerId) {
        return transaction(() -> {
            SimpleTimer timer = simpleTimerRepository.findById(timerId).orElseThrow();
            return new SimpleSnapshot(timer.getSimpleTimerTime(), timer.getEditedAt(),
                    timer.getSimpleTimerUpdatedAt(), timer.getSimpleTimerDeletedAt());
        });
    }

    private CustomSnapshot readCustom(Long timerId) {
        return transaction(() -> {
            CustomTimer timer = customTimerRepository.findById(timerId).orElseThrow();
            return new CustomSnapshot(timer.getCustomTimerName(), timer.getEditedAt(),
                    timer.getCustomTimerUpdatedAt(), timer.getCustomTimerDeletedAt(), stepSnapshots(timerId));
        });
    }

    private List<StepSnapshot> stepSnapshots(Long timerId) {
        return em.createQuery("select s from CustomTimerStep s where s.customTimer.id = :timerId order by s.id",
                        CustomTimerStep.class)
                .setParameter("timerId", timerId)
                .getResultList().stream()
                .map(step -> new StepSnapshot(step.getId(), step.getCustomTimerStepName(),
                        step.getCustomTimerStepOrder(), step.getCustomTimerStepTime(), step.getCustomTimerStepDeletedAt()))
                .toList();
    }

    private List<StepSnapshot> activeSteps(CustomSnapshot timer) {
        return timer.steps().stream().filter(step -> step.deletedAt() == null).toList();
    }

    private List<Long> simpleTimerIds(UUID memberId) {
        return transaction(() -> em.createQuery(
                        "select t.id from SimpleTimer t where t.member.memberId = :memberId order by t.id", Long.class)
                .setParameter("memberId", memberId).getResultList());
    }

    private List<Long> customTimerIds(UUID memberId) {
        return transaction(() -> em.createQuery(
                        "select t.id from CustomTimer t where t.member.memberId = :memberId order by t.id", Long.class)
                .setParameter("memberId", memberId).getResultList());
    }

    private long customStepCount(UUID memberId) {
        return transaction(() -> em.createQuery(
                        "select count(s) from CustomTimerStep s where s.customTimer.member.memberId = :memberId", Long.class)
                .setParameter("memberId", memberId).getSingleResult());
    }

    private long creationReceiptCount(UUID memberId) {
        return transaction(() -> em.createQuery(
                        "select count(r) from TimerCreateReceipt r where r.member.memberId = :memberId", Long.class)
                .setParameter("memberId", memberId).getSingleResult());
    }

    private long editReceiptCount(UUID memberId) {
        return transaction(() -> em.createQuery(
                        "select count(r) from TimerEditReceipt r where r.member.memberId = :memberId", Long.class)
                .setParameter("memberId", memberId).getSingleResult());
    }

    private void assertMillisecondTimestamp(LocalDateTime timestamp) {
        assertThat(timestamp).isNotNull();
        assertThat(timestamp.getNano() % 1_000_000).isZero();
    }

    private record SimpleSnapshot(Integer time, Long editedAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
    }

    private record CustomSnapshot(String name, Long editedAt, LocalDateTime updatedAt, LocalDateTime deletedAt,
                                  List<StepSnapshot> steps) {
    }

    private record StepSnapshot(Long id, String name, Byte order, Integer time, LocalDateTime deletedAt) {
    }
}
