package com.project200.undabang.timer.sync;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.timer.sync.entity.TimerCreateReceipt;
import com.project200.undabang.timer.sync.entity.TimerEditReceipt;
import com.project200.undabang.timer.sync.repository.TimerCreateReceiptRepository;
import com.project200.undabang.timer.sync.repository.TimerEditReceiptRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class TimerReceiptRepositoryTest {
    private static final LocalDateTime ORIGINAL_TIMESTAMP = LocalDateTime.of(2000, 1, 2, 3, 4, 5, 123_000_000);

    @Autowired
    private EntityManager em;

    @Autowired
    private TimerCreateReceiptRepository createReceiptRepository;

    @Autowired
    private TimerEditReceiptRepository editReceiptRepository;

    @Test
    @DisplayName("생성 요청 ID는 같은 회원의 타이머 종류 전체에서 유일하다")
    void creation_request_is_unique_across_timer_types() {
        Member member = createMember();
        UUID requestId = UUID.randomUUID();
        createReceiptRepository.saveAndFlush(TimerCreateReceipt.of(
                member, requestId, TimerType.SIMPLE, 101L, ORIGINAL_TIMESTAMP));

        assertThatThrownBy(() -> createReceiptRepository.saveAndFlush(TimerCreateReceipt.of(
                member, requestId, TimerType.CUSTOM, 202L, ORIGINAL_TIMESTAMP)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("다른 회원은 같은 생성 요청 ID로 각자의 원래 결과를 저장한다")
    void creation_requests_are_scoped_to_member() {
        Member firstMember = createMember();
        Member secondMember = createMember();
        UUID requestId = UUID.randomUUID();
        createReceiptRepository.save(TimerCreateReceipt.of(
                firstMember, requestId, TimerType.SIMPLE, 101L, ORIGINAL_TIMESTAMP));
        createReceiptRepository.save(TimerCreateReceipt.of(
                secondMember, requestId, TimerType.CUSTOM, 202L, ORIGINAL_TIMESTAMP.plusSeconds(1)));
        flushAndClear();

        assertThat(createReceiptRepository.findByMember_MemberIdAndCreateRequestId(firstMember.getMemberId(), requestId))
                .hasValueSatisfying(receipt -> {
                    assertThat(receipt.getTimerId()).isEqualTo(101L);
                    assertThat(receipt.getTimerType()).isEqualTo(TimerType.SIMPLE);
                    assertThat(receipt.getUpdatedAt()).isEqualTo(ORIGINAL_TIMESTAMP);
                });
        assertThat(createReceiptRepository.findByMember_MemberIdAndCreateRequestId(secondMember.getMemberId(), requestId))
                .hasValueSatisfying(receipt -> {
                    assertThat(receipt.getTimerId()).isEqualTo(202L);
                    assertThat(receipt.getTimerType()).isEqualTo(TimerType.CUSTOM);
                    assertThat(receipt.getUpdatedAt()).isEqualTo(ORIGINAL_TIMESTAMP.plusSeconds(1));
                });
    }

    @Test
    @DisplayName("같은 편집 키는 동작이 달라도 중복 저장할 수 없다")
    void edit_request_uniqueness_does_not_include_operation() {
        Member member = createMember();
        UUID editId = UUID.randomUUID();
        editReceiptRepository.saveAndFlush(TimerEditReceipt.of(
                member, TimerType.CUSTOM, 101L, editId, TimerEditOperation.RENAME, ORIGINAL_TIMESTAMP));

        assertThatThrownBy(() -> editReceiptRepository.saveAndFlush(TimerEditReceipt.of(
                member, TimerType.CUSTOM, 101L, editId, TimerEditOperation.DELETE, ORIGINAL_TIMESTAMP)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("편집 키는 회원과 종류와 타이머 ID 경계를 각각 분리한다")
    void edit_requests_are_scoped_to_member_type_and_timer() {
        Member member = createMember();
        Member otherMember = createMember();
        UUID editId = UUID.randomUUID();
        editReceiptRepository.save(TimerEditReceipt.of(
                member, TimerType.SIMPLE, 101L, editId, TimerEditOperation.UPDATE, ORIGINAL_TIMESTAMP));
        editReceiptRepository.save(TimerEditReceipt.of(
                member, TimerType.CUSTOM, 101L, editId, TimerEditOperation.RENAME, ORIGINAL_TIMESTAMP.plusSeconds(1)));
        editReceiptRepository.save(TimerEditReceipt.of(
                member, TimerType.SIMPLE, 102L, editId, TimerEditOperation.DELETE, ORIGINAL_TIMESTAMP.plusSeconds(2)));
        editReceiptRepository.save(TimerEditReceipt.of(
                otherMember, TimerType.SIMPLE, 101L, editId, TimerEditOperation.UPDATE, ORIGINAL_TIMESTAMP.plusSeconds(3)));
        flushAndClear();

        assertThat(editReceiptRepository.findByMember_MemberIdAndTimerTypeAndTimerIdAndEditId(
                member.getMemberId(), TimerType.SIMPLE, 101L, editId))
                .hasValueSatisfying(receipt -> assertThat(receipt.getUpdatedAt()).isEqualTo(ORIGINAL_TIMESTAMP));
        assertThat(editReceiptRepository.findByMember_MemberIdAndTimerTypeAndTimerIdAndEditId(
                member.getMemberId(), TimerType.CUSTOM, 101L, editId))
                .hasValueSatisfying(receipt -> {
                    assertThat(receipt.getOperation()).isEqualTo(TimerEditOperation.RENAME);
                    assertThat(receipt.getUpdatedAt()).isEqualTo(ORIGINAL_TIMESTAMP.plusSeconds(1));
                });
        assertThat(editReceiptRepository.findByMember_MemberIdAndTimerTypeAndTimerIdAndEditId(
                member.getMemberId(), TimerType.SIMPLE, 102L, editId))
                .hasValueSatisfying(receipt -> {
                    assertThat(receipt.getOperation()).isEqualTo(TimerEditOperation.DELETE);
                    assertThat(receipt.getUpdatedAt()).isEqualTo(ORIGINAL_TIMESTAMP.plusSeconds(2));
                });
        assertThat(editReceiptRepository.findByMember_MemberIdAndTimerTypeAndTimerIdAndEditId(
                otherMember.getMemberId(), TimerType.SIMPLE, 101L, editId))
                .hasValueSatisfying(receipt -> assertThat(receipt.getUpdatedAt()).isEqualTo(ORIGINAL_TIMESTAMP.plusSeconds(3)));
    }

    @Test
    @DisplayName("타이머 행이 없어도 오래된 영수증과 밀리초 응답을 그대로 유지한다")
    void receipts_do_not_require_a_live_timer_or_expire() {
        Member member = createMember();
        UUID requestId = UUID.randomUUID();
        UUID editId = UUID.randomUUID();
        TimerCreateReceipt creation = createReceiptRepository.save(TimerCreateReceipt.of(
                member, requestId, TimerType.SIMPLE, 101L, ORIGINAL_TIMESTAMP));
        TimerEditReceipt edit = editReceiptRepository.save(TimerEditReceipt.of(
                member, TimerType.SIMPLE, 101L, editId, TimerEditOperation.DELETE, ORIGINAL_TIMESTAMP));
        flushAndClear();

        assertThat(em.find(TimerCreateReceipt.class, creation.getId()).getUpdatedAt()).isEqualTo(ORIGINAL_TIMESTAMP);
        assertThat(em.find(TimerEditReceipt.class, edit.getId()).getUpdatedAt()).isEqualTo(ORIGINAL_TIMESTAMP);
    }

    @Test
    @DisplayName("회원 소프트 삭제는 영수증을 보존한다")
    void member_soft_deletion_retains_receipts() {
        UUID memberId = UUID.randomUUID();
        Member member = Member.builder()
                .memberId(memberId)
                .memberEmail(memberId + "@sync.test")
                .memberNickname("sync_" + memberId)
                .memberDeletedAt(ORIGINAL_TIMESTAMP)
                .build();
        em.persist(member);
        TimerCreateReceipt creation = createReceiptRepository.save(TimerCreateReceipt.of(
                member, UUID.randomUUID(), TimerType.CUSTOM, 101L, ORIGINAL_TIMESTAMP));
        TimerEditReceipt edit = editReceiptRepository.save(TimerEditReceipt.of(
                member, TimerType.CUSTOM, 101L, UUID.randomUUID(), TimerEditOperation.DELETE, ORIGINAL_TIMESTAMP));
        flushAndClear();

        assertThat(em.find(TimerCreateReceipt.class, creation.getId())).isNotNull();
        assertThat(em.find(TimerEditReceipt.class, edit.getId())).isNotNull();
    }

    @Test
    @DisplayName("회원 물리 삭제는 DB 외래 키로 두 종류의 영수증을 삭제한다")
    void physical_member_deletion_cascades_to_both_receipts() {
        Member member = createMember();
        TimerCreateReceipt creation = createReceiptRepository.save(TimerCreateReceipt.of(
                member, UUID.randomUUID(), TimerType.SIMPLE, 101L, ORIGINAL_TIMESTAMP));
        TimerEditReceipt edit = editReceiptRepository.save(TimerEditReceipt.of(
                member, TimerType.SIMPLE, 101L, UUID.randomUUID(), TimerEditOperation.DELETE, ORIGINAL_TIMESTAMP));
        flushAndClear();

        em.remove(em.find(Member.class, member.getMemberId()));
        flushAndClear();

        assertThat(em.find(Member.class, member.getMemberId())).isNull();
        assertThat(em.find(TimerCreateReceipt.class, creation.getId())).isNull();
        assertThat(em.find(TimerEditReceipt.class, edit.getId())).isNull();
    }

    private Member createMember() {
        UUID memberId = UUID.randomUUID();
        Member member = Member.builder()
                .memberId(memberId)
                .memberEmail(memberId + "@sync.test")
                .memberNickname("sync_" + memberId)
                .build();
        em.persist(member);
        return member;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
