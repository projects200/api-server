package com.project200.undabang.timer.sync;

import com.project200.undabang.common.web.advice.GlobalExceptionHandler;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.timer.sync.repository.TimerCreateReceiptRepository;
import com.project200.undabang.timer.sync.repository.TimerEditReceiptRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DataJpaTest
@Import({TestQuerydslConfig.class, TimerSyncService.class})
class TimerSyncServiceTest {
    private static final UUID REQUEST_ID = UUID.fromString("00000000-0000-0000-0000-000000000574");
    private static final LocalDateTime ORIGINAL_TIMESTAMP = LocalDateTime.of(2000, 1, 2, 3, 4, 5, 123_000_000);

    @Autowired
    private TimerSyncService syncService;

    @Autowired
    private EntityManager em;

    @Autowired
    private TimerCreateReceiptRepository createReceiptRepository;

    @Autowired
    private TimerEditReceiptRepository editReceiptRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("회원 잠금은 영수증을 저장한 뒤에도 명령 트랜잭션에 남는다")
    void member_write_lock_remains_in_enclosing_transaction() {
        Member member = createMember(UUID.randomUUID());
        em.flush();
        em.clear();

        Member lockedMember = syncService.lockMember(member.getMemberId());
        syncService.recordCreation(lockedMember, REQUEST_ID, TimerType.SIMPLE, 101L, ORIGINAL_TIMESTAMP);
        em.flush();

        assertThat(em.getLockMode(lockedMember)).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(syncService.findCreation(member.getMemberId(), REQUEST_ID, TimerType.SIMPLE))
                .contains(new TimerCreateResult(101L, ORIGINAL_TIMESTAMP));
    }

    @Test
    @DisplayName("존재하지 않는 회원은 기존 회원 없음 오류를 반환한다")
    void missing_member_is_not_found() {
        assertThatThrownBy(() -> syncService.lockMember(UUID.randomUUID()))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("동기화 인프라는 명령 트랜잭션 없이 별도 잠금이나 영수증을 커밋하지 않는다")
    void command_transaction_is_mandatory() {
        Member member = Member.builder().memberId(UUID.randomUUID()).build();

        assertThatThrownBy(() -> syncService.lockMember(member.getMemberId()))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> syncService.recordCreation(member, REQUEST_ID, TimerType.SIMPLE, 101L, ORIGINAL_TIMESTAMP))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> syncService.recordEdit(member, TimerType.SIMPLE, 101L, REQUEST_ID,
                TimerEditOperation.UPDATE, ORIGINAL_TIMESTAMP))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    @DisplayName("생성 재요청은 이후 편집 및 삭제 영수증과 무관하게 최초 ID와 시각을 반환한다")
    void creation_replay_preserves_original_result() {
        Member member = syncService.lockMember(createMember(UUID.randomUUID()).getMemberId());
        syncService.recordCreation(member, REQUEST_ID, TimerType.SIMPLE, 101L, ORIGINAL_TIMESTAMP);
        syncService.recordEdit(member, TimerType.SIMPLE, 101L, UUID.randomUUID(),
                TimerEditOperation.UPDATE, ORIGINAL_TIMESTAMP.plusDays(1));
        syncService.recordEdit(member, TimerType.SIMPLE, 101L, UUID.randomUUID(),
                TimerEditOperation.DELETE, ORIGINAL_TIMESTAMP.plusDays(2));
        flushAndClear();

        TimerCreateResult firstReplay = syncService.findCreation(member.getMemberId(), REQUEST_ID, TimerType.SIMPLE).orElseThrow();
        TimerCreateResult secondReplay = syncService.findCreation(member.getMemberId(), REQUEST_ID, TimerType.SIMPLE).orElseThrow();

        assertThat(firstReplay).isEqualTo(new TimerCreateResult(101L, ORIGINAL_TIMESTAMP));
        assertThat(secondReplay).isEqualTo(firstReplay);
        assertThat(creationCount(member.getMemberId())).isEqualTo(1);
        assertThat(editCount(member.getMemberId())).isEqualTo(2);
    }

    @Test
    @DisplayName("생성 영수증은 다른 회원에게 노출하지 않고 다른 종류에 재사용할 수 없다")
    void creation_replay_respects_owner_and_timer_type() {
        Member owner = syncService.lockMember(createMember(UUID.randomUUID()).getMemberId());
        Member other = createMember(UUID.randomUUID());
        syncService.recordCreation(owner, REQUEST_ID, TimerType.SIMPLE, 101L, ORIGINAL_TIMESTAMP);
        flushAndClear();

        assertThat(syncService.findCreation(other.getMemberId(), REQUEST_ID, TimerType.SIMPLE)).isEmpty();
        assertThatThrownBy(() -> syncService.findCreation(owner.getMemberId(), REQUEST_ID, TimerType.CUSTOM))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TIMER_REQUEST_ID_REUSED));
        assertThat(createReceiptRepository.findByMember_MemberIdAndCreateRequestId(owner.getMemberId(), REQUEST_ID))
                .hasValueSatisfying(receipt -> {
                    assertThat(receipt.getTimerType()).isEqualTo(TimerType.SIMPLE);
                    assertThat(receipt.getTimerId()).isEqualTo(101L);
                    assertThat(receipt.getUpdatedAt()).isEqualTo(ORIGINAL_TIMESTAMP);
                });
        assertThat(creationCount(owner.getMemberId())).isEqualTo(1);
        assertThat(creationCount(other.getMemberId())).isZero();
    }

    @Test
    @DisplayName("편집 재요청은 나중 삭제와 무관하게 최초 성공 시각을 반환한다")
    void edit_replay_preserves_original_response_after_later_deletion() {
        Member member = syncService.lockMember(createMember(UUID.randomUUID()).getMemberId());
        UUID deleteId = UUID.randomUUID();
        syncService.recordEdit(member, TimerType.CUSTOM, 101L, REQUEST_ID,
                TimerEditOperation.REPLACE, ORIGINAL_TIMESTAMP);
        syncService.recordEdit(member, TimerType.CUSTOM, 101L, deleteId,
                TimerEditOperation.DELETE, ORIGINAL_TIMESTAMP.plusDays(1));
        flushAndClear();

        assertThat(syncService.findEdit(member.getMemberId(), TimerType.CUSTOM, 101L, REQUEST_ID, TimerEditOperation.REPLACE))
                .contains(new TimerMutationResponse(ORIGINAL_TIMESTAMP));
        assertThat(syncService.findEdit(member.getMemberId(), TimerType.CUSTOM, 101L, deleteId, TimerEditOperation.DELETE))
                .contains(new TimerMutationResponse(ORIGINAL_TIMESTAMP.plusDays(1)));
        assertThat(editCount(member.getMemberId())).isEqualTo(2);
    }

    @Test
    @DisplayName("편집 재요청은 다른 회원과 종류와 타이머를 분리하고 동작 재사용을 거절한다")
    void edit_replay_respects_owner_type_timer_and_operation() {
        Member owner = syncService.lockMember(createMember(UUID.randomUUID()).getMemberId());
        Member other = createMember(UUID.randomUUID());
        syncService.recordEdit(owner, TimerType.CUSTOM, 101L, REQUEST_ID, TimerEditOperation.RENAME, ORIGINAL_TIMESTAMP);
        flushAndClear();

        assertThat(syncService.findEdit(other.getMemberId(), TimerType.CUSTOM, 101L, REQUEST_ID, TimerEditOperation.RENAME)).isEmpty();
        assertThat(syncService.findEdit(owner.getMemberId(), TimerType.SIMPLE, 101L, REQUEST_ID, TimerEditOperation.RENAME)).isEmpty();
        assertThat(syncService.findEdit(owner.getMemberId(), TimerType.CUSTOM, 102L, REQUEST_ID, TimerEditOperation.RENAME)).isEmpty();
        assertThatThrownBy(() -> syncService.findEdit(owner.getMemberId(), TimerType.CUSTOM, 101L, REQUEST_ID, TimerEditOperation.DELETE))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TIMER_REQUEST_ID_REUSED));
        assertThat(editReceiptRepository.findByMember_MemberIdAndTimerTypeAndTimerIdAndEditId(
                owner.getMemberId(), TimerType.CUSTOM, 101L, REQUEST_ID))
                .hasValueSatisfying(receipt -> {
                    assertThat(receipt.getOperation()).isEqualTo(TimerEditOperation.RENAME);
                    assertThat(receipt.getUpdatedAt()).isEqualTo(ORIGINAL_TIMESTAMP);
                });
        assertThat(editCount(owner.getMemberId())).isEqualTo(1);
        assertThat(editCount(other.getMemberId())).isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("명령 실패는 회원 변경과 두 종류의 성공 영수증을 함께 롤백한다")
    void failed_command_rolls_back_state_and_receipts_atomically() {
        UUID memberId = UUID.randomUUID();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            Member member = createMember(memberId);
            syncService.lockMember(memberId);
            member.updateMemberInfo("변경된 회원", null, "롤백할 변경");
            syncService.recordCreation(member, REQUEST_ID, TimerType.SIMPLE, 101L, ORIGINAL_TIMESTAMP);
            syncService.recordEdit(member, TimerType.SIMPLE, 101L, REQUEST_ID, TimerEditOperation.UPDATE, ORIGINAL_TIMESTAMP);
            em.flush();
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        })).isInstanceOf(CustomException.class);

        transaction.executeWithoutResult(status -> {
            assertThat(em.find(Member.class, memberId)).isNull();
            assertThat(syncService.findCreation(memberId, REQUEST_ID, TimerType.SIMPLE)).isEmpty();
            assertThat(syncService.findEdit(memberId, TimerType.SIMPLE, 101L, REQUEST_ID, TimerEditOperation.UPDATE)).isEmpty();
            assertThat(creationCount(memberId)).isZero();
            assertThat(editCount(memberId)).isZero();
        });
    }

    @ParameterizedTest
    @MethodSource("invalidCreateMetadata")
    @DisplayName("생성은 ID 없는 기기 시각과 음수 시각을 거절한다")
    void rejects_invalid_create_metadata(UUID requestId, Long editedAt) {
        assertThatThrownBy(() -> TimerSyncService.validateCreateMetadata(requestId, editedAt))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    static Stream<Arguments> invalidCreateMetadata() {
        return Stream.of(Arguments.of(null, 0L), Arguments.of(null, -1L), Arguments.of(REQUEST_ID, -1L));
    }

    @Test
    @DisplayName("편집 메타데이터가 없으면 기존 요청이고 ID와 비음수 시각이 있으면 동기화 요청이다")
    void distinguishes_legacy_and_sync_edits() {
        assertThat(TimerSyncService.isSyncEdit(null, null)).isFalse();
        assertThat(TimerSyncService.isSyncEdit(REQUEST_ID, 0L)).isTrue();
        assertThat(TimerSyncService.isSyncEdit(REQUEST_ID, Long.MAX_VALUE)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("invalidEditMetadata")
    @DisplayName("편집은 불완전한 메타데이터와 음수 시각을 거절한다")
    void rejects_invalid_edit_metadata(UUID editId, Long editedAt) {
        assertThatThrownBy(() -> TimerSyncService.isSyncEdit(editId, editedAt))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    static Stream<Arguments> invalidEditMetadata() {
        return Stream.of(Arguments.of(null, 0L), Arguments.of(REQUEST_ID, null),
                Arguments.of(REQUEST_ID, -1L), Arguments.of(null, -1L));
    }

    @Test
    @DisplayName("409 오류는 공통 오류 봉투에 현재 타이머 값을 포함한다")
    void conflict_http_response_contains_current_timer() throws Exception {
        errorMvc().perform(get("/sync-test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.succeed").value(false))
                .andExpect(jsonPath("$.code").value("TIMER_EDIT_CONFLICT"))
                .andExpect(jsonPath("$.message").value(ErrorCode.TIMER_EDIT_CONFLICT.getMessage()))
                .andExpect(jsonPath("$.data.simpleTimerId").value(101))
                .andExpect(jsonPath("$.data.time").value(30))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-10-19T11:00:00.123"));
    }

    @Test
    @DisplayName("기존 오류의 사용자 메시지와 null data 및 새 삭제 오류 상태를 보존한다")
    void existing_error_handling_remains_unchanged() throws Exception {
        MockMvc mvc = errorMvc();
        mvc.perform(get("/sync-test/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.succeed").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.message").value("기존 사용자 메시지"))
                .andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(get("/sync-test/deleted"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("TIMER_DELETED"))
                .andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(get("/sync-test/reused"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TIMER_REQUEST_ID_REUSED"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    private MockMvc errorMvc() {
        return MockMvcBuilders.standaloneSetup(new ErrorController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Member createMember(UUID memberId) {
        Member member = Member.builder()
                .memberId(memberId)
                .memberEmail(memberId + "@sync.test")
                .memberNickname("sync_" + memberId)
                .build();
        em.persist(member);
        return member;
    }

    private long creationCount(UUID memberId) {
        return em.createQuery("select count(r) from TimerCreateReceipt r where r.member.memberId = :memberId", Long.class)
                .setParameter("memberId", memberId)
                .getSingleResult();
    }

    private long editCount(UUID memberId) {
        return em.createQuery("select count(r) from TimerEditReceipt r where r.member.memberId = :memberId", Long.class)
                .setParameter("memberId", memberId)
                .getSingleResult();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    record CurrentTimer(Long simpleTimerId, int time, String updatedAt) {
    }

    @RestController
    static class ErrorController {
        @GetMapping("/sync-test/conflict")
        public void conflict() {
            throw new TimerSyncConflictException(new CurrentTimer(101L, 30, "2026-10-19T11:00:00.123"));
        }

        @GetMapping("/sync-test/invalid")
        public void invalid() {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "기존 사용자 메시지");
        }

        @GetMapping("/sync-test/deleted")
        public void deleted() {
            throw new CustomException(ErrorCode.TIMER_DELETED);
        }

        @GetMapping("/sync-test/reused")
        public void reused() {
            throw new CustomException(ErrorCode.TIMER_REQUEST_ID_REUSED);
        }
    }
}
